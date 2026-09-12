package dev.muon.raven_apoth.mixin.apotheosis;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.loot.LootRule;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * SocketLootRule reads the event-modified socket count, so a socket_bonus affix already on the
 * item makes the rule's "only raise" check skip the roll. Read the stored component instead.
 */
@Mixin(value = LootRule.SocketLootRule.class, remap = false)
public class SocketLootRuleMixin {

    @WrapOperation(
        method = "execute",
        at = @At(value = "INVOKE", target = "Ldev/shadowsoffire/apotheosis/socket/SocketHelper;getSockets(Lnet/minecraft/world/item/ItemStack;)I")
    )
    private int raven_apoth$readStoredSockets(ItemStack stack, Operation<Integer> original) {
        return stack.getOrDefault(Apoth.Components.SOCKETS, 0);
    }
}
