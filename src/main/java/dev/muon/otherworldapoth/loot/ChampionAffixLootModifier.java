package dev.muon.otherworldapoth.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworld.leveling.LevelingUtils;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/**
 * Global loot modifier that adds a guaranteed affix item drop when a champion entity dies.
 * Uses Champions' entity_is_champion condition - when conditions pass (champion + killed by player),
 * adds one random affix item with level-based rarity.
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

        Player player = LootUtils.findRelevantPlayer(context);
        if (player == null) {
            return generatedLoot;
        }

        int level = LevelingUtils.getEntityLevel(living);
        if (level <= 0) {
            return generatedLoot;
        }

        LootRarity rarity = LootUtils.getRarityForMobLevel(level, context.getRandom(), player.getLuck(), false);
        ItemStack affixItem = LootController.createRandomLootItem(
                context.getRandom(),
                rarity,
                player,
                context.getLevel()
        );

        if (!affixItem.isEmpty()) {
            generatedLoot.add(affixItem);
        }

        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
