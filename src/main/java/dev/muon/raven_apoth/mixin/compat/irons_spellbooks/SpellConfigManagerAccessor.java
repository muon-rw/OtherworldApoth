package dev.muon.raven_apoth.mixin.compat.irons_spellbooks;

import com.google.common.collect.ImmutableMap;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = SpellConfigManager.class, remap = false)
public interface SpellConfigManagerAccessor {

    @Accessor("config")
    ImmutableMap<?, ?> raven_apoth$getConfig();

    @Accessor("datapackOverride")
    Map<?, ?> raven_apoth$getDatapackOverride();
}
