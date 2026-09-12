package dev.muon.raven_apoth.mixin;

import dev.muon.raven_apoth.loot.LootUtils;
import dev.muon.raven_core.leveling.LevelingUtils;
import dev.shadowsoffire.apotheosis.loot.LootController;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LootTable.class)
public abstract class LootTableMixin {

    @Inject(
            method = "fill(Lnet/minecraft/world/Container;Lnet/minecraft/world/level/storage/loot/LootParams;J)V",
            at = @At("RETURN")
    )
    private void raven_apoth$convertContainerLoot(Container container, LootParams params, long seed, CallbackInfo ci) {
        if (!(params.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof Player player)) return;
        int level = LevelingUtils.getPlayerLevel(player);
        if (level < 1) return;

        Vec3 origin = params.getOptionalParameter(LootContextParams.ORIGIN);
        BlockPos pos = origin != null ? BlockPos.containing(origin) : player.blockPosition();
        GenContext gCtx = GenContext.forPlayerAtPos(player.getRandom(), player, pos);
        float chance = LootUtils.chestChance(level);

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!LootUtils.isConvertible(stack) || player.getRandom().nextFloat() >= chance) continue;
            LootController.createLootItem(stack, LootUtils.rarityForPlayerLevel(level, gCtx), gCtx);
            LootUtils.markFromChest(stack);
        }
    }
}
