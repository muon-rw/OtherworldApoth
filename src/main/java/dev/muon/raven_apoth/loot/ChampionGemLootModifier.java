package dev.muon.raven_apoth.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_apoth.config.RavenApothConfig;
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

import java.util.OptionalInt;

public class ChampionGemLootModifier extends ContextualLootModifier {
    public static final MapCodec<ChampionGemLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst)
            .apply(inst, ChampionGemLootModifier::new));

    // Champions' champion_loot GLM queries an inner table, which re-runs every GLM on the same thread.
    private static final ThreadLocal<Boolean> IS_PROCESSING = ThreadLocal.withInitial(() -> false);

    protected ChampionGemLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx, GenContext gCtx) {
        if (IS_PROCESSING.get() || "champions".equals(ctx.getQueriedLootTableId().getNamespace())) {
            return loot;
        }
        if (!(ctx.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof LivingEntity living)) {
            return loot;
        }
        OptionalInt tier = ChampionRanks.tierOf(living);
        if (tier.isEmpty()) {
            return loot;
        }

        IS_PROCESSING.set(true);
        try {
            float chance = (float) Math.min(RavenApothConfig.championGemChance + gCtx.luck() * RavenApothConfig.championGemLuckFactor, 1.0);
            if (gCtx.rand().nextFloat() < chance) {
                Gem gem = GemRegistry.INSTANCE.getRandomItem(gCtx);
                if (gem != null) {
                    loot.add(gem.toStack(LootUtils.purityForChampionTier(tier.getAsInt(), gCtx)));
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
