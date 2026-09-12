package dev.muon.raven_apoth.loot;

import net.minecraft.world.entity.LivingEntity;
import top.theillusivec4.champions.api.ChampionsApi;

import java.util.OptionalInt;

public final class ChampionRanks {
    private ChampionRanks() {}

    public static OptionalInt tierOf(LivingEntity entity) {
        return ChampionsApi.get().getChampion(entity)
                .map(champion -> OptionalInt.of(champion.tier().level()))
                .orElse(OptionalInt.empty());
    }
}
