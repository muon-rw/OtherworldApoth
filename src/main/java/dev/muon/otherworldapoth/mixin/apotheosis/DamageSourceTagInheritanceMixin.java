package dev.muon.otherworldapoth.mixin.apotheosis;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Set;

/**
 * Enhances Apotheosis' DamageSourceExtension so that source.is(tag) returns true when any of the
 * extra tags (e.g. irons_spellbooks:fire_magic) are contained in the queried tag (e.g. forge:is_magic).
 * Apotheosis only checks exact match; this adds tag inheritance.
 */
@Mixin(value = DamageSource.class, remap = true, priority = 1100)
public class DamageSourceTagInheritanceMixin {

    @Unique
    private static Field extraTypesField;

    @ModifyReturnValue(method = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z", at = @At("RETURN"), require = 1)
    private boolean otherworldapoth$checkTagInheritance(boolean original, TagKey<DamageType> tag) {
        if (original) return true;

        Set<TagKey<DamageType>> extraTypes = getExtraTypes((DamageSource) (Object) this);
        if (extraTypes == null || extraTypes.isEmpty()) return original;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return original;

        var registry = server.registryAccess().registry(Registries.DAMAGE_TYPE);
        if (registry.isEmpty()) return original;

        for (TagKey<DamageType> extraTag : extraTypes) {
            if (extraTag.equals(tag)) return true;
            var optHolderSet = registry.get().getTag(extraTag);
            if (optHolderSet.isPresent()) {
                for (Holder<DamageType> holder : optHolderSet.get()) {
                    if (holder.is(tag)) return true;
                }
            }
        }
        return original;
    }

    @Unique
    @Nullable
    @SuppressWarnings("unchecked")
    private static Set<TagKey<DamageType>> getExtraTypes(DamageSource source) {
        Field field = findExtraTypesField(source);
        if (field == null) return null;
        try {
            Object val = field.get(source);
            if (val instanceof Set<?> set && !set.isEmpty()) {
                Object first = set.iterator().next();
                if (first instanceof TagKey<?> tk && tk.isFor(Registries.DAMAGE_TYPE)) {
                    return (Set<TagKey<DamageType>>) set;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Unique
    private static Field findExtraTypesField(DamageSource source) {
        if (extraTypesField != null) return extraTypesField;
        for (Field field : source.getClass().getDeclaredFields()) {
            if (Set.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                extraTypesField = field;
                return field;
            }
        }
        return null;
    }
}
