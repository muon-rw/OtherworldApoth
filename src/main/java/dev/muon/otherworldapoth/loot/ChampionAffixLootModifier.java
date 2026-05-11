package dev.muon.otherworldapoth.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.adventure.loot.AffixLootEntry;
import dev.shadowsoffire.apotheosis.adventure.loot.AffixLootRegistry;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityClamp;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemRegistry;
import dev.shadowsoffire.placebo.reload.WeightedDynamicRegistry.IDimensional;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import top.theillusivec4.champions.api.IChampion;
import top.theillusivec4.champions.common.capability.ChampionCapability;
import top.theillusivec4.champions.common.rank.Rank;

/**
 * Global loot modifier for champion entity deaths.
 * When the champions:entity_champion + killed_by_player conditions pass:
 * Injects EITHER a fresh affix loot item OR a gem (50/50 roll), with rarity scaled to the champion's rank.
 * Rank tier is read from the Champions capability and mapped to a rarity range via
 * {@link dev.muon.otherworldapoth.config.OWApothConfig#championRankRarityMappings}.
 */
public class ChampionAffixLootModifier extends LootModifier {

    public static final Codec<ChampionAffixLootModifier> CODEC =
            RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, ChampionAffixLootModifier::new));

    // Prevents re-entry when the Champions champion_loot GLM calls lootTable.getRandomItems on
    // a separate table, which re-runs all GLMs including this one. See ChampionLootModifier#doApply.
    private static final ThreadLocal<Boolean> IS_PROCESSING = ThreadLocal.withInitial(() -> false);

    public ChampionAffixLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!Apotheosis.enableAdventure || IS_PROCESSING.get()) {
            return generatedLoot;
        }

        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (!(entity instanceof LivingEntity living)) {
            return generatedLoot;
        }

        int rankTier = getChampionRankTier(living);
        if (rankTier <= 0) {
            return generatedLoot;
        }

        IS_PROCESSING.set(true);
        try {
            float luck = context.getLuck();
            LootRarity rarity = LootUtils.getRarityForChampionRank(rankTier, context.getRandom(), luck);

            if (context.getRandom().nextBoolean()) {
                AffixLootEntry entry = AffixLootRegistry.INSTANCE.getRandomItem(
                        context.getRandom(),
                        luck,
                        IDimensional.matches(context.getLevel()),
                        clampAccepts(rarity)
                );
                if (entry != null) {
                    ItemStack affixItem = LootController.createLootItem(
                            entry.getStack(),
                            entry.getType(),
                            rarity,
                            context.getRandom()
                    );
                    if (!affixItem.isEmpty()) {
                        generatedLoot.add(affixItem);
                    }
                }
            } else {
                Gem gem = GemRegistry.INSTANCE.getRandomItem(
                        context.getRandom(),
                        luck,
                        IDimensional.matches(context.getLevel())
                );
                if (gem != null) {
                    generatedLoot.add(GemRegistry.createGemStack(gem, rarity));
                }
            }
        } finally {
            IS_PROCESSING.set(false);
        }

        return generatedLoot;
    }

    private static int getChampionRankTier(LivingEntity entity) {
        return ChampionCapability.getCapability(entity)
                .resolve()
                .map(IChampion::getServer)
                .flatMap(IChampion.Server::getRank)
                .map(Rank::getTier)
                .orElse(0);
    }

    private static <T extends RarityClamp> java.util.function.Predicate<T> clampAccepts(LootRarity rarity) {
        int o = rarity.ordinal();
        return c -> c.getMinRarity().ordinal() <= o && o <= c.getMaxRarity().ordinal();
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
