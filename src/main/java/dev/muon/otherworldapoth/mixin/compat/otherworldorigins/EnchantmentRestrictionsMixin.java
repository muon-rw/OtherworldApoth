package dev.muon.otherworldapoth.mixin.compat.otherworldorigins;

import dev.muon.otherworldapoth.affix.AttunementAffix;
import dev.muon.otherworldorigins.restrictions.EnchantmentRestrictions;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EnchantmentRestrictions.class, remap = false)
public class EnchantmentRestrictionsMixin {

    @Inject(method = "isEnchantmentAllowed(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/enchantment/Enchantment;)Z", at = @At("HEAD"), cancellable = true)
    private static void checkAttunementAffix(Player player, Enchantment enchantment, CallbackInfoReturnable<Boolean> cir) {
        // Determine which equipment slots to check based on the enchantment type
        EquipmentSlot[] slotsToCheck = getSlotsForEnchantment(enchantment);
        
        // Check only the relevant equipment slots for the Attunement affix
        for (EquipmentSlot slot : slotsToCheck) {
            ItemStack equipment = player.getItemBySlot(slot);
            if (equipment.isEmpty()) {
                continue;
            }

            // Check if this equipment has the Attunement affix
            boolean hasAttunement = AffixHelper.streamAffixes(equipment)
                    .anyMatch(instance -> instance.affix().get() instanceof AttunementAffix);

            if (hasAttunement) {
                // If the player has Attunement affix on relevant equipment, allow the enchantment
                cir.setReturnValue(true);
                return;
            }
        }
    }

    // Helper to reduce unnecessary slot lookups
    // TODO: Probably better: pass the context of an itemstack within EnchantmentRestrictions itself
    @Unique
    private static EquipmentSlot[] getSlotsForEnchantment(Enchantment enchantment) {
        ResourceLocation enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        if (enchantmentId == null) {
            // Unknown enchantment - check all slots to be safe
            return EquipmentSlot.values();
        }

        String path = enchantmentId.getPath();

        return switch (path) {
            case "mending" -> EquipmentSlot.values();
            case "feather_falling" -> new EquipmentSlot[]{EquipmentSlot.FEET};
            case "thorns" ->
                    new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            default ->
                    new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
        };

    }
}
