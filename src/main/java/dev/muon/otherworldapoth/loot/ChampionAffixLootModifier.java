package dev.muon.otherworldapoth.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.adventure.loot.AffixLootEntry;
import dev.shadowsoffire.apotheosis.adventure.loot.AffixLootRegistry;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
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
 * 1. Injects a fresh affix loot item drawn from the AffixLootRegistry, with rarity scaled to the champion's rank.
 * 2. Injects a gem with quality scaled to the champion's rank.
 * Rank tier (1-5, 5 = Ultimate) is read from the Champions capability.
 */
public class ChampionAffixLootModifier extends LootModifier {

    public static final Codec<ChampionAffixLootModifier> CODEC =
            RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, ChampionAffixLootModifier::new));

    public ChampionAffixLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!Apotheosis.enableAdventure) {
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

        float luck = context.getLuck();

        AffixLootEntry entry = AffixLootRegistry.INSTANCE.getRandomItem(
                context.getRandom(),
                luck,
                IDimensional.matches(context.getLevel())
        );
        if (entry != null) {
            LootRarity itemRarity = LootUtils.getRarityForChampionRank(rankTier, context.getRandom(), luck);
            ItemStack affixItem = LootController.createLootItem(
                    entry.getStack(),
                    entry.getType(),
                    itemRarity,
                    context.getRandom()
            );
            if (!affixItem.isEmpty()) {
                generatedLoot.add(affixItem);
            }
        }

        Gem gem = GemRegistry.INSTANCE.getRandomItem(
                context.getRandom(),
                luck,
                IDimensional.matches(context.getLevel())
        );
        if (gem != null) {
            LootRarity gemRarity = LootUtils.getRarityForChampionRank(rankTier, context.getRandom(), luck);
            generatedLoot.add(GemRegistry.createGemStack(gem, gemRarity));
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
