package dev.muon.raven_apoth.tiers;

import dev.muon.dynamic_difficulty.api.LevelingAPI;
import dev.muon.raven_apoth.RavenApoth;
import dev.muon.raven_apoth.config.RavenApothConfig;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.Map;

public final class WorldTierLevels {

    private static Map<WorldTier, Integer> requiredLevels = Map.of();

    private WorldTierLevels() {}

    public static void reload() {
        Map<WorldTier, Integer> parsed = new EnumMap<>(WorldTier.class);
        for (String entry : RavenApothConfig.worldTierLevels) {
            String[] parts = entry.split("=");
            if (parts.length != 2) {
                RavenApoth.LOGGER.warn("Ignoring malformed worldTierLevels entry '{}'", entry);
                continue;
            }
            WorldTier tier = byName(parts[0].trim());
            if (tier == null) {
                RavenApoth.LOGGER.warn("Ignoring worldTierLevels entry '{}': unknown tier", entry);
                continue;
            }
            try {
                parsed.put(tier, Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException e) {
                RavenApoth.LOGGER.warn("Ignoring worldTierLevels entry '{}': bad level", entry);
            }
        }
        requiredLevels = parsed;
    }

    public static int requiredLevel(WorldTier tier) {
        return requiredLevels.getOrDefault(tier, 0);
    }

    public static boolean isUnlocked(Player player, WorldTier tier) {
        return LevelingAPI.getLevel(player) >= requiredLevel(tier);
    }

    private static WorldTier byName(String name) {
        for (WorldTier tier : WorldTier.values()) {
            if (tier.getSerializedName().equalsIgnoreCase(name)) {
                return tier;
            }
        }
        return null;
    }
}
