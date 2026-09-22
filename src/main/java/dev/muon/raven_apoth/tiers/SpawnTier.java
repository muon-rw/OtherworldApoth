package dev.muon.raven_apoth.tiers;

import dev.muon.raven_apoth.RavenApothAttachments;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;

import java.util.Optional;

/**
 * Apotheosis applies a spawning mob's tier augments from the nearest player's tier but never records that tier,
 * and then rolls loot from the killer's tier. The tier is marked here, from the same player Apotheosis asks,
 * so drops follow the mob that was actually fought. Unstamped mobs count as Haven.
 */
public class SpawnTier {

    public static WorldTier of(LivingEntity entity) {
        return entity.getExistingData(RavenApothAttachments.SPAWN_TIER).orElse(WorldTier.HAVEN);
    }

    public static Optional<SpawnOrigin> originOf(LivingEntity entity) {
        return entity.getExistingData(RavenApothAttachments.SPAWN_ORIGIN);
    }

    // Apotheosis handles this event at LOW; running after it keeps the two reads of the nearest player adjacent.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void markSpawn(FinalizeSpawnEvent event) {
        if (event.isCanceled() || event.isSpawnCancelled()) {
            return;
        }
        SpawnOrigin origin = SpawnOrigin.of(event.getSpawnType());
        if (origin != null) {
            event.getEntity().setData(RavenApothAttachments.SPAWN_ORIGIN, origin);
        }
        Player nearest = event.getLevel().getNearestPlayer(event.getX(), event.getY(), event.getZ(), -1, false);
        if (nearest != null) {
            event.getEntity().setData(RavenApothAttachments.SPAWN_TIER, WorldTier.getTier(nearest));
        }
    }

    @SubscribeEvent
    public void migrateSpawnMarkers(LivingConversionEvent.Post event) {
        LivingEntity before = event.getEntity();
        LivingEntity after = event.getOutcome();
        before.getExistingData(RavenApothAttachments.SPAWN_ORIGIN).ifPresent(o -> after.setData(RavenApothAttachments.SPAWN_ORIGIN, o));
        before.getExistingData(RavenApothAttachments.SPAWN_TIER).ifPresent(t -> after.setData(RavenApothAttachments.SPAWN_TIER, t));
    }
}
