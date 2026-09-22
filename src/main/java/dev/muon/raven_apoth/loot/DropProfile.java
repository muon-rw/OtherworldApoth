package dev.muon.raven_apoth.loot;

import dev.muon.dynamic_difficulty.api.LevelingAPI;
import dev.muon.raven_apoth.config.RavenApothConfig;
import dev.muon.raven_apoth.tiers.SpawnOrigin;
import dev.muon.raven_apoth.tiers.SpawnTier;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.common.Tags;

import java.util.List;
import java.util.Set;

/**
 * Everything that decides a mob's Apotheosis drops.
 *
 * ONLY Mob level (plus a bonus per champion rank) picks the rarity range (and gem purity).
 * The mob's spawn tier, its champion rank and player luck only move chances/odds within that range.
 */
public record DropProfile(int level, int championRank, WorldTier tier, List<LootRarity> rarities, Set<Purity> purities,
                          float affixChance, float gemChance) {

    /**
     * Bosses always roll, since many are summoned by altars and never see a spawn event. Everything else has to be
     * hostile and to have spawned on its own, so dummies, golems, summons, spawn eggs and livestock drop nothing.
     */
    public static boolean isEligible(LivingEntity mob) {
        if (mob.getType().is(Tags.EntityTypes.BOSSES)) {
            return true;
        }
        return mob instanceof Enemy && LevelingAPI.hasLevel(mob) && SpawnTier.originOf(mob)
                .filter(origin -> origin == SpawnOrigin.WILD || RavenApothConfig.spawnerChanceFactor > 0)
                .isPresent();
    }

    public static DropProfile of(LivingEntity mob, float luck) {
        int level = LevelingAPI.hasLevel(mob) ? LevelingAPI.getLevel(mob) : 1;
        int rank = ChampionRanks.tierOf(mob).orElse(0);
        WorldTier tier = SpawnTier.of(mob);
        List<LootRarity> rarities = LootUtils.mobRarityBracket(level + rank * RavenApothConfig.championLevelBonus);

        double affix = Math.min(RavenApothConfig.affixBaseChance + level * RavenApothConfig.affixLevelChanceIncrease, RavenApothConfig.affixMaxChance)
                + tier.ordinal() * RavenApothConfig.tierAffixChanceBonus
                + luck * RavenApothConfig.affixLuckFactor;
        double gem = Math.min(RavenApothConfig.gemBaseChance + level * RavenApothConfig.gemLevelChanceIncrease, RavenApothConfig.gemMaxChance)
                + tier.ordinal() * RavenApothConfig.tierGemChanceBonus
                + luck * RavenApothConfig.gemLuckFactor;
        if (rank > 0) {
            gem += RavenApothConfig.championGemChance + rank * RavenApothConfig.championGemRankBonus;
        }
        if (SpawnTier.originOf(mob).orElse(null) == SpawnOrigin.SPAWNER) {
            affix *= RavenApothConfig.spawnerChanceFactor;
            gem *= RavenApothConfig.spawnerChanceFactor;
        }
        if (mob.getType().is(Tags.EntityTypes.BOSSES)) {
            affix = Math.max(affix, RavenApothConfig.bossAffixChance);
            gem = Math.max(gem, RavenApothConfig.bossGemChance);
        }
        return new DropProfile(level, rank, tier, rarities, LootUtils.purityBracket(rarities), clampChance(affix), clampChance(gem));
    }

    public GenContext context(GenContext killerContext) {
        return LootUtils.withTier(killerContext, this.tier);
    }

    public LootRarity rollRarity(GenContext killerContext) {
        return LootUtils.rollRarity(this.rarities, this.context(killerContext));
    }

    public Purity rollPurity(GenContext killerContext) {
        return LootUtils.rollPurity(this.purities, this.context(killerContext));
    }

    private static float clampChance(double chance) {
        return (float) Math.clamp(chance, 0, 1);
    }
}
