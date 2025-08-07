package dev.muon.otherworldapoth.util;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SchoolUtil {

    public static Set<SchoolType> getSpellSchoolsFromGear(ItemStack stack) {
        Set<SchoolType> schools = new HashSet<>();
        EquipmentSlot slot = LivingEntity.getEquipmentSlotForItem(stack);
        Map<Attribute, Collection<AttributeModifier>> attributes = stack.getItem().getAttributeModifiers(slot, stack).asMap();

        for (Attribute attribute : attributes.keySet()) {
            ResourceLocation attrId = ForgeRegistries.ATTRIBUTES.getKey(attribute);
            if (attrId != null && attrId.getPath().endsWith("_spell_power")) {
                String schoolName = attrId.getPath().replace("_spell_power", "");
                SchoolType school = SchoolRegistry.getSchool(new ResourceLocation(attrId.getNamespace(),  schoolName));
                if (school != null) {
                    schools.add(school);
                }
            }
        }
        return schools;
    }
}