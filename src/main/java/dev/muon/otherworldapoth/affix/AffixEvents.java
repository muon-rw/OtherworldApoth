package dev.muon.otherworldapoth.affix;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.event.GetItemSocketsEvent;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import io.redspace.ironsspellbooks.api.events.*;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
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

        for (ItemStack stack : caster.getAllSlots()) {
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

        for (ItemStack stack : caster.getAllSlots()) {
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
        if (event.getEntity().level().isClientSide()) return;
        SchoolType school = event.getSpell().getSchoolType();
        int totalBonus = StreamSupport.stream(event.getEntity().getAllSlots().spliterator(), false)
                .flatMap(stack -> StreamSupport.stream(AffixHelper.streamAffixes(stack).spliterator(), false))
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


}
