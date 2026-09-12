package dev.muon.raven_apoth.mixin.client;

import dev.muon.dynamic_difficulty.api.LevelingAPI;
import dev.muon.dynamic_difficulty.client.LevelPlateHandler;
import dev.muon.raven_apoth.config.RavenApothConfig;
import dev.muon.raven_apoth.loot.LootUtils;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LevelPlateHandler.class, remap = false)
public abstract class LevelPlateHandlerMixin {

    @Inject(
            method = "getLevelColor(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/LivingEntity;)I",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void raven_apoth$colorLevelByRarity(Player player, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        int entityLevel = LevelingAPI.getLevel(entity);
        String mapping = LootUtils.findMappingForLevel(RavenApothConfig.levelRarityMappings, entityLevel);
        String maxRarity = mapping.substring(mapping.lastIndexOf('-') + 1);
        DynamicHolder<LootRarity> rarity = RarityRegistry.INSTANCE.holder(raven_apoth$rarityId(maxRarity));
        if (rarity.isBound()) {
            cir.setReturnValue(0xFF000000 | rarity.get().color().getValue());
        }
    }

    @Unique
    private static ResourceLocation raven_apoth$rarityId(String name) {
        if (name.contains(":")) return ResourceLocation.parse(name);
        if (name.equals("ancient")) return ResourceLocation.fromNamespaceAndPath("ancientreforging", "ancient");
        return Apotheosis.loc(name);
    }
}
