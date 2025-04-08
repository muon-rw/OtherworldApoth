package dev.muon.otherworldapoth.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworld.leveling.LevelingUtils;
import dev.muon.otherworldapoth.config.OWApothConfig;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
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

public class LeveledAffixLootModifier extends LootModifier {
    public static final Codec<LeveledAffixLootModifier> CODEC = RecordCodecBuilder.create(inst -> codecStart(inst)
            .apply(inst, LeveledAffixLootModifier::new));

    protected LeveledAffixLootModifier(LootItemCondition[] conditions) {
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

        for (ItemStack stack : generatedLoot) {
            if (!LootCategory.forItem(stack).isNone() &&
                    AffixHelper.getAffixes(stack).isEmpty() &&
                    shouldConvertItem(level, context.getRandom())) {
                LootRarity rarity = LootUtils.getRarityForMobLevel(level, context.getRandom(), context.getLuck(), false);
                LootController.createLootItem(stack, rarity, context.getRandom());
            }
        }

        return generatedLoot;
    }

    private boolean shouldConvertItem(int level, RandomSource rand) {
        return rand.nextFloat() < Math.min(
                OWApothConfig.affixBaseChance + (level * OWApothConfig.affixLevelChanceIncrease),
                OWApothConfig.affixMaxChance
        );
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}