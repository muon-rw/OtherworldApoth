package dev.muon.otherworldapoth.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldapoth.LootCategories;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

public class MagicTelepathicAffix extends Affix {
    public static final Codec<MagicTelepathicAffix> CODEC = RecordCodecBuilder.create((inst) -> {
        return inst.group(LootRarity.CODEC.fieldOf("min_rarity").forGetter((a) -> {
            return a.minRarity;
        })).apply(inst, MagicTelepathicAffix::new);
    });
    public static Vec3 blockDropTargetPos = null;
    protected LootRarity minRarity;

    public MagicTelepathicAffix(LootRarity minRarity) {
        super(AffixType.ABILITY);
        this.minRarity = minRarity;
    }

    public void addInformation(ItemStack stack, LootRarity rarity, float level, Consumer<Component> list) {
        LootCategory cat = LootCategory.forItem(stack);
        String type = !cat.isRanged() && !cat.isWeapon() ? "tool" : "weapon";
        ResourceLocation var10001 = this.getId();
        list.accept(Component.translatable("affix." + var10001 + ".desc." + type));
    }

    public boolean enablesTelepathy() {
        return true;
    }

    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    public static void drops(DamageSource source, Collection<ItemEntity> drops) {
        boolean canTeleport;
        Vec3 targetPos;
        label25: {
            canTeleport = false;
            targetPos = null;
            Entity var6 = source.getDirectEntity();
            if (var6 instanceof AbstractArrow arrow) {
                if (arrow.getOwner() != null) {
                    canTeleport = AffixHelper.streamAffixes(arrow).anyMatch(AffixInstance::enablesTelepathy);
                    targetPos = arrow.getOwner().position();
                    break label25;
                }
            }

            var6 = source.getDirectEntity();
            if (var6 instanceof LivingEntity living) {
                ItemStack weapon = living.getMainHandItem();
                canTeleport = AffixHelper.streamAffixes(weapon).anyMatch(AffixInstance::enablesTelepathy);
                targetPos = living.position();
            }
        }

        if (canTeleport) {
            Iterator var7 = drops.iterator();

            while(var7.hasNext()) {
                ItemEntity item = (ItemEntity)var7.next();
                item.setPos(targetPos.x, targetPos.y, targetPos.z);
                item.setPickUpDelay(0);
            }
        }

    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        return (LootCategories.isStaff(stack) && rarity.isAtLeast(this.minRarity));
    }
}
