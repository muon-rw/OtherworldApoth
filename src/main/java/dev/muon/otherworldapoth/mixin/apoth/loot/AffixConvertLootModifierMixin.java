package dev.muon.otherworldapoth.mixin.apoth.loot;

import dev.shadowsoffire.apotheosis.adventure.loot.AffixConvertLootModifier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AffixConvertLootModifier.class, remap = false)
public class AffixConvertLootModifierMixin {
    @Inject(method = "doApply", at = @At("HEAD"), cancellable = true)
    private void cancelAffixConversion(ObjectArrayList<ItemStack> generatedLoot, LootContext context, CallbackInfoReturnable<ObjectArrayList<ItemStack>> cir) {
        cir.setReturnValue(generatedLoot);
    }
}