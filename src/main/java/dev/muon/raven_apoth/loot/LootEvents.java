package dev.muon.raven_apoth.loot;

import dev.muon.dynamic_difficulty.api.LevelingAPI;
import dev.muon.raven_apoth.RavenApothComponents;
import dev.shadowsoffire.apotheosis.loot.AffixLootEntry;
import dev.shadowsoffire.apotheosis.loot.AffixLootRegistry;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootController;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.tiers.Constraints;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import dev.shadowsoffire.apothic_attributes.modifiers.EquipmentSlotCompat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.Arrays;
import java.util.OptionalInt;
import java.util.function.Predicate;

public class LootEvents {

    private static final String CHAMPION_ALE_GIVEN = "raven_apoth.champion_ale_given";

    @SubscribeEvent
    public void onItemToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (!LootCategory.forItem(stack).isNone()) {
            stack.set(RavenApothComponents.PLAYER_DROPPED, true);
        }
    }

    // Mob.dropCustomDeathLoot empties each slot as it drops it, so the death event is the last
    // moment equipped gear can be converted in place; loot-table drops go through LeveledAffixLootModifier.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Monster monster)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || !LevelingAPI.hasLevel(monster)) {
            return;
        }
        int level = LevelingAPI.getLevel(monster);
        GenContext gCtx = GenContext.forPlayerAtPos(monster.getRandom(), player, monster.blockPosition());
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = monster.getItemBySlot(slot);
            if (LootUtils.isConvertible(stack) && gCtx.rand().nextFloat() < LootUtils.affixChance(level, player.getLuck())) {
                LootController.createLootItem(stack, LootUtils.rarityForMobLevel(level, gCtx), gCtx);
                LootUtils.markFromMob(stack);
            }
        }
    }

    // Champions rolls natural champions in its own EntityJoinLevelEvent listener at NORMAL priority;
    // commands and presets write the champion data before the entity is added, so LOW sees both.
    @SubscribeEvent(priority = EventPriority.LOW)
    @SuppressWarnings("UnstableApiUsage")
    public void onChampionJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (mob.getPersistentData().getBoolean(CHAMPION_ALE_GIVEN)) {
            return;
        }
        OptionalInt tier = ChampionRanks.tierOf(mob);
        if (tier.isEmpty()) {
            return;
        }

        Player nearest = level.getNearestPlayer(mob, -1.0D);
        GenContext gCtx = nearest != null
                ? GenContext.forPlayerAtPos(mob.getRandom(), nearest, mob.blockPosition())
                : GenContext.standalone(mob.getRandom(), WorldTier.HAVEN, 0, level, mob.blockPosition());
        LootRarity rarity = LootUtils.rarityForChampionTier(tier.getAsInt(), gCtx);
        Predicate<AffixLootEntry> acceptsRarity = e -> e.rarities().isEmpty() || e.rarities().contains(rarity);
        AffixLootEntry entry = AffixLootRegistry.INSTANCE.getRandomItem(gCtx, Constraints.eval(gCtx), acceptsRarity);
        if (entry == null) {
            return;
        }
        ItemStack item = LootController.createLootItem(entry.stack().copy(), entry.getType(), rarity, gCtx);
        if (item.isEmpty()) {
            return;
        }

        LootCategory cat = LootCategory.forItem(item);
        EquipmentSlot slot = Arrays.stream(EquipmentSlot.values())
                .filter(eSlot -> cat.getSlots().test(EquipmentSlotCompat.fromVanilla(eSlot)))
                .findFirst()
                .orElse(EquipmentSlot.MAINHAND);
        LootUtils.markFromMob(item);
        mob.setItemSlot(slot, item);
        mob.setGuaranteedDrop(slot);
        mob.getPersistentData().putBoolean(CHAMPION_ALE_GIVEN, true);
    }
}
