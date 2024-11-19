package dev.muon.otherworldapoth.attribute;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class AttributeEvents {
    @SubscribeEvent
    public static void onSpellCast(SpellOnCastEvent event) {
        SchoolType school = event.getSchoolType();
        AttributeInstance levelAttr = event.getEntity().getAttribute(AttributeRegistry.getSchoolLevelAttribute(school));

        if (levelAttr != null) {
            int baseLevel = event.getSpellLevel();
            int bonusLevel = (int) levelAttr.getValue();
            event.setSpellLevel(baseLevel + bonusLevel);
        }
    }
}