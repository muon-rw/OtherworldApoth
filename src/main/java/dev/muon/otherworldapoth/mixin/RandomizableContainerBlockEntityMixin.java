package dev.muon.otherworldapoth.mixin;

import dev.muon.medieval.leveling.LevelingUtils;
import dev.muon.otherworldapoth.config.LootConfig;
import dev.muon.otherworldapoth.loot.LootUtils;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(RandomizableContainerBlockEntity.class)
public abstract class RandomizableContainerBlockEntityMixin {

    @Inject(
            method = "unpackLootTable",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/loot/LootTable;fill(Lnet/minecraft/world/Container;Lnet/minecraft/world/level/storage/loot/LootParams;J)V",
                    shift = At.Shift.AFTER
            )
    )
    private void otherworldapoth$modifyChestLoot(@Nullable Player player, CallbackInfo ci) {
        if (player == null) return;

        RandomizableContainerBlockEntity self = (RandomizableContainerBlockEntity)(Object)this;
        int level = LevelingUtils.getPlayerLevel(player);
        if (level < 1) return;

        float conversionChance = (float) Math.min(
                LootConfig.chestLootBaseChance + (level * LootConfig.chestLootLevelChanceIncrease),
                LootConfig.chestLootMaxChance
        );

        for (int i = 0; i < self.getContainerSize(); i++) {
            ItemStack stack = self.getItem(i);

            if (!LootCategory.forItem(stack).isNone() &&
                    AffixHelper.getAffixes(stack).isEmpty() &&
                    player.getRandom().nextFloat() < conversionChance) {

                LootRarity rarity = LootUtils.getRarityForPlayerLevel(
                        level,
                        player.getRandom(),
                        player.getLuck()
                );
                LootController.createLootItem(stack, rarity, player.getRandom());
            }
        }
    }
}