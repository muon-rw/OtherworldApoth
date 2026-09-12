package dev.muon.raven_apoth.mixin.compat.irons_spellbooks;

import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SchoolType.class, remap = false)
public interface SchoolTypeAccessor {

    @Accessor("powerAttribute")
    Holder<Attribute> raven_apoth$getPowerAttribute();

    @Accessor("resistanceAttribute")
    Holder<Attribute> raven_apoth$getResistanceAttribute();
}
