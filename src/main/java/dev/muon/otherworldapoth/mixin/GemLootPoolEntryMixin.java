package dev.muon.otherworldapoth.mixin;

import dev.shadowsoffire.apotheosis.adventure.loot.GemLootPoolEntry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(value = GemLootPoolEntry.class, remap = false)
public class GemLootPoolEntryMixin {
    @Inject(method = "createItemStack", at = @At("HEAD"), cancellable = true, remap = true)
    private void cancelGemInjection(Consumer<ItemStack> list, LootContext ctx, CallbackInfo ci) {
        // TODO: We might want to use this system in some cases, instead of disabling it entirely
        ci.cancel();
    }
}
