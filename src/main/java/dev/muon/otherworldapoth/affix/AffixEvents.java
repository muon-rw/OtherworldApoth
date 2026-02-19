package dev.muon.otherworldapoth.affix;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.event.GetItemSocketsEvent;
import dev.shadowsoffire.apotheosis.util.DamageSourceExtension;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import io.redspace.ironsspellbooks.api.events.*;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.muon.otherworldapoth.OtherworldApoth;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class AffixEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void dropsLowest(LivingDropsEvent e) {
        MagicTelepathicAffix.drops(e);
    }

    @SubscribeEvent
    public void hookAddSocketsAffix(GetItemSocketsEvent event) {
        ItemStack stack = event.getStack();

        int affixBonus = StreamSupport.stream(AffixHelper.streamAffixes(stack).spliterator(), false)
                .filter(inst -> inst.affix().get() instanceof SocketBonusAffix)
                .mapToInt(inst -> {
                    SocketBonusAffix affix = (SocketBonusAffix) inst.affix().get();
                    return affix.getBonusSockets(inst.rarity().get(), inst.level());
                })
                .sum();

        if (affixBonus > 0) {
            event.setSockets(event.getSockets() + affixBonus);
        }
    }

    @SubscribeEvent
    public void hookSpellDamageAffix(SpellDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        LivingEntity caster = event.getSpellDamageSource().getEntity() instanceof LivingEntity living ? living : null;
        if (caster == null) return;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = caster.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            LootCategory cat = LootCategory.forItem(stack);
            if (cat.isNone()) continue;
            if (!Arrays.stream(cat.getSlots()).anyMatch(s -> s == slot)) continue;

            AffixHelper.streamAffixes(stack).forEach(inst -> {
                if (inst.affix().get() instanceof SpellEffectAffix affix) {
                    if (affix.target == SpellEffectAffix.SpellTarget.SPELL_DAMAGE_TARGET) {
                        affix.applyEffect(event.getEntity(), inst.rarity().get(), inst.level());
                    } else if (affix.target == SpellEffectAffix.SpellTarget.SPELL_DAMAGE_SELF) {
                        affix.applyEffect(caster, inst.rarity().get(), inst.level());
                    }
                } else if (inst.affix().get() instanceof SpellTriggerAffix affix && affix.trigger == SpellTriggerAffix.TriggerType.SPELL_DAMAGE) {
                    LivingEntity target = affix.target.map(targetType -> switch (targetType) {
                        case SELF -> caster;
                        case TARGET -> event.getEntity();
                    }).orElse(event.getEntity());

                    affix.triggerSpell(caster, target, inst.rarity().get(), inst.level());
                }
            });
        }
    }


    @SubscribeEvent
    public void hookSpellHealAffix(SpellHealEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        LivingEntity caster = event.getEntity();

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = caster.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            LootCategory cat = LootCategory.forItem(stack);
            if (cat.isNone()) continue;
            if (!Arrays.stream(cat.getSlots()).anyMatch(s -> s == slot)) continue;

            AffixHelper.streamAffixes(stack).forEach(inst -> {
                if (inst.affix().get() instanceof SpellEffectAffix affix) {
                    if (affix.target == SpellEffectAffix.SpellTarget.SPELL_HEAL_TARGET) {
                        affix.applyEffect(event.getTargetEntity(), inst.rarity().get(), inst.level());
                    } else if (affix.target == SpellEffectAffix.SpellTarget.SPELL_HEAL_SELF) {
                        affix.applyEffect(caster, inst.rarity().get(), inst.level());
                    }
                } else if (inst.affix().get() instanceof SpellTriggerAffix affix && affix.trigger == SpellTriggerAffix.TriggerType.SPELL_HEAL) {
                    LivingEntity target = affix.target.map(targetType -> switch (targetType) {
                        case SELF -> caster;
                        case TARGET -> event.getTargetEntity();
                    }).orElse(event.getTargetEntity());

                    affix.triggerSpell(caster, target, inst.rarity().get(), inst.level());
                }
            });
        }
    }


    @SubscribeEvent
    public void hookSpellLevelAffix(ModifySpellLevelEvent event) {
        if (event.getEntity() == null) return;
        SchoolType school = event.getSpell().getSchoolType();
        LivingEntity entity = event.getEntity();
        int totalBonus = Arrays.stream(EquipmentSlot.values())
                .flatMap(slot -> {
                    ItemStack stack = entity.getItemBySlot(slot);
                    if (stack.isEmpty()) return Stream.empty();
                    LootCategory cat = LootCategory.forItem(stack);
                    if (cat.isNone()) return Stream.empty();
                    boolean slotMatches = Arrays.stream(cat.getSlots()).anyMatch(s -> s == slot);
                    if (!slotMatches) return Stream.empty();
                    return StreamSupport.stream(AffixHelper.streamAffixes(stack).spliterator(), false);
                })
                .filter(inst -> inst.affix().get() instanceof SpellLevelAffix affix
                        && affix.getSchool() == school)
                .mapToInt(inst -> ((SpellLevelAffix) inst.affix().get())
                        .getBonusLevel(inst.rarity().get(), inst.level()))
                .sum();
        if (totalBonus > 0) {
            event.addLevels(totalBonus);
        }
    }


    @SubscribeEvent
    public void onChangeMana(ChangeManaEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        Player player = event.getEntity();
        MagicData magicData = event.getMagicData();
        SpellData castingSpell = magicData.getCastingSpell();

        if (castingSpell == null || event.getNewMana() >= event.getOldMana()) {
            return;
        }

        AbstractSpell spell = castingSpell.getSpell();
        if (spell == null) return;

        SchoolType spellSchool = spell.getSchoolType();

        // Only ever checking mainhand since this is a weapon-only aug
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty()) return;

        float totalReduction = 0f;

        Map<DynamicHolder<? extends Affix>, AffixInstance> affixes = AffixHelper.getAffixes(mainHand);
        for (AffixInstance instance : affixes.values()) {
            if (instance.isValid() && instance.affix().isBound()) {
                Affix affix = instance.affix().get();
                if (affix instanceof ManaCostAffix manaCostAffix) {
                    if (manaCostAffix.getSchool() == spellSchool) {
                        float reduction = manaCostAffix.getReductionPercent(instance.rarity().get(), instance.level());
                        totalReduction += reduction;
                    }
                }
            }
        }

        if (totalReduction > 0) {
            float manaCost = event.getOldMana() - event.getNewMana();
            float reducedCost = manaCost * (1 - Math.min(totalReduction, 0.9f)); // Cap at 90% reduction
            float newManaValue = event.getOldMana() - reducedCost;
            event.setNewMana(newManaValue);
        }
    }

    /**
     * Transmutation affix: convert weapon damage to magic damage of the affix's school.
     * Only applies to direct weapon damage (melee player/mob attack, arrows) - not spell-triggered damage.
     * Uses LivingAttackEvent so we modify the DamageSource before it flows through the damage pipeline.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void hookTransmutationAffix(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();

        // Only apply to weapon damage: player attack, mob attack, or arrow (excludes spells: INDIRECT_MAGIC, etc.)
        boolean isWeaponDamage = source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO) || source.is(DamageTypes.ARROW);
        if (!isWeaponDamage) return;

        // Ranged: arrow with transmutation affix (affix is on the arrow from the bow)
        if (source.getDirectEntity() instanceof AbstractArrow arrow) {
            AffixHelper.streamAffixes(arrow)
                    .filter(inst -> inst.affix().get() instanceof TransmutationAffix)
                    .findFirst()
                    .ifPresent(inst -> {
                        TransmutationAffix affix = (TransmutationAffix) inst.affix().get();
                        if (source instanceof DamageSourceExtension ext) {
                            ext.addTag(affix.getDamageTypeTag());
                        }
                        OtherworldApoth.LOGGER.debug("[Transmutation] Ranged: attacker={}, target={}, school={}, damageTypeTag={}, amount={}, tags={}",
                                source.getEntity() != null ? source.getEntity().getName().getString() : "null",
                                target.getName().getString(),
                                affix.getSchool().getId(),
                                affix.getDamageTypeTag().location(),
                                event.getAmount(),
                                getDamageSourceTags(source, target));
                    });
            return;
        }

        // Melee: attacker's main hand has transmutation affix
        if (source.getEntity() instanceof LivingEntity attacker) {
            ItemStack mainHand = attacker.getMainHandItem();
            if (!mainHand.isEmpty()) {
                AffixHelper.streamAffixes(mainHand)
                        .filter(inst -> inst.affix().get() instanceof TransmutationAffix)
                        .findFirst()
                        .ifPresent(inst -> {
                            TransmutationAffix affix = (TransmutationAffix) inst.affix().get();
                            if (source instanceof DamageSourceExtension ext) {
                                ext.addTag(affix.getDamageTypeTag());
                            }
                            OtherworldApoth.LOGGER.debug("[Transmutation] Melee: attacker={}, target={}, school={}, damageTypeTag={}, amount={}, tags={}",
                                    attacker.getName().getString(),
                                    target.getName().getString(),
                                    affix.getSchool().getId(),
                                    affix.getDamageTypeTag().location(),
                                    event.getAmount(),
                                    getDamageSourceTags(source, target));
                        });
            }
        }
    }

    // Collects all damage type tags that apply to this source (intrinsic + DamageSourceExtension extras).
    // For Debugging
    private static List<String> getDamageSourceTags(DamageSource source, LivingEntity target) {
        List<String> tags = new ArrayList<>();
        target.level().registryAccess().registry(Registries.DAMAGE_TYPE).ifPresent(registry ->
                registry.getTags().forEach(entry -> {
                    TagKey<DamageType> tagKey = entry.getFirst();
                    if (source.is(tagKey)) {
                        tags.add("#" + tagKey.location());
                    }
                }));
        return tags;
    }
}
