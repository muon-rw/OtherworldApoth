package dev.muon.otherworldapoth.mixin.client;

import dev.muon.otherworld.leveling.LevelingUtils;
import dev.muon.otherworld.leveling.client.LevelDisplayRenderer;
import dev.muon.otherworldapoth.config.OWApothConfig;
import dev.muon.otherworldapoth.loot.LootUtils;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LevelDisplayRenderer.class, remap = false)
public class LevelDisplayRendererMixin {

    @Inject(method = "getLevelColor", at = @At("HEAD"), cancellable = true)
    private static void colorLevelsByRarity(Player player, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        int entityLevel;
        if (entity instanceof Player targetPlayer) {
            entityLevel = LevelingUtils.getPlayerLevel(targetPlayer);
        } else {
            entityLevel = LevelingUtils.getEntityLevel(entity);
        }

        String mapping = LootUtils.findMappingForLevel(OWApothConfig.levelRarityMappings, entityLevel);

        String[] rarities = mapping.split("-");
        String maxRarityId = rarities[1];

        DynamicHolder<LootRarity> maxRarity = RarityRegistry.INSTANCE.holder(Apotheosis.loc(maxRarityId));
        if (maxRarity.isBound()) {
            cir.setReturnValue(maxRarity.get().getColor().getValue());
        }
    }
}