package dev.muon.raven_apoth.config;

import dev.muon.raven_apoth.RavenApoth;
import dev.muon.raven_apoth.tiers.WorldTierLevels;
import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.placebo.config.Configuration;

public class RavenApothConfig {
    private static final String RARITY_NAMES =
            "Rarity names may be bare (apotheosis namespace; bare 'ancient' means ancientreforging:ancient) or full ids.\n" +
            "Within a range the rarity is rolled with Apotheosis tier weights and Luck: the spawn tier of the mob for mob drops, the tier of the opener for chests.";
    public static double affixBaseChance;
    public static double affixLevelChanceIncrease;
    public static double affixMaxChance;
    public static double affixLuckFactor;
    public static double gemBaseChance;
    public static double gemLevelChanceIncrease;
    public static double gemMaxChance;
    public static double chestLootBaseChance;
    public static double chestLootLevelChanceIncrease;
    public static double chestLootMaxChance;
    public static double championGemChance;
    public static double championGemRankBonus;
    public static int championLevelBonus;
    public static double gemLuckFactor;
    public static double tierAffixChanceBonus;
    public static double tierGemChanceBonus;
    public static double bossAffixChance;
    public static double bossGemChance;
    public static double spawnerChanceFactor;

    public static String[] levelRarityMappings;
    public static String[] playerLevelRarityMappings;
    public static boolean gateWorldTiersByLevel;
    public static String[] worldTierLevels;

    public static void init() {
        Configuration config = new Configuration(ApothicAttributes.getConfigFile(RavenApoth.MODID));
        config.setTitle("Raven Apotheosis Configuration");

        String category = "loot";

        affixBaseChance = config.getFloat("affixBaseChance", category, 0.05f, 0, 1,
                "Base chance for an item to receive affixes");
        affixLevelChanceIncrease = config.getFloat("affixLevelChanceIncrease", category, 0.01f, 0, 1,
                "How much the affix chance increases per level");
        affixMaxChance = config.getFloat("affixMaxChance", category, 0.25f, 0, 1,
                "Maximum level-based chance for an item to receive affixes; tier and Luck bonuses apply on top");
        affixLuckFactor = config.getFloat("affixLuckFactor", category, 0.01f, 0, 1,
                "How much each point of the killing player's Luck adds to the level-based affix conversion chance");

        tierAffixChanceBonus = config.getFloat("tierAffixChanceBonus", category, 0.02f, 0, 1,
                "Added to the affix conversion chance per world tier above Haven, using the tier the mob spawned under");

        championLevelBonus = config.getInt("championLevelBonus", category, 3, 0, 100,
                "Levels a champion counts as being above its real level, per champion rank, when picking its rarity range");
        championGemChance = config.getFloat("championGemChance", category, 0.25f, 0, 1,
                "Added to the gem drop chance of any champion, on top of its guaranteed affix item");
        championGemRankBonus = config.getFloat("championGemRankBonus", category, 0.15f, 0, 1,
                "Added to the gem drop chance per champion rank; with the defaults a rank 5 champion always drops a gem");

        gemBaseChance = config.getFloat("gemBaseChance", category, 0.02f, 0, 1,
                "Base chance for a gem to drop");
        gemLevelChanceIncrease = config.getFloat("gemLevelChanceIncrease", category, 0.005f, 0, 1,
                "How much the gem drop chance increases per level");
        gemMaxChance = config.getFloat("gemMaxChance", category, 0.15f, 0, 1,
                "Maximum level-based chance for a gem to drop; tier, champion and Luck bonuses apply on top");
        gemLuckFactor = config.getFloat("gemLuckFactor", category, 0.005f, 0, 1,
                "How much each point of Luck on the killing player adds to the gem drop chance");
        tierGemChanceBonus = config.getFloat("tierGemChanceBonus", category, 0.01f, 0, 1,
                "Added to the gem drop chance per world tier above Haven, using the tier the mob spawned under");

        bossAffixChance = config.getFloat("bossAffixChance", category, 1.0f, 0, 1,
                "Minimum affix conversion chance for the gear dropped by mobs in the c:bosses entity tag");
        bossGemChance = config.getFloat("bossGemChance", category, 1.0f, 0, 1,
                "Minimum gem drop chance for mobs in the c:bosses entity tag");

        spawnerChanceFactor = config.getFloat("spawnerChanceFactor", category, 0.5f, 0, 1,
                "Multiplier on the affix and gem chances of mobs that came from a spawner; 0 makes them drop nothing.\n" +
                        "Only hostile mobs that spawned on their own (or from a spawner) and mobs in c:bosses ever roll loot.");

        chestLootBaseChance = config.getFloat("chestLootBaseChance", category, 0.15f, 0, 1,
                "Base chance for a chest item to receive affixes");
        chestLootLevelChanceIncrease = config.getFloat("chestLootLevelChanceIncrease", category, 0.02f, 0, 1,
                "How much the chest loot affix chance increases per player level");
        chestLootMaxChance = config.getFloat("chestLootMaxChance", category, 0.5f, 0, 1,
                "Maximum chance for a chest item to receive affixes");

        levelRarityMappings = config.getStringList("levelAffixMappings", category,
                new String[] {
                        "25=mythic-ancient",
                        "20=epic-mythic",
                        "15=rare-epic",
                        "10=uncommon-rare",
                        "5=common-uncommon",
                        "1=common-common"
                },
                "Mob level to rarity range for mob drops; gem purity follows the same range (common..ancient as cracked..perfect).\n" +
                        "Format: 'level=minRarity-maxRarity'. Champions count as championLevelBonus levels higher per rank.\n" +
                        "Each entry applies to levels from its value up to (but not including) the next threshold.\n" +
                        "For example: '20=epic-mythic' applies to levels 20-24 if the next threshold is 25.\n" +
                        RARITY_NAMES);

        playerLevelRarityMappings = config.getStringList("playerLevelMappings", category,
                new String[] {
                        "16=rare-mythic",
                        "12=rare-epic",
                        "6=uncommon-rare",
                        "4=common-uncommon",
                        "2=common-common"
                },
                "Level threshold to rarity mapping for container loot based on player level.\n" +
                        "Each entry applies to levels from its value up to (but not including) the next threshold.\n" +
                        "For example: '12=rare-epic' applies to levels 12-15 if the next threshold is 16.\n" +
                        RARITY_NAMES);

        String tiers = "world_tiers";
        gateWorldTiersByLevel = config.getBoolean("gateWorldTiersByLevel", tiers, true,
                "Unlock Apotheosis world tiers by player level (worldTierLevels) instead of Apotheosis' progression advancements.");
        worldTierLevels = config.getStringList("worldTierLevels", tiers,
                new String[] {
                        "frontier=5",
                        "ascent=10",
                        "summit=15",
                        "pinnacle=20"
                },
                "Player level required to unlock each world tier. Format: 'tier=level'. Haven is always unlocked.");
        WorldTierLevels.reload();

        if (config.hasChanged()) {
            config.save();
        }
    }
}
