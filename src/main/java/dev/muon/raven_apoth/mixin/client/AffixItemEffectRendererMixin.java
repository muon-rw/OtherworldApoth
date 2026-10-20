package dev.muon.raven_apoth.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import dev.muon.raven_core.event.QuestItemEvents;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.client.AffixItemEffectRenderer;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AffixItemEffectRenderer.class, remap = false)
public abstract class AffixItemEffectRendererMixin {
    @Unique
    private static final int raven_apoth$QUEST_BEAM_COLOR = 0xFFFF55;

    // Quest items borrow mythic's beam, shadow and particles so they read as the most important drop on the ground.
    @WrapOperation(
            method = "render",
            at = @At(value = "INVOKE", target = "Ldev/shadowsoffire/apotheosis/affix/AffixHelper;getRarity(Lnet/minecraft/world/item/ItemStack;)Ldev/shadowsoffire/placebo/reload/DynamicHolder;")
    )
    private static DynamicHolder<LootRarity> raven_apoth$renderQuestItemsAsMythic(ItemStack stack, Operation<DynamicHolder<LootRarity>> original,
                                                                               @Share("questItem") LocalBooleanRef questItem) {
        DynamicHolder<LootRarity> rarity = original.call(stack);
        questItem.set(!rarity.isBound() && QuestItemEvents.isQuestItem(stack));
        return questItem.get() ? RarityRegistry.INSTANCE.holder(Apotheosis.loc("mythic")) : rarity;
    }

    @ModifyExpressionValue(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/TextColor;getValue()I")
    )
    private static int raven_apoth$colorQuestItemBeamYellow(int color, @Share("questItem") LocalBooleanRef questItem) {
        return questItem.get() ? raven_apoth$QUEST_BEAM_COLOR : color;
    }
}
