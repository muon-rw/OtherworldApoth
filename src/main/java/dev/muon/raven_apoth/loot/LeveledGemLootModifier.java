package dev.muon.raven_apoth.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.loot.modifiers.ContextualLootModifier;
import dev.shadowsoffire.apotheosis.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.socket.gem.GemRegistry;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import org.jetbrains.annotations.NotNull;

public class LeveledGemLootModifier extends ContextualLootModifier {
    public static final MapCodec<LeveledGemLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst)
            .apply(inst, LeveledGemLootModifier::new));

    // Champions' champion_loot GLM queries an inner table, which re-runs every GLM on the same thread.
    private static final ThreadLocal<Boolean> IS_PROCESSING = ThreadLocal.withInitial(() -> false);

    protected LeveledGemLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx, GenContext gCtx) {
        if (IS_PROCESSING.get() || "champions".equals(ctx.getQueriedLootTableId().getNamespace())) {
            return loot;
        }
        if (!(ctx.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof LivingEntity living) || !DropProfile.isEligible(living)) {
            return loot;
        }
        DropProfile profile = DropProfile.of(living, gCtx.luck());

        IS_PROCESSING.set(true);
        try {
            if (gCtx.rand().nextFloat() < profile.gemChance()) {
                Gem gem = GemRegistry.INSTANCE.getRandomItem(profile.context(gCtx));
                if (gem != null) {
                    loot.add(gem.toStack(profile.rollPurity(gCtx)));
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
