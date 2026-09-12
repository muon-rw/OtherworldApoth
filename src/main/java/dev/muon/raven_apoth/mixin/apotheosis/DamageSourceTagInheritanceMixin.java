package dev.muon.raven_apoth.mixin.apotheosis;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

/**
 * Apotheosis' DamageSourceMixin only matches its extra tags exactly. This makes a source carrying
 * irons_spellbooks:fire_magic also match parent tags such as c:is_magic.
 */
@Mixin(value = DamageSource.class, priority = 1100)
public class DamageSourceTagInheritanceMixin {

    @Dynamic("Added by dev.shadowsoffire.apotheosis.mixin.DamageSourceMixin")
    @Shadow(remap = false)
    @Nullable
    private Set<TagKey<DamageType>> extraTypes;

    @ModifyReturnValue(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("RETURN"))
    private boolean raven_apoth$matchParentTags(boolean original, TagKey<DamageType> tag) {
        if (original || this.extraTypes == null || this.extraTypes.isEmpty()) return original;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return false;
        Optional<Registry<DamageType>> registry = server.registryAccess().registry(Registries.DAMAGE_TYPE);
        if (registry.isEmpty()) return false;

        for (TagKey<DamageType> extraTag : this.extraTypes) {
            Optional<HolderSet.Named<DamageType>> members = registry.get().getTag(extraTag);
            if (members.isEmpty()) continue;
            for (Holder<DamageType> holder : members.get()) {
                if (holder.is(tag)) return true;
            }
        }
        return false;
    }
}
