package dev.muon.raven_apoth.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.raven_apoth.config.RavenApothConfig;
import dev.muon.raven_apoth.tiers.WorldTierLevels;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.client.WorldTierSelectScreen;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(value = WorldTierSelectScreen.class, remap = false)
public class WorldTierSelectScreenMixin {

    @ModifyReturnValue(method = "tierLocked", at = @At("RETURN"))
    private static List<Component> raven_apoth$describeLevelRequirement(List<Component> original, WorldTier tier) {
        if (!RavenApothConfig.gateWorldTiersByLevel) {
            return original;
        }
        Component tierName = Apotheosis.lang("text", "world_tier." + tier.getSerializedName()).withStyle(ChatFormatting.GOLD);
        return List.of(Component.translatable("button.raven_apoth.tier_level", WorldTierLevels.requiredLevel(tier), tierName).withStyle(ChatFormatting.RED));
    }
}
