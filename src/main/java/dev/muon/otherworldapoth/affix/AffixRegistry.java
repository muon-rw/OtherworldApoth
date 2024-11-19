package dev.muon.otherworldapoth.affix;

import dev.muon.otherworldapoth.OtherworldApoth;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.placebo.reload.DynamicRegistry;
import net.minecraft.resources.ResourceLocation;

public class AffixRegistry extends DynamicRegistry<Affix> {
    public static final AffixRegistry INSTANCE = new AffixRegistry();

    private AffixRegistry() {
        super(OtherworldApoth.LOGGER, "otherworld_affixes", true, true);
    }

    @Override
    protected void registerBuiltinCodecs() {
        this.registerCodec(OtherworldApoth.loc("spell_effect"), SpellEffectAffix.CODEC);
        this.registerCodec(OtherworldApoth.loc("magic_telepathic"), MagicTelepathicAffix.CODEC);
    }

    @Override
    protected void validateItem(ResourceLocation key, Affix value) {
        super.validateItem(key, value);
    }

    public static void init() {
        INSTANCE.registerToBus();
    }
}