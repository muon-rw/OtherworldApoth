package dev.muon.raven_apoth.mixin.compat.raven_dnd_origins;

import dev.muon.raven_apoth.affix.SpellAttunementAffix;
import dev.muon.raven_dnd_origins.restrictions.SpellRestrictions;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SpellRestrictions.class, remap = false)
public class SpellRestrictionsMixin {

    @Inject(
            method = "isSpellAllowed(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void raven_apoth$allowWhenSpellAttuned(Player player, ItemStack source, AbstractSpell spell, CallbackInfoReturnable<Boolean> cir) {
        if (!source.isEmpty() && SpellAttunementAffix.isAttuned(source)) {
            cir.setReturnValue(true);
        }
    }
}
