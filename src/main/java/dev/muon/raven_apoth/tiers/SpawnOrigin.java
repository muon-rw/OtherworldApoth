package dev.muon.raven_apoth.tiers;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.MobSpawnType;
import org.jetbrains.annotations.Nullable;

/**
 * Used to exclude mobs with no recorded origin (spawn eggs, summons, commands,
 * breeding, player-built golems, placed dummies) from Apotheosis loot
 */
public enum SpawnOrigin implements StringRepresentable {
    WILD("wild"),
    SPAWNER("spawner");

    public static final Codec<SpawnOrigin> CODEC = StringRepresentable.fromEnum(SpawnOrigin::values);

    private final String name;

    SpawnOrigin(String name) {
        this.name = name;
    }

    @Nullable
    public static SpawnOrigin of(MobSpawnType type) {
        return switch (type) {
            case NATURAL, CHUNK_GENERATION, STRUCTURE, PATROL, EVENT, JOCKEY, REINFORCEMENT, TRIGGERED, TRIAL_SPAWNER -> WILD;
            case SPAWNER -> SPAWNER;
            default -> null;
        };
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
