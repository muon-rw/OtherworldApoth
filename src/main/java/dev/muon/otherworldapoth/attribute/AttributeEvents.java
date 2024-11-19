package dev.muon.otherworldapoth.attribute;

import dev.muon.otherworldapoth.OtherworldApoth;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AttributeEvents {

    @SubscribeEvent
    public void onSpellCast(SpellOnCastEvent event) {
        SchoolType school = event.getSchoolType();
        AttributeInstance levelAttr = event.getEntity().getAttribute(AttributeRegistry.getSchoolLevelAttribute(school));

        if (levelAttr != null && levelAttr.getValue() > 0) {
            int baseLevel = event.getSpellLevel();
            int bonusLevel = (int) levelAttr.getValue();
            event.setSpellLevel(baseLevel + bonusLevel);
            OtherworldApoth.LOGGER.debug("Changed spell level from {} to {} for {}", baseLevel, (baseLevel + bonusLevel), event.getEntity().getName());
        }
    }
}