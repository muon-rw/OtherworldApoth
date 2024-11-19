package dev.muon.otherworldapoth.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldapoth.LootCategories;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

public class MagicTelepathicAffix extends Affix {
    public static final Codec<MagicTelepathicAffix> CODEC = RecordCodecBuilder.create((inst) ->
            inst.group(LootRarity.CODEC.fieldOf("min_rarity").forGetter(a -> a.minRarity))
                    .apply(inst, MagicTelepathicAffix::new));

    protected LootRarity minRarity;

    public MagicTelepathicAffix(LootRarity minRarity) {
        super(AffixType.ABILITY);
        this.minRarity = minRarity;
    }

    @Override
    public boolean enablesTelepathy() {
        return true;
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public MutableComponent getDescription(ItemStack stack, LootRarity rarity, float level) {
        return Component.translatable("affix." + this.getId() + ".desc.staff");
    }

    public static void drops(LivingDropsEvent e) {
        DamageSource src = e.getSource();
        Entity directEntity = src.getDirectEntity();
        Entity causingEntity = src.getEntity();

        boolean canTeleport = false;
        Vec3 targetPos = null;

        if (directEntity instanceof Projectile spell && spell.getOwner() != null) {
            canTeleport = AffixHelper.streamAffixes(spell).anyMatch(AffixInstance::enablesTelepathy);
            if (canTeleport) {
                targetPos = spell.getOwner().position();
            }
        }

        if (!canTeleport && causingEntity instanceof LivingEntity living) {
            ItemStack weapon = living.getMainHandItem();

            canTeleport = dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper.streamAffixes(weapon).anyMatch(AffixInstance::enablesTelepathy);
            if (canTeleport) {
                targetPos = living.position();
            }
        }

        if (canTeleport) {
            for (ItemEntity item : e.getDrops()) {
                item.setPos(targetPos.x, targetPos.y, targetPos.z);
                item.setPickUpDelay(0);
            }
        }
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        return LootCategories.isStaff(stack) && rarity.isAtLeast(this.minRarity);
    }
}