package dev.muon.raven_apoth.loot;

import com.mojang.serialization.MapCodec;
import dev.muon.raven_apoth.RavenApoth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class RavenApothLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, RavenApoth.MODID);

    static {
        REGISTER.register("leveled_affixes", () -> LeveledAffixLootModifier.CODEC);
        REGISTER.register("leveled_gems", () -> LeveledGemLootModifier.CODEC);
        REGISTER.register("champion_gem", () -> ChampionGemLootModifier.CODEC);
    }

    private RavenApothLootModifiers() {}

    public static void register(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
