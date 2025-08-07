package dev.muon.otherworldapoth.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldapoth.LootCategories;
import dev.muon.otherworldapoth.util.SchoolUtil;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.placebo.util.StepFunction;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Set;

public class ManaCostAffix extends Affix {
    public static final Codec<ManaCostAffix> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    ResourceLocation.CODEC.fieldOf("school").forGetter(a -> a.school.getId()),
                    LootRarity.mapCodec(StepFunction.CODEC).fieldOf("values").forGetter(a -> a.values),
                    LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.types)
            ).apply(inst, ManaCostAffix::new));

    protected final SchoolType school;
    protected final Map<LootRarity, StepFunction> values;
    protected final Set<LootCategory> types;

    public ManaCostAffix(ResourceLocation schoolId, Map<LootRarity, StepFunction> values, Set<LootCategory> types) {
        super(AffixType.ABILITY);
        this.school = SchoolRegistry.getSchool(schoolId);
        if (this.school == null) {
            throw new IllegalArgumentException("Invalid school ID provided for ManaCostAffix: " + schoolId);
        }
        this.values = values;
        this.types = types;
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        if (cat != LootCategories.STAFF && cat != LootCategory.SWORD && cat != LootCategory.HEAVY_WEAPON) {
            return false;
        }

        if (!this.values.containsKey(rarity)) {
            return false;
        }

        if (!this.types.isEmpty() && !this.types.contains(cat)) {
            return false;
        }

        // Check if the gear has the matching school
        Set<SchoolType> gearSchools = SchoolUtil.getSpellSchoolsFromGear(stack);
        return gearSchools.contains(this.school);
    }

    @Override
    public MutableComponent getDescription(ItemStack stack, LootRarity rarity, float level) {
        float reduction = this.getReductionPercent(rarity, level);

        String schoolTranslationKey = "school." + school.getId().getNamespace() + "." + school.getId().getPath();

        return Component.translatable("affix.otherworldapoth.mana_cost.desc",
                Component.translatable(schoolTranslationKey).withStyle(school.getDisplayName().getStyle()),
                fmt(reduction * 100));
    }

    @Override
    public Component getAugmentingText(ItemStack stack, LootRarity rarity, float level) {
        MutableComponent comp = this.getDescription(stack, rarity, level);

        float minReduction = this.getReductionPercent(rarity, 0);
        float maxReduction = this.getReductionPercent(rarity, 1);

        Component minComp = Component.translatable("%s%%", fmt(minReduction * 100));
        Component maxComp = Component.translatable("%s%%", fmt(maxReduction * 100));

        return comp.append(valueBounds(minComp, maxComp));
    }

    public SchoolType getSchool() {
        return school;
    }

    public float getReductionPercent(LootRarity rarity, float level) {
        StepFunction func = this.values.get(rarity);
        return func != null ? func.get(level) : 0f;
    }
}