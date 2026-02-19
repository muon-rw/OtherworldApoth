package dev.muon.otherworldapoth.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public class TransmutationAffix extends Affix {
    public static final Codec<TransmutationAffix> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    ResourceLocation.CODEC.fieldOf("school").forGetter(a -> a.school.getId()),
                    LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.types)
            ).apply(inst, TransmutationAffix::new));

    protected final SchoolType school;
    protected final Set<LootCategory> types;
    protected final TagKey<DamageType> damageTypeTag;

    public TransmutationAffix(ResourceLocation schoolId, Set<LootCategory> types) {
        super(AffixType.ABILITY);
        this.school = SchoolRegistry.getSchool(schoolId);
        if (this.school == null) {
            throw new IllegalArgumentException("Invalid school ID provided for TransmutationAffix: " + schoolId);
        }
        this.types = types;
        // Construct damage type tag: {namespace}:{school_id}_magic
        this.damageTypeTag = TagKey.create(Registries.DAMAGE_TYPE, 
                ResourceLocation.fromNamespaceAndPath(schoolId.getNamespace(), schoolId.getPath() + "_magic"));
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public MutableComponent getDescription(ItemStack stack, LootRarity rarity, float level) {
        return Component.translatable("affix.otherworldapoth:transmutation.desc",
                this.school.getDisplayName().copy().withStyle(this.school.getDisplayName().getStyle()));
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        if (cat == null || cat.isNone()) {
            return false;
        }
        return this.types.isEmpty() || this.types.contains(cat);
    }

    public TagKey<DamageType> getDamageTypeTag() {
        return this.damageTypeTag;
    }

    public SchoolType getSchool() {
        return this.school;
    }
}
