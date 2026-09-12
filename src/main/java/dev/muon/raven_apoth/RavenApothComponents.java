package dev.muon.raven_apoth;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RavenApothComponents {
    public static final DeferredRegister.DataComponents REGISTER =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, RavenApoth.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> PLAYER_DROPPED =
            REGISTER.registerComponentType("player_dropped", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    private RavenApothComponents() {}

    public static void register(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
