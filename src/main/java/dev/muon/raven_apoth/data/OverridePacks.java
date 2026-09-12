package dev.muon.raven_apoth.data;

import dev.muon.raven_apoth.RavenApoth;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * Ships a built-in datapack that disables Apotheosis' own affix and gem loot injection, invaders
 * and elites, since raven_apoth replaces them with its leveled system. Needs
 * {@link Pack.Position#TOP} so it wins against Apotheosis' own mod resources.
 */
public final class OverridePacks {
    private OverridePacks() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(OverridePacks::addApotheosisOverridePack);
    }

    private static void addApotheosisOverridePack(AddPackFindersEvent event) {
        event.addPackFinders(
                RavenApoth.loc("apotheosis_overrides"),
                PackType.SERVER_DATA,
                Component.literal("Raven Apoth Apotheosis Overrides"),
                PackSource.BUILT_IN,
                true,
                Pack.Position.TOP
        );
    }
}
