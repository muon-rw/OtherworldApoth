package dev.muon.otherworldapoth.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import dev.shadowsoffire.placebo.util.StepFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Set;

public class SpellEffectAffix extends Affix {
    public static final Codec<SpellEffectAffix> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                    ForgeRegistries.MOB_EFFECTS.getCodec().fieldOf("mob_effect").forGetter(a -> a.effect),
                    SpellTarget.CODEC.fieldOf("target").forGetter(a -> a.target),
                    LootRarity.mapCodec(EffectData.CODEC).fieldOf("values").forGetter(a -> a.values),
                    PlaceboCodecs.nullableField(Codec.INT, "cooldown", 0).forGetter(a -> a.cooldown),
                    LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.types))
            .apply(inst, SpellEffectAffix::new));

    protected final MobEffect effect;
    protected final SpellTarget target;
    protected final Map<LootRarity, EffectData> values;
    protected final int cooldown;
    protected final Set<LootCategory> types;

    public void applyEffect(LivingEntity target, LootRarity rarity, float level) {
        if (isOnCooldown(this.getId(), this.cooldown, target)) return;
        target.addEffect(this.values.get(rarity).build(this.effect, level));
        startCooldown(this.getId(), target);
    }

    public SpellEffectAffix(MobEffect effect, SpellTarget target, Map<LootRarity, EffectData> values, int cooldown, Set<LootCategory> types) {
        super(AffixType.ABILITY);
        this.effect = effect;
        this.target = target;
        this.values = values;
        this.cooldown = cooldown;
        this.types = types;
    }

    public static enum SpellTarget {
        SPELL_CAST_SELF,
        SPELL_CAST_TARGET;

        public static final Codec<SpellTarget> CODEC = PlaceboCodecs.enumCodec(SpellTarget.class);
    }

    private static Component toComponent(MobEffectInstance inst) {
        MutableComponent mutablecomponent = Component.translatable(inst.getDescriptionId());
        MobEffect mobeffect = inst.getEffect();

        if (inst.getAmplifier() > 0) {
            mutablecomponent = Component.translatable("potion.withAmplifier", mutablecomponent,
                    Component.translatable("potion.potency." + inst.getAmplifier()));
        }

        if (inst.getDuration() > 20) {
            mutablecomponent = Component.translatable("potion.withDuration", mutablecomponent,
                    MobEffectUtil.formatDuration(inst, 1));
        }

        return mutablecomponent.withStyle(mobeffect.getCategory().getTooltipFormatting());
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
        MobEffectInstance inst = this.values.get(rarity).build(this.effect, level);
        return Component.translatable("affix.apotheosis.target." + this.target.name().toLowerCase(), toComponent(inst));
    }

    public static record EffectData(StepFunction duration, StepFunction amplifier, int cooldown) {
        private static Codec<EffectData> CODEC = RecordCodecBuilder.create(inst -> inst
                .group(
                        StepFunction.CODEC.fieldOf("duration").forGetter(EffectData::duration),
                        StepFunction.CODEC.fieldOf("amplifier").forGetter(EffectData::amplifier),
                        PlaceboCodecs.nullableField(Codec.INT, "cooldown", -1).forGetter(EffectData::cooldown))
                .apply(inst, EffectData::new));

        public MobEffectInstance build(MobEffect effect, float level) {
            return new MobEffectInstance(effect, this.duration.getInt(level), this.amplifier.getInt(level));
        }
    }
}