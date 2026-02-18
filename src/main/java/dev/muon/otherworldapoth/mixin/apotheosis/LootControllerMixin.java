package dev.muon.otherworldapoth.mixin.apotheosis;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LootController.class, remap = false)
public class LootControllerMixin {

    /**
     * Apotheosis has a bug where they read the (potentially event-modified) socket count here;
     * This leads to infinitely recursing sockets if anyone actually tries adding sockets via
     * the event passing the previous value.
     *
     * Change this access to read the socket NBT directly instead, which does not read from the event.
     */
    @WrapOperation(
            method = "createLootItem(Lnet/minecraft/world/item/ItemStack;Ldev/shadowsoffire/apotheosis/adventure/loot/LootCategory;Ldev/shadowsoffire/apotheosis/adventure/loot/LootRarity;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/item/ItemStack;",
            at = @At(value = "INVOKE", target = "Ldev/shadowsoffire/apotheosis/adventure/socket/SocketHelper;getSockets(Lnet/minecraft/world/item/ItemStack;)I")
    )
    private static int getBaseSocketsOnly(ItemStack stack, Operation<Integer> original) {
        CompoundTag afxData = stack.getTagElement("affix_data");
        return afxData != null ? afxData.getInt("sockets") : 0;
    }
}