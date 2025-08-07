package dev.muon.otherworldapoth.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldapoth.OtherworldApoth;
import dev.muon.otherworldapoth.util.SchoolUtil;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AttributeAffix;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.bonus.GemBonus;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import dev.shadowsoffire.placebo.util.StepFunction;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SchoolAttributeAffix extends AttributeAffix {

    public static final Codec<SchoolAttributeAffix> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                    ForgeRegistries.ATTRIBUTES.getCodec().fieldOf("attribute").forGetter(a -> a.attribute),
                    PlaceboCodecs.enumCodec(Operation.class).fieldOf("operation").forGetter(a -> a.operation),
                    GemBonus.VALUES_CODEC.fieldOf("values").forGetter(a -> a.values),
                    LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.types),
                    ResourceLocation.CODEC.optionalFieldOf("school").forGetter(a -> a.schoolId)
            ).apply(inst, SchoolAttributeAffix::new));

    protected final Optional<ResourceLocation> schoolId;
    protected final Optional<SchoolType> school;

    public SchoolAttributeAffix(Attribute attr, Operation op, Map<LootRarity, StepFunction> values, Set<LootCategory> categories, Optional<ResourceLocation> schoolId) {
        super(attr, op, values, categories);
        this.schoolId = schoolId;
        this.school = schoolId.flatMap(id -> {
            SchoolType s = SchoolRegistry.getSchool(id);
            if (s == null) {
                OtherworldApoth.LOGGER.warn("Unknown school ID {} provided for SchoolAttributeAffix, affix may not apply correctly until school is registered.", id);
                return Optional.empty();
            }
            return Optional.of(s);
        });
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        if (!super.canApplyTo(stack, cat, rarity)) {
            return false;
        }
        if (cat == null || cat.isNone()) {
            return false;
        }

        Set<SchoolType> gearSchools = SchoolUtil.getSpellSchoolsFromGear(stack);

        if (this.school.isPresent()) {
            SchoolType requiredSchool = this.school.get();
            return gearSchools.contains(requiredSchool);
        } else {
            return gearSchools.isEmpty();
        }
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }
}