package dev.muon.raven_apoth.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;

import java.util.Set;

public class SpellAttunementAffix extends Affix {
    public static final Codec<SpellAttunementAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            Affix.affixDef(),
            RarityRegistry.INSTANCE.holderCodec().fieldOf("min_rarity").forGetter(a -> a.minRarity),
            LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.types))
        .apply(inst, SpellAttunementAffix::new));

    protected final DynamicHolder<LootRarity> minRarity;
    protected final Set<LootCategory> types;

    public SpellAttunementAffix(AffixDefinition definition, DynamicHolder<LootRarity> minRarity, Set<LootCategory> types) {
        super(definition);
        this.minRarity = minRarity;
        this.types = types;
    }

    public static boolean isAttuned(ItemStack stack) {
        return AffixHelper.streamAffixes(stack).anyMatch(inst -> inst.getAffix() instanceof SpellAttunementAffix);
    }

    private static boolean hasImbuedSpells(ItemStack stack) {
        return !(stack.getItem() instanceof SpellBook)
            && ISpellContainer.isSpellContainer(stack)
            && !ISpellContainer.get(stack).isEmpty();
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public MutableComponent getDescription(AffixInstance inst, AttributeTooltipContext ctx) {
        return Component.translatable("affix.raven_apoth:spell_attunement.desc");
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        if (cat == null || cat.isNone() || !this.minRarity.isBound() || !hasImbuedSpells(stack)) {
            return false;
        }
        return rarity.sortIndex() >= this.minRarity.get().sortIndex() && (this.types.isEmpty() || this.types.contains(cat));
    }
}
