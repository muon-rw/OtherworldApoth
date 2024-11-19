package dev.muon.otherworldapoth.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Set;

public class SpellLevelAffix extends Affix {
    public static final Codec<SpellLevelAffix> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    ResourceLocation.CODEC.fieldOf("school").forGetter(a -> a.school.getId()),
                    Codec.unboundedMap(LootRarity.CODEC, Codec.INT).fieldOf("values").forGetter(a -> a.values),
                    LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.validTypes)
            ).apply(inst, SpellLevelAffix::new));

    protected final SchoolType school;
    protected final Map<LootRarity, Integer> values;
    protected final Set<LootCategory> validTypes;

    public SpellLevelAffix(ResourceLocation schoolId, Map<LootRarity, Integer> values, Set<LootCategory> types) {
        super(AffixType.ABILITY);
        this.school = SchoolRegistry.REGISTRY.get().getValue(schoolId);
        this.values = values;
        this.validTypes = types;
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public MutableComponent getDescription(ItemStack stack, LootRarity rarity, float level) {
        return Component.translatable("affix.otherworldapoth.spell_level.desc",
                Component.translatable("school.irons_spellbooks." + school.getId().getPath()),
                this.values.get(rarity));
    }

    @Override
    public Component getAugmentingText(ItemStack stack, LootRarity rarity, float level) {
        return this.getDescription(stack, rarity, level);
    }

    public SchoolType getSchool() {
        return school;
    }

    public int getBonusLevel(LootRarity rarity, float level) {
        return this.values.get(rarity);
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        if (cat.isNone()) return false;
        return (this.validTypes.isEmpty() || this.validTypes.contains(cat))
                && this.values.containsKey(rarity);
    }
}