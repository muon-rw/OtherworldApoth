package dev.muon.otherworldapoth.mixin.apoth;

import dev.shadowsoffire.apotheosis.adventure.boss.BossEvents;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent.FinalizeSpawn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BossEvents.class, remap = false)
public class BossEventsMixin {

    @Inject(method = "naturalBosses", at = @At("HEAD"), cancellable = true)
    private void otherworldapoth$preventBossSpawns(FinalizeSpawn e, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "minibosses", at = @At("HEAD"), cancellable = true)
    private void otherworldapoth$preventMinibossSpawns(FinalizeSpawn e, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "delayedMinibosses", at = @At("HEAD"), cancellable = true)
    private void otherworldapoth$preventDelayedMinibossSpawns(EntityJoinLevelEvent e, CallbackInfo ci) {
        ci.cancel();
    }
}