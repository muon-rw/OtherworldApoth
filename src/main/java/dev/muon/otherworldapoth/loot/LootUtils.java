package dev.muon.otherworldapoth.loot;

import dev.muon.otherworldapoth.OtherworldApoth;
import dev.muon.otherworldapoth.config.OWApothConfig;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import javax.annotation.Nullable;
import java.util.List;

public class LootUtils {

    /**
     * Finds the appropriate rarity mapping for a given level using threshold format.
     * Example mappings: ["25=mythic-ancient", "20=epic-mythic", "15=rare-epic"]
     * Level 27 would return "mythic-ancient"
     * Level 22 would return "epic-mythic"
     *
     * @param mappings Array of level threshold mappings in format "level=minRarity-maxRarity"
     * @param level The entity level to find mapping for
     * @return The rarity range string (e.g. "common-rare")
     */
    public static String findMappingForLevel(String[] mappings, int level) {
        return java.util.Arrays.stream(mappings)
                .map(s -> s.split("="))
                .filter(parts -> Integer.parseInt(parts[0]) <= level)
                .max((a, b) -> Integer.compare(
                        Integer.parseInt(a[0]),
                        Integer.parseInt(b[0])
                ))
                .map(parts -> parts[1])
                .orElse("common-common");
    }

    /**
     * Attempts to find the relevant player from a LootContext.
     * Checks various player-related parameters in order of relevance.
     *
     * @param ctx The LootContext to search
     * @return The relevant player, or null if none found
     */
    @Nullable
    public static Player findRelevantPlayer(LootContext ctx) {
        if (ctx.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof Player p) return p;
        if (ctx.getParamOrNull(LootContextParams.DIRECT_KILLER_ENTITY) instanceof Player p) return p;
        if (ctx.getParamOrNull(LootContextParams.KILLER_ENTITY) instanceof Player p) return p;
        if (ctx.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER) != null) return ctx.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER);
        return null;
    }

    public static LootRarity getRarityForPlayerLevel(int playerLevel, RandomSource rand, float luck) {
        String mapping = findMappingForLevel(OWApothConfig.playerLevelRarityMappings, playerLevel);
        OtherworldApoth.LOGGER.debug("Finding rarity for player level {}, mapping: {}", playerLevel, mapping);

        String[] rarities = mapping.split("-");
        DynamicHolder<LootRarity> minRarity = RarityRegistry.INSTANCE.holder(Apotheosis.loc(rarities[0]));
        DynamicHolder<LootRarity> maxRarity = RarityRegistry.INSTANCE.holder(Apotheosis.loc(rarities[1]));

        List<DynamicHolder<LootRarity>> validRarities = RarityRegistry.INSTANCE.getOrderedRarities().stream()
                .filter(r -> r.get().ordinal() >= minRarity.get().ordinal()
                        && r.get().ordinal() <= maxRarity.get().ordinal())
                .toList();

        double totalWeight = validRarities.stream()
                .mapToDouble(r -> {
                    LootRarity rarity = r.get();
                    return rarity.getWeight() + (luck * rarity.getQuality());
                })
                .sum();
        double roll = rand.nextDouble() * totalWeight;

        double currentWeight = 0;
        DynamicHolder<LootRarity> selected = validRarities.get(0);

        for (DynamicHolder<LootRarity> rarity : validRarities) {
            LootRarity r = rarity.get();
            currentWeight += r.getWeight() + (luck * r.getQuality());
            if (roll <= currentWeight) {
                selected = rarity;
                break;
            }
        }

        OtherworldApoth.LOGGER.debug("Selected rarity: {}", selected.getId());
        return selected.get();
    }

    public static LootRarity getRarityForMobLevel(int mobLevel, RandomSource rand, float luck, boolean isGem) {
        String mapping = findMappingForLevel(
                isGem ? OWApothConfig.gemRarityMappings : OWApothConfig.levelRarityMappings,
                mobLevel
        );
        OtherworldApoth.LOGGER.debug("Finding rarity for mob level {} ({}), mapping: {}",
                mobLevel, isGem ? "gem" : "item", mapping);

        String[] rarities = mapping.split("-");
        DynamicHolder<LootRarity> minRarity = RarityRegistry.INSTANCE.holder(Apotheosis.loc(rarities[0]));
        DynamicHolder<LootRarity> maxRarity = RarityRegistry.INSTANCE.holder(Apotheosis.loc(rarities[1]));

        List<DynamicHolder<LootRarity>> validRarities = RarityRegistry.INSTANCE.getOrderedRarities().stream()
                .filter(r -> r.get().ordinal() >= minRarity.get().ordinal()
                        && r.get().ordinal() <= maxRarity.get().ordinal())
                .toList();

        double luckBonus = Math.max(0, luck * 0.1);
        double totalWeight = validRarities.stream()
                .mapToDouble(r -> {
                    LootRarity rarity = r.get();
                    return rarity.getWeight() + (luck * rarity.getQuality());
                })
                .sum();
        double roll = rand.nextDouble() * totalWeight;

        double currentWeight = 0;
        DynamicHolder<LootRarity> selected = validRarities.get(0);

        for (DynamicHolder<LootRarity> rarity : validRarities) {
            currentWeight += rarity.get().getWeight() + luckBonus;
            if (roll <= currentWeight) {
                selected = rarity;
                break;
            }
        }

        OtherworldApoth.LOGGER.debug("Selected rarity: {}", selected.getId());
        return selected.get();
    }

}