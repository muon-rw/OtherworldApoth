package dev.muon.otherworldapoth.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworld.leveling.LevelingUtils;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
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

/**
 * Global loot modifier for champion entity deaths.
 * Uses Champions' entity_is_champion condition - when conditions pass (champion + killed by player):
 * 1. Guarantees that all dropped affixable items are converted to level-appropriate rarity.
 * 2. Always injects one gem with quality based on the champion's level.
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

        int level = LevelingUtils.getEntityLevel(living);
        if (level <= 0) {
            return generatedLoot;
        }

        float luck = context.getLuck();

        // Guarantee conversion of dropped items to level-appropriate rarity
        for (ItemStack stack : generatedLoot) {
            if (!LootCategory.forItem(stack).isNone() && AffixHelper.getAffixes(stack).isEmpty()) {
                LootRarity rarity = LootUtils.getRarityForMobLevel(level, context.getRandom(), luck, false);
                LootController.createLootItem(stack, rarity, context.getRandom());
            }
        }

        // Always inject a gem with quality based on champion level
        LootRarity gemRarity = LootUtils.getRarityForMobLevel(level, context.getRandom(), luck, true);
        Gem gem = GemRegistry.INSTANCE.getRandomItem(
                context.getRandom(),
                luck,
                IDimensional.matches(context.getLevel())
        );
        if (gem != null) {
            ItemStack gemStack = GemRegistry.createGemStack(gem, gemRarity);
            generatedLoot.add(gemStack);
        }

        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
