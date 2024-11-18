package dev.muon.otherworldapoth.mixin;

import dev.muon.medieval.leveling.LevelingUtils;
import dev.muon.otherworldapoth.config.LootConfig;
import dev.muon.otherworldapoth.loot.LootUtils;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static dev.muon.otherworldapoth.loot.LootEvents.PLAYER_DROPPED;

@Mixin(Mob.class)
public abstract class MobMixin {

    // Forge's onLivingDrops is not called in dropCustomDeathLoot, instead they spawn the item directly into the world.

    @ModifyVariable(
            method = "dropCustomDeathLoot",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Mob;spawnAtLocation(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"),
            ordinal = 0
    )
    private ItemStack otherworldapoth$modifyEquipmentDrop(ItemStack stack, DamageSource source) {
        Mob self = (Mob)(Object)this;
        if (self instanceof Monster monster &&
                source.getEntity() instanceof Player player &&
                !LootCategory.forItem(stack).isNone() &&
                AffixHelper.getAffixes(stack).isEmpty()) {
            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.getBoolean(PLAYER_DROPPED)) {
                int level = LevelingUtils.getEntityLevel(monster);
                if (level > 0) {
                    double chance = Math.min(
                            LootConfig.affixBaseChance + (level * LootConfig.affixLevelChanceIncrease),
                            LootConfig.affixMaxChance
                    );

                    if (monster.getRandom().nextFloat() < chance) {
                        LootRarity rarity = LootUtils.getRarityForMobLevel(
                                level,
                                monster.getRandom(),
                                player.getLuck(),
                                false
                        );
                        LootController.createLootItem(stack, rarity, monster.getRandom());
                    }
                }
            }
        }
        return stack;
    }
}
