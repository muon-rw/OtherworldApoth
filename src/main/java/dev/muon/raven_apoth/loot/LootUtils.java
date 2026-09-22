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

import java.util.ArrayList;
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

    public static GenContext withTier(GenContext ctx, WorldTier tier) {
        return new GenContext(ctx.rand(), tier, ctx.luck(), ctx.dimension(), ctx.biome(), ctx.stages());
    }

    public static List<LootRarity> mobRarityBracket(int effectiveLevel) {
        return rarityBracket(findMappingForLevel(RavenApothConfig.levelRarityMappings, effectiveLevel));
    }

    public static LootRarity rollRarity(List<LootRarity> bracket, GenContext ctx) {
        LootRarity rolled = RarityRegistry.INSTANCE.getRandomItem(ctx, new LinkedHashSet<>(bracket));
        return rolled != null ? rolled : bracket.getFirst();
    }

    public static LootRarity rarityForPlayerLevel(int level, GenContext ctx) {
        return rollRarity(rarityBracket(findMappingForLevel(RavenApothConfig.playerLevelRarityMappings, level)), ctx);
    }

    /**
     * Gems follow the same bracket as gear: a rarity's position among the sorted rarities is the purity's ordinal,
     * so common..ancient lines up with cracked..perfect.
     */
    public static Set<Purity> purityBracket(List<LootRarity> rarityBracket) {
        List<LootRarity> sorted = RarityRegistry.getSortedRarities();
        Purity[] purities = Purity.values();
        int lo = Math.clamp(sorted.indexOf(rarityBracket.getFirst()), 0, purities.length - 1);
        int hi = Math.clamp(sorted.indexOf(rarityBracket.getLast()), lo, purities.length - 1);
        return EnumSet.range(purities[lo], purities[hi]);
    }

    public static Purity rollPurity(Set<Purity> bracket, GenContext ctx) {
        return Purity.random(ctx, bracket);
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

    private static List<LootRarity> rarityBracket(String mapping) {
        List<LootRarity> sorted = RarityRegistry.getSortedRarities();
        String[] bounds = mapping.split("-");
        int min = Integer.MIN_VALUE;
        int max = Integer.MAX_VALUE;
        LootRarity minRarity = resolveRarity(bounds[0]);
        if (minRarity != null) min = minRarity.sortIndex();
        LootRarity maxRarity = resolveRarity(bounds.length > 1 ? bounds[1] : bounds[0]);
        if (maxRarity != null) max = maxRarity.sortIndex();

        List<LootRarity> bracket = new ArrayList<>();
        for (LootRarity rarity : sorted) {
            if (rarity.sortIndex() >= min && rarity.sortIndex() <= max) {
                bracket.add(rarity);
            }
        }
        if (bracket.isEmpty()) {
            warnOnce("emptyBracket:" + mapping, "Rarity range '{}' matches no rarity, using the lowest rarity", mapping);
            bracket.add(sorted.getFirst());
        }
        return bracket;
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

    public static ResourceLocation rarityId(String name) {
        if (name.contains(":")) return ResourceLocation.parse(name);
        if (name.equals("ancient")) return ResourceLocation.parse(ANCIENT_RARITY);
        return Apotheosis.loc(name);
    }

    private static void warnOnce(String key, String message, Object arg) {
        if (WARNED.add(key)) {
            RavenApoth.LOGGER.warn(message, arg);
        }
    }
}
