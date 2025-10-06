package dev.muon.otherworldapoth.mixin.spell_nerfs;

import com.gametechbc.traveloptics.spells.fire.AnnihilationSpell;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AnnihilationSpell.class, remap = false)
public class AnnihilationMixin {
    @ModifyReturnValue(method = "getDamage", at = @At("RETURN"))
    private float modifyDamage(float original) {
        //TODO: Use now-shippable Iron's config instead
        return Math.min(original * 0.33f, 1000.0f);
    }
}