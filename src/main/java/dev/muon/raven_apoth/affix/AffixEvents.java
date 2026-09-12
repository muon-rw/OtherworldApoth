package dev.muon.raven_apoth.affix;

import dev.shadowsoffire.apotheosis.event.GetItemSocketsEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public class AffixEvents {

    @SubscribeEvent
    public void onGetSockets(GetItemSocketsEvent e) {
        SocketBonusAffix.addBonusSockets(e);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onIncomingDamage(LivingIncomingDamageEvent e) {
        TransmutationAffix.tagDamageSource(e);
    }
}
