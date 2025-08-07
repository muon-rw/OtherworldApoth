package dev.muon.otherworldapoth.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldapoth.util.SpellCastUtil;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import dev.shadowsoffire.placebo.util.StepFunction;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SpellTriggerAffix extends Affix {
    private static final ThreadLocal<Boolean> IS_TRIGGERING = ThreadLocal.withInitial(() -> false);

    public static boolean isCurrentlyTriggering() {
        return IS_TRIGGERING.get();
    }

    public static final Codec<SpellTriggerAffix> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                    SpellRegistry.REGISTRY.get().getCodec().fieldOf("spell").forGetter(a -> a.spell),
                    TriggerType.CODEC.fieldOf("trigger").forGetter(a -> a.trigger),
                    LootRarity.mapCodec(TriggerData.CODEC).fieldOf("values").forGetter(a -> a.values),
                    LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.types),
                    TargetType.CODEC.optionalFieldOf("target").forGetter(a -> a.target))
            .apply(inst, SpellTriggerAffix::new));

    protected final AbstractSpell spell;
    protected final TriggerType trigger;
    protected final Map<LootRarity, TriggerData> values;
    protected final Set<LootCategory> types;
    protected final Optional<TargetType> target;

    public SpellTriggerAffix(AbstractSpell spell, TriggerType trigger,
                             Map<LootRarity, TriggerData> values, Set<LootCategory> types,
                             Optional<TargetType> target) {
        super(AffixType.ABILITY);
        this.spell = spell;
        this.trigger = trigger;
        this.values = values;
        this.types = types;
        this.target = target;
    }

//    public void triggerSpell(LivingEntity caster, LivingEntity target, AffixInstance inst) {
//        triggerSpell(caster, target, inst.rarity().get(), inst.level());
//    }

    public void triggerSpell(LivingEntity caster, LivingEntity target, LootRarity rarity, float level) {
        // Prevent infinite recursion, since we don't have access to CastSource in the SpellDamageEvent/SpellHealEvent
        if (IS_TRIGGERING.get()) {
            return;
        }

        TriggerData data = this.values.get(rarity);
        if (data == null || caster.level().isClientSide()) return;

        int spellLevel = data.level().getInt(level);
        AbstractSpell spellInstance = this.spell;
        String spellId = spellInstance.getSpellId();

        MagicData magicData = MagicData.getPlayerMagicData(caster);
        boolean hasActiveRecast = magicData.getPlayerRecasts().hasRecastForSpell(spellId);

        int cooldown = data.cooldown();
        if (!hasActiveRecast && cooldown != 0 && Affix.isOnCooldown(this.getId(), cooldown, caster)) {
            return;
        }

        try {
            IS_TRIGGERING.set(true);

            SpellCastUtil.castSpell(caster, spellInstance, spellLevel, target);

            if (!hasActiveRecast && cooldown != 0) {
                Affix.startCooldown(this.getId(), caster);
            }
        } finally {
            IS_TRIGGERING.set(false);
        }
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        return (this.types.isEmpty() || this.types.contains(cat)) && this.values.containsKey(rarity);
    }

    @Override
    public MutableComponent getDescription(ItemStack stack, LootRarity rarity, float level) {
        TriggerData data = this.values.get(rarity);
        if (data == null) return Component.empty();

        String triggerKey = "affix.otherworldapoth.trigger." + this.trigger.name().toLowerCase();
        AbstractSpell spellInstance = this.spell;
        int spellLevel = data.level().getInt(level);

        Component coloredSpellName = spellInstance.getDisplayName(null).copy()
                .append(" ")
                .append(Component.translatable("enchantment.level." + spellLevel))
                .withStyle(spellInstance.getSchoolType().getDisplayName().getStyle());

        boolean isSelfCast = this.target.map(t -> t == TargetType.SELF).orElse(false);
        String finalKey = isSelfCast ? triggerKey + ".self" : triggerKey;

        MutableComponent comp = Component.translatable(finalKey, coloredSpellName);

        int cooldown = data.cooldown();
        if (cooldown != 0) {
            Component cd = Component.translatable("affix.apotheosis.cooldown", StringUtil.formatTickDuration(cooldown));
            comp = comp.append(" ").append(cd);
        }

        return comp;
    }

    @Override
    public Component getAugmentingText(ItemStack stack, LootRarity rarity, float level) {
        TriggerData data = this.values.get(rarity);
        if (data == null) return Component.empty();

        int currentLevel = data.level().getInt(level);
        AbstractSpell spellInstance = this.spell;

        Component coloredSpellName = spellInstance.getDisplayName(null).copy()
                .append(" ")
                .append(Component.translatable("enchantment.level." + currentLevel))
                .withStyle(spellInstance.getSchoolType().getDisplayName().getStyle());

        boolean isSelfCast = this.target.map(t -> t == TargetType.SELF).orElse(false);
        String triggerKey = "affix.otherworldapoth.trigger." + this.trigger.name().toLowerCase();
        String finalKey = isSelfCast ? triggerKey + ".self" : triggerKey;

        MutableComponent comp = Component.translatable(finalKey, coloredSpellName);

        int minLevel = data.level().getInt(0);
        int maxLevel = data.level().getInt(1);
        if (minLevel != maxLevel) {
            Component minComp = Component.translatable("enchantment.level." + minLevel);
            Component maxComp = Component.translatable("enchantment.level." + maxLevel);
            comp.append(Affix.valueBounds(minComp, maxComp));
        }

        int cooldown = data.cooldown();
        if (cooldown != 0) {
            Component cd = Component.translatable("affix.apotheosis.cooldown", StringUtil.formatTickDuration(cooldown));
            comp = comp.append(" ").append(cd);
        }

        return comp;
    }

    @Override
    public void doPostAttack(ItemStack stack, LootRarity rarity, float level, LivingEntity user, @Nullable Entity target) {
        if (this.trigger == TriggerType.MELEE_HIT && target instanceof LivingEntity livingTarget) {
            LivingEntity actualTarget = this.target.map(targetType -> switch (targetType) {
                case SELF -> user;
                case TARGET -> livingTarget;
            }).orElse(livingTarget);

            triggerSpell(user, actualTarget, rarity, level);
        }
    }

    @Override
    public void doPostHurt(ItemStack stack, LootRarity rarity, float level, LivingEntity user, @Nullable Entity attacker) {
        if (this.trigger == TriggerType.HURT && attacker instanceof LivingEntity livingAttacker) {
            LivingEntity actualTarget = this.target.map(targetType -> switch (targetType) {
                case SELF -> user;
                case TARGET -> livingAttacker;
            }).orElse(user);

            triggerSpell(user, actualTarget, rarity, level);
        }
    }

    @Override
    public void onArrowImpact(AbstractArrow arrow, LootRarity rarity, float level, HitResult res, HitResult.Type type) {
        if (this.trigger == TriggerType.ARROW_HIT && type == HitResult.Type.ENTITY &&
                res instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity hitEntity &&
                arrow.getOwner() instanceof LivingEntity owner) {

            LivingEntity actualTarget = this.target.map(targetType -> switch (targetType) {
                case SELF -> owner;
                case TARGET -> hitEntity;
            }).orElse(hitEntity);

            triggerSpell(owner, actualTarget, rarity, level);
        }
    }

    public enum TriggerType {
        SPELL_DAMAGE,
        SPELL_HEAL,
        MELEE_HIT,
        ARROW_HIT,
        HURT;
        public static final Codec<TriggerType> CODEC = PlaceboCodecs.enumCodec(TriggerType.class);
    }

    public enum TargetType {
        SELF,
        TARGET;

        public static final Codec<TargetType> CODEC = PlaceboCodecs.enumCodec(TargetType.class);
    }

    public record TriggerData(StepFunction level, int cooldown) {
        private static final Codec<TriggerData> CODEC = RecordCodecBuilder.create(inst -> inst
                .group(
                        StepFunction.CODEC.optionalFieldOf("level", StepFunction.constant(1)).forGetter(TriggerData::level),
                        Codec.INT.optionalFieldOf("cooldown", 0).forGetter(TriggerData::cooldown))
                .apply(inst, TriggerData::new));
    }
}