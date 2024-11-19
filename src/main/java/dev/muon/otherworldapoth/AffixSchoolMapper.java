package dev.muon.otherworldapoth;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AffixSchoolMapper {

    public static Set<SchoolType> getSpellSchoolsFromGear(ItemStack stack, EquipmentSlot slot) {
        Set<SchoolType> schools = new HashSet<>();
        Map<Attribute, Collection<AttributeModifier>> attributes = stack.getAttributeModifiers(slot).asMap();

        // VERY CURSED DO NOT LOOK
        for (Attribute attribute : attributes.keySet()) {
            ResourceLocation attrId = ForgeRegistries.ATTRIBUTES.getKey(attribute);
            if (attrId != null && attrId.getPath().endsWith("_spell_power")) {
                String schoolName = attrId.getPath().replace("_spell_power", "");
                SchoolType school = SchoolRegistry.getSchool(new ResourceLocation("irons_spellbooks", schoolName));
                if (school != null) {
                    schools.add(school);
                }
            }
        }
        return schools;
    }

    public static Set<SchoolType> getSpellSchoolsFromWeapon(ItemStack stack) {
        return getSpellSchoolsFromGear(stack, EquipmentSlot.MAINHAND);
    }

    public static SchoolType getSpellSchoolForAffix(String affixId) {
        ResourceLocation affixResource = new ResourceLocation(affixId);
        String path = affixResource.getPath();

        if (path.contains("elemental/")) {
            String[] pathParts = path.split("/");
            for (String part : pathParts) {
                if (part.startsWith("school_")) {
                    String schoolName = part.substring("school_".length());
                    // returns null if no SchoolType match
                    return SchoolRegistry.getSchool(new ResourceLocation("irons_spellbooks", schoolName));
                }
            }
        }
        return null;
    }

    public static boolean isGenericSpellAffix(String affixId) {
        ResourceLocation affixResource = new ResourceLocation(affixId);
        String path = affixResource.getPath();
        return path.contains("elemental/school_none/");
    }

    public static boolean isElementalAffix(String affixId) {
        ResourceLocation affixResource = new ResourceLocation(affixId);
        String path = affixResource.getPath();
        return path.contains("elemental/");
    }
}