package dev.muon.otherworldapoth.mixin;

import dev.muon.otherworldapoth.OtherworldApoth;
import dev.muon.otherworldapoth.affix.MagicTelepathicAffix;
import dev.muon.otherworldapoth.affix.SpellEffectAffix;
import dev.muon.otherworldapoth.affix.SpellLevelAffix;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry;
import dev.shadowsoffire.placebo.reload.DynamicRegistry;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AffixRegistry.class, remap = false)
public abstract class AffixRegistryMixin extends DynamicRegistry<Affix> {

    public AffixRegistryMixin(Logger logger, String path, boolean synced, boolean subtypes) {
        super(OtherworldApoth.LOGGER, "otherworld_affixes", true, true);
    }
    // This is *much* easier :)
    @Inject(method = "registerBuiltinCodecs", at = @At("TAIL"))
    protected void registerOtherworldAffixes(CallbackInfo info) {
        OtherworldApoth.LOGGER.info("Registering Otherworld Affix Codecs to Apotheosis Registry");
        this.registerCodec(OtherworldApoth.loc("spell_effect"), SpellEffectAffix.CODEC);
        this.registerCodec(OtherworldApoth.loc("magic_telepathic"), MagicTelepathicAffix.CODEC);
        this.registerCodec(OtherworldApoth.loc("spell_level"), SpellLevelAffix.CODEC);
    }
}