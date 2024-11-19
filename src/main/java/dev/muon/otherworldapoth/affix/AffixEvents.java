package dev.muon.otherworldapoth.affix;

import dev.muon.otherworldapoth.OtherworldApoth;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.stream.StreamSupport;

public class AffixEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void dropsLowest(LivingDropsEvent e) {
        MagicTelepathicAffix.drops(e);
    }

    @SubscribeEvent
    public void onSpellDamage(SpellDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        LivingEntity caster = event.getSpellDamageSource().getEntity() instanceof LivingEntity living ? living : null;
        if (caster == null) return;

        for (ItemStack stack : caster.getAllSlots()) {
            AffixHelper.streamAffixes(stack).forEach(inst -> {
                if (inst.affix().get() instanceof SpellEffectAffix affix) {
                    if (affix.target == SpellEffectAffix.SpellTarget.SPELL_CAST_TARGET) {
                        affix.applyEffect(event.getEntity(), inst.rarity().get(), inst.level());
                    } else if (affix.target == SpellEffectAffix.SpellTarget.SPELL_CAST_SELF) {
                        affix.applyEffect(caster, inst.rarity().get(), inst.level());
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public void onSpellHeal(SpellHealEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        for (ItemStack stack : event.getEntity().getAllSlots()) {
            AffixHelper.streamAffixes(stack).forEach(inst -> {
                if (inst.affix().get() instanceof SpellEffectAffix affix) {
                    if (affix.target == SpellEffectAffix.SpellTarget.SPELL_CAST_TARGET) {
                        affix.applyEffect(event.getTargetEntity(), inst.rarity().get(), inst.level());
                    } else if (affix.target == SpellEffectAffix.SpellTarget.SPELL_CAST_SELF) {
                        affix.applyEffect(event.getEntity(), inst.rarity().get(), inst.level());
                    }
                }
            });
        }
    }


    @SubscribeEvent
    public void onSpellCast(SpellOnCastEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        SchoolType school = event.getSchoolType();
        int baseLevel = event.getSpellLevel();

        int totalBonus = StreamSupport.stream(event.getEntity().getAllSlots().spliterator(), false)
                .flatMap(stack -> StreamSupport.stream(AffixHelper.streamAffixes(stack).spliterator(), false))
                .filter(inst -> inst.affix().get() instanceof SpellLevelAffix affix
                        && affix.getSchool() == school)
                .mapToInt(inst -> ((SpellLevelAffix) inst.affix().get())
                        .getBonusLevel(inst.rarity().get(), inst.level()))
                .sum();

        if (totalBonus > 0) {
            event.setSpellLevel(baseLevel + totalBonus);
            OtherworldApoth.LOGGER.debug("Changed spell level from {} to {} for {}",
                    baseLevel, (baseLevel + totalBonus), event.getEntity().getName());
        }
    }

}
