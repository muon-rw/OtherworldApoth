package dev.muon.otherworldapoth.mixin.apoth.type;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.TelepathicAffix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = TelepathicAffix.class, remap = false)
public class TelepathicAffixMixin {

    @ModifyArg(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Ldev/shadowsoffire/apotheosis/adventure/affix/Affix;<init>(Ldev/shadowsoffire/apotheosis/adventure/affix/AffixType;)V"),
        index = 0,
        remap = false
    )
    private static AffixType modifyAffixType(AffixType original) {
        return AffixType.POTION;
    }
}
