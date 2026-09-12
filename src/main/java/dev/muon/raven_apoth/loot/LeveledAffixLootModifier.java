package dev.muon.raven_apoth.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.dynamic_difficulty.api.LevelingAPI;
import dev.shadowsoffire.apotheosis.loot.LootController;
import dev.shadowsoffire.apotheosis.loot.modifiers.ContextualLootModifier;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import org.jetbrains.annotations.NotNull;

public class LeveledAffixLootModifier extends ContextualLootModifier {
    public static final MapCodec<LeveledAffixLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst)
            .apply(inst, LeveledAffixLootModifier::new));

    // Champions' champion_loot GLM queries an inner table, which re-runs every GLM on the same thread.
    private static final ThreadLocal<Boolean> IS_PROCESSING = ThreadLocal.withInitial(() -> false);

    protected LeveledAffixLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx, GenContext gCtx) {
        if (IS_PROCESSING.get() || "champions".equals(ctx.getQueriedLootTableId().getNamespace())) {
            return loot;
        }
        if (!(ctx.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof LivingEntity living) || !LevelingAPI.hasLevel(living)) {
            return loot;
        }
        int level = LevelingAPI.getLevel(living);

        IS_PROCESSING.set(true);
        try {
            for (ItemStack stack : loot) {
                if (LootUtils.isConvertible(stack) && gCtx.rand().nextFloat() < LootUtils.affixChance(level, gCtx.luck())) {
                    LootController.createLootItem(stack, LootUtils.rarityForMobLevel(level, gCtx), gCtx);
                    LootUtils.markFromMob(stack);
                }
            }
        } finally {
            IS_PROCESSING.set(false);
        }
        return loot;
    }

    @Override
    public @NotNull MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
