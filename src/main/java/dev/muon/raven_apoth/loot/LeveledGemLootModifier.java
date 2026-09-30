package dev.muon.raven_apoth.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.loot.modifiers.ContextualLootModifier;
import dev.shadowsoffire.apotheosis.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.socket.gem.GemRegistry;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.CompoundTag;
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

    // One kill runs several loot tables with the same entity (Dynamic Difficulty's leveled_mobs table and
    // inject_level_drops GLM, Champions' champion_loot), and each one re-runs every GLM.
    private static final String GEM_ROLLED = "raven_apoth.gem_rolled";

    protected LeveledGemLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx, GenContext gCtx) {
        if (!(ctx.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof LivingEntity living) || !DropProfile.isEligible(living)) {
            return loot;
        }
        CompoundTag data = living.getPersistentData();
        if (data.getBoolean(GEM_ROLLED)) {
            return loot;
        }
        data.putBoolean(GEM_ROLLED, true);

        DropProfile profile = DropProfile.of(living, gCtx.luck());
        if (gCtx.rand().nextFloat() < profile.gemChance()) {
            Gem gem = GemRegistry.INSTANCE.getRandomItem(profile.context(gCtx));
            if (gem != null) {
                loot.add(gem.toStack(profile.rollPurity(gCtx)));
            }
        }
        return loot;
    }

    @Override
    public @NotNull MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
