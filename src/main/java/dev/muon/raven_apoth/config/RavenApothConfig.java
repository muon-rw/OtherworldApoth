package dev.muon.raven_apoth.config;

import dev.muon.raven_apoth.RavenApoth;
import dev.muon.raven_apoth.tiers.WorldTierLevels;
import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.placebo.config.Configuration;

public class RavenApothConfig {
    private static final String RARITY_NAMES =
            "Rarity names may be bare (apotheosis namespace; bare 'ancient' means ancientreforging:ancient) or full ids.\n" +
            "Within a range the rarity is rolled with Apotheosis tier weights (see weightTier) and Luck.";
    private static final String PURITY_NAMES =
            "Valid purities: cracked, chipped, flawed, normal, flawless, perfect.\n" +
            "Within a range the purity is rolled with Apotheosis tier weights (see weightTier) and Luck.";

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
    public static double championGemLuckFactor;

    public static String[] levelRarityMappings;
    public static String[] playerLevelRarityMappings;
    public static String[] championRankRarityMappings;
    public static String[] levelPurityMappings;
    public static String[] championPurityMappings;

    public static String weightTier;
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
                "Maximum chance for an item to receive affixes");
        affixLuckFactor = config.getFloat("affixLuckFactor", category, 0.01f, 0, 1,
                "How much each point of the killing player's Luck adds to the level-based affix conversion chance");

        championGemChance = config.getFloat("championGemChance", category, 0.25f, 0, 1,
                "Base chance for a champion to also drop a gem, on top of its guaranteed affix item");
        championGemLuckFactor = config.getFloat("championGemLuckFactor", category, 0.02f, 0, 1,
                "How much each point of the killing player's Luck adds to the champion gem drop chance");

        gemBaseChance = config.getFloat("gemBaseChance", category, 0.02f, 0, 1,
                "Base chance for a gem to drop");
        gemLevelChanceIncrease = config.getFloat("gemLevelChanceIncrease", category, 0.005f, 0, 1,
                "How much the gem drop chance increases per level");
        gemMaxChance = config.getFloat("gemMaxChance", category, 0.15f, 0, 1,
                "Maximum chance for a gem to drop");

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
                "Level threshold to rarity mapping for affixes. Format: 'level=minRarity-maxRarity'\n" +
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

        championRankRarityMappings = config.getStringList("championRankMappings", category,
                new String[] {
                        "6=mythic-ancient",
                        "5=epic-mythic",
                        "4=rare-epic",
                        "3=uncommon-rare",
                        "2=common-uncommon",
                        "1=common-common"
                },
                "Champion rank (tier) to rarity mapping for the bonus affix item dropped by champions.\n" +
                        "Format: 'tier=minRarity-maxRarity'.\n" +
                        RARITY_NAMES);

        levelPurityMappings = config.getStringList("levelPurityMappings", category,
                new String[] {
                        "25=flawless-perfect",
                        "20=normal-flawless",
                        "15=flawed-normal",
                        "10=chipped-flawed",
                        "5=cracked-chipped",
                        "1=cracked-cracked"
                },
                "Level threshold to gem purity mapping. Format: 'level=minPurity-maxPurity'\n" +
                        "Each entry applies to levels from its value up to (but not including) the next threshold.\n" +
                        PURITY_NAMES);

        championPurityMappings = config.getStringList("championPurityMappings", category,
                new String[] {
                        "5=flawless-perfect",
                        "4=normal-flawless",
                        "3=flawed-normal",
                        "2=chipped-flawed",
                        "1=cracked-chipped"
                },
                "Champion rank (tier) to gem purity mapping for the bonus gem dropped by champions.\n" +
                        "Format: 'tier=minPurity-maxPurity'.\n" +
                        PURITY_NAMES);

        weightTier = config.getString("weightTier", category, "player", """
                Rarity and purity weights come from Apotheosis world tiers.
                'player' uses the player's tier; name a tier (haven, frontier, ascent, summit, pinnacle),
                such as pinnacle, so mob level alone decides.""");

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
