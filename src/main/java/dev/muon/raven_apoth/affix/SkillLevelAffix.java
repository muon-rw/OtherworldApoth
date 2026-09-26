package dev.muon.raven_apoth.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.irons_apothic.affix.SpellLevelAffix;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.placebo.util.StepFunction;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A {@link SpellLevelAffix} for non-magic schools, whose spells are called skills and whose power attributes
 * are not {@code *_spell_power}, so Irons Apothic's gear school matching would never let it roll.
 */
public class SkillLevelAffix extends SpellLevelAffix {
    public static final Codec<SkillLevelAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            Affix.affixDef(),
            ResourceLocation.CODEC.fieldOf("school").forGetter(a -> a.schoolIds.orElseThrow().getFirst()),
            LootRarity.mapCodec(StepFunction.CODEC.fieldOf("level").codec())
                .fieldOf("values")
                .forGetter(a -> a.values.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().level()))),
            LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.validTypes))
        .apply(inst, SkillLevelAffix::new));

    public SkillLevelAffix(AffixDefinition definition, ResourceLocation school, Map<LootRarity, StepFunction> levels, Set<LootCategory> types) {
        super(definition, Optional.of(List.of(school)), levels.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> new LevelData(e.getValue()))), types);
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public MutableComponent getDescription(AffixInstance inst, AttributeTooltipContext ctx) {
        LevelData data = this.values.get(inst.getRarity());
        return data == null ? Component.empty() : describe(data.level().getInt(inst.level()));
    }

    @Override
    public Component getAugmentingText(AffixInstance inst, AttributeTooltipContext ctx) {
        LevelData data = this.values.get(inst.getRarity());
        if (data == null) {
            return Component.empty();
        }
        MutableComponent comp = describe(data.level().getInt(inst.level()));
        int min = data.level().getInt(0);
        int max = data.level().getInt(1);
        if (min != max) {
            comp.append(Affix.valueBounds(Component.literal(String.valueOf(min)), Component.literal(String.valueOf(max))));
        }
        return comp;
    }

    private MutableComponent describe(int bonus) {
        String key = bonus == 1 ? "affix.raven_apoth.skill_level.desc.singular" : "affix.raven_apoth.skill_level.desc";
        return Component.translatable(key, this.schoolName(), bonus);
    }

    private Component schoolName() {
        return this.schools.flatMap(s -> s.stream().findFirst()).map(SchoolType::getDisplayName).orElse(Component.empty());
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        return !cat.isNone() && this.values.containsKey(rarity) && this.validTypes.contains(cat)
            && this.schools.map(s -> !s.isEmpty()).orElse(false);
    }
}
