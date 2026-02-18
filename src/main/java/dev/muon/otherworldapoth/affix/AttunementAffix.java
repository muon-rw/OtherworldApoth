package dev.muon.otherworldapoth.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public class AttunementAffix extends Affix {
    public static final Codec<AttunementAffix> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    LootRarity.CODEC.fieldOf("min_rarity").forGetter(a -> a.minRarity),
                    LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.types)
            ).apply(inst, AttunementAffix::new));

    protected final LootRarity minRarity;
    protected final Set<LootCategory> types;

    public AttunementAffix(LootRarity minRarity, Set<LootCategory> types) {
        super(AffixType.POTION);
        this.minRarity = minRarity;
        this.types = types;
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public MutableComponent getDescription(ItemStack stack, LootRarity rarity, float level) {
        return Component.translatable("affix.otherworldapoth:attunement.desc");
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        if (cat == null || cat.isNone()) {
            return false;
        }
        return rarity.isAtLeast(this.minRarity) && (this.types.isEmpty() || this.types.contains(cat));
    }
}
