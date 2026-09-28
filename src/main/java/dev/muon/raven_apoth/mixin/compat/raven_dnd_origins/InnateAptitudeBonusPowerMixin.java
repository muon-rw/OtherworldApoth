package dev.muon.raven_apoth.mixin.compat.raven_dnd_origins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.raven_apoth.affix.GearAptitudeBonuses;
import dev.muon.raven_dnd_origins.power.InnateAptitudeBonusPower;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Set;

@Mixin(value = InnateAptitudeBonusPower.class, remap = false)
public class InnateAptitudeBonusPowerMixin {

    @ModifyReturnValue(method = "getBonus", at = @At("RETURN"))
    private static int raven_apoth$addGearBonus(int bonus, Entity entity, String aptitudeName) {
        return bonus + GearAptitudeBonuses.applied(entity, aptitudeName);
    }

    @ModifyReturnValue(method = "sumBonusesForAptitudes", at = @At("RETURN"))
    private static int raven_apoth$addGearBonuses(int sum, Entity entity, Set<String> aptitudeNames) {
        return sum + GearAptitudeBonuses.appliedSum(entity, aptitudeNames);
    }
}
