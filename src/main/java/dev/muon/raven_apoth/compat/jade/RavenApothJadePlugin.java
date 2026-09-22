package dev.muon.raven_apoth.compat.jade;

import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class RavenApothJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(MobLootProvider.INSTANCE, LivingEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(MobLootProvider.INSTANCE, LivingEntity.class);
    }
}
