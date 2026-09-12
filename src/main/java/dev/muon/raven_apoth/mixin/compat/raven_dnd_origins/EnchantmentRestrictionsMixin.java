package dev.muon.raven_apoth.mixin.compat.raven_dnd_origins;

import dev.muon.raven_apoth.affix.AttunementAffix;
import dev.muon.raven_dnd_origins.restrictions.EnchantmentRestrictions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EnchantmentRestrictions.class, remap = false)
public class EnchantmentRestrictionsMixin {

    @Inject(method = "isEnchantmentAllowed(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/resources/ResourceLocation;)Z", at = @At("HEAD"), cancellable = true)
    private static void raven_apoth$allowWhenAttuned(Player player, ResourceLocation enchantmentId, CallbackInfoReturnable<Boolean> cir) {
        if (EnchantmentRestrictions.getRequiredClass(enchantmentId) == null) {
            return;
        }
        for (EquipmentSlot slot : raven_apoth$slotsFor(enchantmentId)) {
            ItemStack equipment = player.getItemBySlot(slot);
            if (!equipment.isEmpty() && AttunementAffix.isAttuned(equipment)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }

    @Unique
    private static EquipmentSlot[] raven_apoth$slotsFor(ResourceLocation enchantmentId) {
        return switch (enchantmentId.getPath()) {
            case "mending" -> EquipmentSlot.values();
            case "feather_falling" -> new EquipmentSlot[]{EquipmentSlot.FEET};
            case "thorns" -> new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            default -> new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
        };
    }
}
