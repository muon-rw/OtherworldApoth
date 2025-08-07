package dev.muon.otherworldapoth;

import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import io.redspace.ironsspellbooks.item.CastingItem;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;


public class LootCategories {
    public static final LootCategory STAFF = LootCategory.register(LootCategory.HEAVY_WEAPON, "staff",
            s -> s.getItem() instanceof CastingItem,
            arr(EquipmentSlot.MAINHAND));

    private static EquipmentSlot[] arr(EquipmentSlot... s) {
        return s;
    }

    public static boolean isStaff(ItemStack stack) {
        return LootCategory.forItem(stack).equals(STAFF);
    }

    public static void init() {}
}
