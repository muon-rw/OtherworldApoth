package dev.muon.raven_apoth;

import dev.muon.raven_apoth.tiers.SpawnOrigin;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class RavenApothAttachments {
    public static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, RavenApoth.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<WorldTier>> SPAWN_TIER =
            REGISTER.register("spawn_tier", () -> AttachmentType.builder(() -> WorldTier.HAVEN).serialize(WorldTier.CODEC).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SpawnOrigin>> SPAWN_ORIGIN =
            REGISTER.register("spawn_origin", () -> AttachmentType.builder(() -> SpawnOrigin.WILD).serialize(SpawnOrigin.CODEC).build());

    private RavenApothAttachments() {}

    public static void register(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
