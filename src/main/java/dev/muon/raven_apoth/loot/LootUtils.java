package dev.muon.raven_apoth.loot;

import dev.muon.raven_apoth.RavenApoth;
import dev.muon.raven_apoth.RavenApothComponents;
import dev.muon.raven_apoth.config.RavenApothConfig;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class LootUtils {
    private static final String ANCIENT_RARITY = "ancientreforging:ancient";
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private LootUtils() {}

    public static String findMappingForLevel(String[] mappings, int level) {
        return Arrays.stream(mappings)
                .map(s -> s.split("="))
                .filter(parts -> Integer.parseInt(parts[0]) <= level)
                .max(Comparator.comparingInt(parts -> Integer.parseInt(parts[0])))
                .map(parts -> parts[1])
                .orElse("common-common");
    }

    public static GenContext rollContext(GenContext ctx) {
        String tierName = RavenApothConfig.weightTier;
        if (tierName == null || tierName.equalsIgnoreCase("player")) {
            return ctx;
        }
        for (WorldTier tier : WorldTier.values()) {
            if (tier.getSerializedName().equalsIgnoreCase(tierName)) {
                return new GenContext(ctx.rand(), tier, ctx.luck(), ctx.dimension(), ctx.biome(), ctx.stages());
            }
        }
        warnOnce("weightTier:" + tierName, "Unknown weightTier '{}', using the player's tier", tierName);
        return ctx;
    }

    public static LootRarity rarityForMobLevel(int level, GenContext ctx) {
        return rollRarity(findMappingForLevel(RavenApothConfig.levelRarityMappings, level), ctx);
    }

    public static LootRarity rarityForPlayerLevel(int level, GenContext ctx) {
        return rollRarity(findMappingForLevel(RavenApothConfig.playerLevelRarityMappings, level), ctx);
    }

    public static LootRarity rarityForChampionTier(int tier, GenContext ctx) {
        return rollRarity(findMappingForLevel(RavenApothConfig.championRankRarityMappings, tier), ctx);
    }

    public static Purity purityForMobLevel(int level, GenContext ctx) {
        return rollPurity(findMappingForLevel(RavenApothConfig.levelPurityMappings, level), ctx);
    }

    public static Purity purityForChampionTier(int tier, GenContext ctx) {
        return rollPurity(findMappingForLevel(RavenApothConfig.championPurityMappings, tier), ctx);
    }

    public static float affixChance(int level, float luck) {
        double chance = RavenApothConfig.affixBaseChance
                + level * RavenApothConfig.affixLevelChanceIncrease
                + luck * RavenApothConfig.affixLuckFactor;
        return (float) Math.min(chance, RavenApothConfig.affixMaxChance);
    }

    public static float gemChance(int level) {
        double chance = RavenApothConfig.gemBaseChance + level * RavenApothConfig.gemLevelChanceIncrease;
        return (float) Math.min(chance, RavenApothConfig.gemMaxChance);
    }

    public static float chestChance(int playerLevel) {
        double chance = RavenApothConfig.chestLootBaseChance + playerLevel * RavenApothConfig.chestLootLevelChanceIncrease;
        return (float) Math.min(chance, RavenApothConfig.chestLootMaxChance);
    }

    public static boolean isConvertible(ItemStack stack) {
        return !LootCategory.forItem(stack).isNone()
                && !AffixHelper.hasAffixes(stack)
                && !stack.getOrDefault(RavenApothComponents.PLAYER_DROPPED.get(), false);
    }

    public static void markFromMob(ItemStack stack) {
        stack.set(Apoth.Components.FROM_MOB, true);
    }

    public static void markFromChest(ItemStack stack) {
        stack.set(Apoth.Components.FROM_CHEST, true);
    }

    private static LootRarity rollRarity(String mapping, GenContext ctx) {
        Set<LootRarity> pool = rarityPool(mapping);
        LootRarity rolled = RarityRegistry.INSTANCE.getRandomItem(rollContext(ctx), pool);
        return rolled != null ? rolled : pool.iterator().next();
    }

    private static Set<LootRarity> rarityPool(String mapping) {
        List<LootRarity> sorted = RarityRegistry.getSortedRarities();
        String[] bounds = mapping.split("-");
        int min = Integer.MIN_VALUE;
        int max = Integer.MAX_VALUE;
        LootRarity minRarity = resolveRarity(bounds[0]);
        if (minRarity != null) min = minRarity.sortIndex();
        LootRarity maxRarity = resolveRarity(bounds.length > 1 ? bounds[1] : bounds[0]);
        if (maxRarity != null) max = maxRarity.sortIndex();

        Set<LootRarity> pool = new LinkedHashSet<>();
        for (LootRarity rarity : sorted) {
            if (rarity.sortIndex() >= min && rarity.sortIndex() <= max) {
                pool.add(rarity);
            }
        }
        if (pool.isEmpty()) {
            warnOnce("emptyPool:" + mapping, "Rarity range '{}' matches no rarity, using the lowest rarity", mapping);
            pool.add(sorted.getFirst());
        }
        return pool;
    }

    private static LootRarity resolveRarity(String name) {
        ResourceLocation id = rarityId(name.trim());
        DynamicHolder<LootRarity> holder = RarityRegistry.INSTANCE.holder(id);
        if (!holder.isBound()) {
            warnOnce("rarity:" + id, "Rarity '{}' is not registered, dropping that bound", id);
            return null;
        }
        return holder.get();
    }

    private static ResourceLocation rarityId(String name) {
        if (name.contains(":")) return ResourceLocation.parse(name);
        if (name.equals("ancient")) return ResourceLocation.parse(ANCIENT_RARITY);
        return Apotheosis.loc(name);
    }

    private static Purity rollPurity(String mapping, GenContext ctx) {
        return Purity.random(rollContext(ctx), purityPool(mapping));
    }

    private static Set<Purity> purityPool(String mapping) {
        String[] bounds = mapping.split("-");
        Purity min = resolvePurity(bounds[0]);
        Purity max = resolvePurity(bounds.length > 1 ? bounds[1] : bounds[0]);
        int lo = min != null ? min.ordinal() : 0;
        int hi = max != null ? max.ordinal() : Purity.PERFECT.ordinal();
        if (lo > hi) {
            warnOnce("purityRange:" + mapping, "Purity range '{}' is inverted, using the lowest purity", mapping);
            return EnumSet.of(Purity.CRACKED);
        }
        return EnumSet.range(Purity.values()[lo], Purity.values()[hi]);
    }

    private static Purity resolvePurity(String name) {
        String trimmed = name.trim();
        for (Purity purity : Purity.values()) {
            if (purity.getSerializedName().equalsIgnoreCase(trimmed)) return purity;
        }
        warnOnce("purity:" + trimmed, "Purity '{}' is unknown, dropping that bound", trimmed);
        return null;
    }

    private static void warnOnce(String key, String message, Object arg) {
        if (WARNED.add(key)) {
            RavenApoth.LOGGER.warn(message, arg);
        }
    }
}
