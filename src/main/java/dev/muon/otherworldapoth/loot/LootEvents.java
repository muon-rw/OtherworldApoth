package dev.muon.otherworldapoth.loot;

import dev.muon.otherworld.leveling.LevelingUtils;
import dev.muon.otherworldapoth.OtherworldApoth;
import dev.muon.otherworldapoth.config.OWApothConfig;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.loot.AffixLootEntry;
import dev.shadowsoffire.apotheosis.adventure.loot.AffixLootRegistry;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityClamp;
import dev.shadowsoffire.placebo.reload.WeightedDynamicRegistry.IDimensional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.champions.api.IChampion;
import top.theillusivec4.champions.common.capability.ChampionCapability;
import top.theillusivec4.champions.common.rank.Rank;

import java.util.function.Predicate;

public class LootEvents {

    public static final String PLAYER_DROPPED = "otherworldapoth.player_dropped";
    private static final String CHAMPION_ALE_GIVEN = "otherworldapoth.champion_ale_given";

    @SubscribeEvent
    public void onItemDrop(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (!LootCategory.forItem(stack).isNone()) {
            stack.getOrCreateTag().putBoolean(PLAYER_DROPPED, true);
        }
    }

    /**
     * Hands every champion a guaranteed affix item, mirroring Apotheosis' equip-on-spawn approach
     * ({@code AdventureEvents#special}). We hook {@link EntityJoinLevelEvent} at LOW priority rather
     * than {@code SpawnChampionEvent.Post} so we also cover champions whose rank is set up-front via
     * {@code ChampionBuilder#spawnPreset} (/champions summon, presets, data/structure-defined
     * champions) — those never fire the Post event. Champions assigns the rank either in its own
     * HIGHEST-priority join handler (natural spawns) or before the entity is added (presets), so the
     * rank is present by the time this LOW handler runs. A persistent flag guards against re-equipping
     * when the entity reloads or changes dimension. The gem is a separate roll handled at death by
     * {@link ChampionGemLootModifier}.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onChampionJoin(EntityJoinLevelEvent event) {
        if (!Apotheosis.enableAdventure || event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        int rankTier = ChampionCapability.getCapability(mob)
                .resolve()
                .map(IChampion::getServer)
                .flatMap(IChampion.Server::getRank)
                .map(Rank::getTier)
                .orElse(0);
        if (rankTier <= 0 || mob.getPersistentData().getBoolean(CHAMPION_ALE_GIVEN)) {
            return;
        }

        ServerLevel level = (ServerLevel) event.getLevel();
        RandomSource rand = mob.getRandom();
        // No killer exists at spawn; use the nearest player's luck, as Apotheosis does for its own
        // random-spawn affix items.
        Player nearest = level.getNearestPlayer(mob, -1.0D);
        float luck = nearest != null ? nearest.getLuck() : 0.0F;

        LootRarity rarity = LootUtils.getRarityForChampionRank(rankTier, rand, luck);
        AffixLootEntry entry = AffixLootRegistry.INSTANCE.getRandomItem(
                rand, luck, IDimensional.matches(level), clampAccepts(rarity));
        if (entry == null) {
            return;
        }
        ItemStack affixItem = LootController.createLootItem(entry.getStack(), entry.getType(), rarity, rand);
        if (affixItem.isEmpty()) {
            return;
        }
        EquipmentSlot[] slots = LootCategory.forItem(affixItem).getSlots();
        if (slots.length == 0) {
            return;
        }
        mob.getPersistentData().putBoolean(CHAMPION_ALE_GIVEN, true);
        LootUtils.markRandomSpawn(affixItem);
        mob.setItemSlot(slots[0], affixItem);
        mob.setGuaranteedDrop(slots[0]);
    }

    private static <T extends RarityClamp> Predicate<T> clampAccepts(LootRarity rarity) {
        int o = rarity.ordinal();
        return c -> c.getMinRarity().ordinal() <= o && o <= c.getMaxRarity().ordinal();
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
                                OWApothConfig.affixBaseChance + (level * OWApothConfig.affixLevelChanceIncrease)
                                        + (player.getLuck() * OWApothConfig.affixLuckFactor),
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
