package dev.muon.raven_apoth.mixin.apotheosis;

import dev.muon.raven_apoth.config.RavenApothConfig;
import dev.muon.raven_apoth.tiers.WorldTierLevels;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WorldTier.class, remap = false)
public class WorldTierMixin {

    @Inject(method = "isUnlocked", at = @At("HEAD"), cancellable = true)
    private static void raven_apoth$unlockByLevel(Player player, WorldTier tier, CallbackInfoReturnable<Boolean> cir) {
        if (RavenApothConfig.gateWorldTiersByLevel) {
            cir.setReturnValue(WorldTierLevels.isUnlocked(player, tier));
        }
    }
}
