package dev.muon.otherworldapoth.datapacks;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

import java.util.function.Consumer;

public class AAscendedReplacerSource implements RepositorySource {
    private final PackType packType;

    public AAscendedReplacerSource(PackType packType) {
        this.packType = packType;
    }

    @Override
    public void loadPacks(Consumer<Pack> packAdder) {
        Pack pack = Pack.readMetaAndCreate(
                "aascended_replacer",
                Component.literal("OW Apoth Ascended Replacer"),
                true,
                packId -> new IronsReplacerPackResources(),
                this.packType,
                Pack.Position.TOP,
                PackSource.BUILT_IN
        );

        if (pack != null) {
            packAdder.accept(pack);
        }
    }
}