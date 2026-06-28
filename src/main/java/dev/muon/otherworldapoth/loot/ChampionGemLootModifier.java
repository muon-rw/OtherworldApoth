package dev.muon.otherworldapoth.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldapoth.config.OWApothConfig;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemRegistry;
import dev.shadowsoffire.placebo.reload.WeightedDynamicRegistry.IDimensional;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
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
 * Global loot modifier for champion entity deaths — gem drop only.
 * <p>
 * The guaranteed affix item is handed to the champion at spawn time (see
 * {@link LootEvents#onChampionSpawn}, mirroring Apotheosis' equip-on-spawn approach). This modifier
 * handles the <em>additional</em> gem drop: when the champions:entity_champion + killed_by_player
 * conditions pass, it rolls a luck-scaled chance to add a gem whose rarity is scaled to the champion's
 * rank via {@link dev.muon.otherworldapoth.config.OWApothConfig#championRankRarityMappings}.
 */
public class ChampionGemLootModifier extends LootModifier {

    public static final Codec<ChampionGemLootModifier> CODEC =
            RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, ChampionGemLootModifier::new));

    // Prevents re-entry when the Champions champion_loot GLM calls lootTable.getRandomItems on
    // a separate table, which re-runs all GLMs including this one. See ChampionLootModifier#doApply.
    private static final ThreadLocal<Boolean> IS_PROCESSING = ThreadLocal.withInitial(() -> false);

    public ChampionGemLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!Apotheosis.enableAdventure || IS_PROCESSING.get()) {
            return generatedLoot;
        }
        // Champions' own GLM calls lootTable.getRandomItems on champions:champion_loot, which
        // triggers a fresh GLM pass with the inner table id. Skip those re-entrant invocations
        // so the modifier fires exactly once per actual mob-death loot table run.
        ResourceLocation queriedId = context.getQueriedLootTableId();
        if (queriedId != null && "champions".equals(queriedId.getNamespace())) {
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
            float gemChance = (float) Math.min(
                    OWApothConfig.championGemChance + luck * OWApothConfig.championGemLuckFactor,
                    1.0);
            if (context.getRandom().nextFloat() < gemChance) {
                LootRarity rarity = LootUtils.getRarityForChampionRank(rankTier, context.getRandom(), luck);
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

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
