package dev.muon.raven_apoth.mixin.compat.raven_dnd_origins;

import dev.muon.raven_apoth.affix.AttunementAffix;
import dev.muon.raven_dnd_origins.restrictions.EnchantmentRestrictions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EnchantmentRestrictions.class, remap = false)
public class EnchantmentRestrictionsMixin {

    @Inject(
            method = "isEnchantmentAllowed(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/ResourceLocation;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void raven_apoth$allowWhenAttuned(Player player, ItemStack stack, ResourceLocation enchantmentId, CallbackInfoReturnable<Boolean> cir) {
        if (!stack.isEmpty() && EnchantmentRestrictions.getRequiredClass(enchantmentId) != null && AttunementAffix.isAttuned(stack)) {
            cir.setReturnValue(true);
        }
    }
}
