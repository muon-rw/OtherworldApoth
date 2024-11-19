package dev.muon.otherworldapoth.attribute;

import dev.muon.otherworldapoth.OtherworldApoth;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegisterEvent;

import java.util.HashMap;
import java.util.Map;

public class AttributeRegistry {
    private static final Map<SchoolType, Attribute> SCHOOL_LEVEL_ATTRIBUTES = new HashMap<>();

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(AttributeRegistry::registerAttributes);
    }


    // From Additional Attributes mod, by Cadentem/SiverDX
    @SuppressWarnings("UnstableApiUsage")
    private static void registerAttributes(RegisterEvent event) {
        if (event.getRegistryKey() == SchoolRegistry.REGISTRY.get().getRegistryKey()) {
            if (ForgeRegistries.ATTRIBUTES instanceof ForgeRegistry<Attribute> registry) {
                registry.unfreeze();

                SchoolRegistry.REGISTRY.get().getValues().forEach(school -> {
                    String schoolPath = school.getId().getPath();
                    Attribute attribute = new RangedAttribute(
                            "attribute.otherworldapoth." + schoolPath + "_spell_level",
                            0.0, 0.0, 5.0);

                    registry.register(OtherworldApoth.loc(schoolPath + "_spell_level"), attribute);
                    SCHOOL_LEVEL_ATTRIBUTES.put(school, attribute);
                });

                registry.freeze();
            }
        }
    }

    public static Attribute getSchoolLevelAttribute(SchoolType school) {
        return SCHOOL_LEVEL_ATTRIBUTES.get(school);
    }
}