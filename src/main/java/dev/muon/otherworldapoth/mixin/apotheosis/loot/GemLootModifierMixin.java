package dev.muon.otherworldapoth.mixin.apotheosis.loot;

import dev.shadowsoffire.apotheosis.adventure.loot.GemLootModifier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GemLootModifier.class, remap = false)
public class GemLootModifierMixin {
    @Inject(method = "doApply", at = @At("HEAD"), cancellable = true)
    private void cancelGemInjection(ObjectArrayList<ItemStack> generatedLoot, LootContext context, CallbackInfoReturnable<ObjectArrayList<ItemStack>> cir) {
        cir.setReturnValue(generatedLoot);
    }
}