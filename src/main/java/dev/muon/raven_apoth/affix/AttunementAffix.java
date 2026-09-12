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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;

import java.util.Set;

public class AttunementAffix extends Affix {
    public static final Codec<AttunementAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            Affix.affixDef(),
            RarityRegistry.INSTANCE.holderCodec().fieldOf("min_rarity").forGetter(a -> a.minRarity),
            LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.types))
        .apply(inst, AttunementAffix::new));

    protected final DynamicHolder<LootRarity> minRarity;
    protected final Set<LootCategory> types;

    public AttunementAffix(AffixDefinition definition, DynamicHolder<LootRarity> minRarity, Set<LootCategory> types) {
        super(definition);
        this.minRarity = minRarity;
        this.types = types;
    }

    public static boolean isAttuned(ItemStack stack) {
        return AffixHelper.streamAffixes(stack).anyMatch(inst -> inst.getAffix() instanceof AttunementAffix);
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public MutableComponent getDescription(AffixInstance inst, AttributeTooltipContext ctx) {
        return Component.translatable("affix.raven_apoth:attunement.desc");
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        if (cat == null || cat.isNone() || !this.minRarity.isBound()) {
            return false;
        }
        return rarity.sortIndex() >= this.minRarity.get().sortIndex() && (this.types.isEmpty() || this.types.contains(cat));
    }
}
