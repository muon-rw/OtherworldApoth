package dev.muon.otherworldapoth.replacer;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

import java.util.function.Consumer;

public class OWApothSource implements RepositorySource {
    private final PackType packType;

    public OWApothSource(PackType packType) {
        this.packType = packType;
    }

    @Override
    public void loadPacks(Consumer<Pack> packAdder) {
        Pack pack = Pack.readMetaAndCreate(
                "apotheosis_replacer",
                Component.literal("Apotheosis Replacer"),
                true,
                packId -> new OWApothPackResources(),
                this.packType,
                Pack.Position.TOP,
                PackSource.BUILT_IN
        );

        if (pack != null) {
            packAdder.accept(pack);
        }
    }
}