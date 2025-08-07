package dev.muon.otherworldapoth.loot;

import dev.muon.otherworld.leveling.LevelingUtils;
import dev.muon.otherworldapoth.OtherworldApoth;
import dev.muon.otherworldapoth.config.OWApothConfig;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class LootEvents {

    public static final String PLAYER_DROPPED = "otherworldapoth.player_dropped";

    @SubscribeEvent
    public void onItemDrop(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (!LootCategory.forItem(stack).isNone()) {
            stack.getOrCreateTag().putBoolean(PLAYER_DROPPED, true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Monster monster)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        int level = LevelingUtils.getEntityLevel(monster);
        if (level <= 0) {
            return;
        }

        OtherworldApoth.LOGGER.debug("Processing drops for level {} {}", level, monster.getType().getDescription());
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack equipped = monster.getItemBySlot(slot);
            if (equipped.isEmpty()) {
                continue;
            }

            event.getDrops().forEach(drop -> {
                ItemStack stack = drop.getItem();

                if (stack.getItem() == equipped.getItem() &&
                        !LootCategory.forItem(stack).isNone() &&
                        AffixHelper.getAffixes(stack).isEmpty()) {
                    CompoundTag tag = stack.getTag();
                    if (tag == null) {
                        return;
                    }
                    if (tag.getBoolean(PLAYER_DROPPED)) {
                        float chance = (float) Math.min(
                                OWApothConfig.affixBaseChance + (level * OWApothConfig.affixLevelChanceIncrease),
                                OWApothConfig.affixMaxChance);
                        float roll = monster.getRandom().nextFloat();

                        if (roll < chance) {
                            LootRarity rarity = LootUtils.getRarityForMobLevel(level, monster.getRandom(), player.getLuck(), false);
                            LootController.createLootItem(stack, rarity, monster.getRandom());
                        }
                    }
                }
            });
        }
    }

}