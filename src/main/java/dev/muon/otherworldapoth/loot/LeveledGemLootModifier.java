package dev.muon.otherworldapoth.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.medieval.leveling.LevelingUtils;
import dev.muon.otherworldapoth.config.LootConfig;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemRegistry;
import dev.shadowsoffire.placebo.reload.WeightedDynamicRegistry.IDimensional;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

public class LeveledGemLootModifier extends LootModifier {
    public static final Codec<LeveledGemLootModifier> CODEC = RecordCodecBuilder.create(inst -> codecStart(inst)
            .apply(inst, LeveledGemLootModifier::new));

    protected LeveledGemLootModifier(LootItemCondition[] conditions) {
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

        if (shouldDropGem(level, context.getRandom())) {
            LootRarity rarity = LootUtils.getRarityForMobLevel(level, context.getRandom(), context.getLuck(), true);

            Gem gem = GemRegistry.INSTANCE.getRandomItem(
                    context.getRandom(),
                    context.getLuck(),
                    IDimensional.matches(context.getLevel())
            );

            if (gem != null) {
                ItemStack gemStack = GemRegistry.createGemStack(gem, rarity);
                generatedLoot.add(gemStack);
            }
        }

        return generatedLoot;
    }

    private boolean shouldDropGem(int level, RandomSource rand) {
        return rand.nextFloat() < Math.min(
                LootConfig.gemBaseChance + (level * LootConfig.gemLevelChanceIncrease),
                LootConfig.gemMaxChance
        );
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
