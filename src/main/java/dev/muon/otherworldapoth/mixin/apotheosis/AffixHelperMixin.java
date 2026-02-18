package dev.muon.otherworldapoth.mixin.apotheosis;

import dev.muon.otherworldapoth.LootCategories;
import dev.muon.otherworldapoth.OtherworldApoth;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Handles compensation for loot categories that don't support durability (like staffs).
 * Instead of receiving a durability bonus (which is useless for non-damageable items),
 * these items receive a replacement affix.
 * 
 * Intercepts AffixHelper.setAffixes() to remove DURABLE and add compensation before NBT is written.
 */
@Mixin(value = AffixHelper.class, remap = false)
public class AffixHelperMixin {
    
    @Unique
    private static final Random jRand = new Random();
    
    /**
     * Before affixes are written to NBT, check if this is a staff with DURABLE.
     * If so, remove DURABLE and add a compensation affix.
     */
    @Inject(
        method = "setAffixes",
        at = @At("HEAD")
    )
    private static void replaceStaffDurability(
        ItemStack stack,
        Map<DynamicHolder<? extends Affix>, AffixInstance> affixes,
        CallbackInfo ci
    ) {
        LootCategory cat = LootCategory.forItem(stack);
        if (!cat.equals(LootCategories.STAFF)) return;
        
        // Find the DURABLE affix if present
        DynamicHolder<? extends Affix> durableKey = null;
        AffixInstance durableInstance = null;
        for (Map.Entry<DynamicHolder<? extends Affix>, AffixInstance> entry : affixes.entrySet()) {
            if (entry.getKey().getId().getPath().equals("durable")) {
                durableKey = entry.getKey();
                durableInstance = entry.getValue();
                break;
            }
        }
        
        if (durableKey == null) {
            return;
        }
        
        // Get the rarity from the durable instance
        LootRarity rarity = durableInstance.rarity().get();
        OtherworldApoth.LOGGER.debug("[StaffDurabilityCompensation] Removing DURABLE affix from {} (rarity: {})", 
            stack.getItem(), rarity);
        
        // Remove the durable affix
        affixes.remove(durableKey);
        
        // Build set of current affixes for getAvailableAffixes check
        Set<DynamicHolder<? extends Affix>> currentAffixes = new HashSet<>(affixes.keySet());
        
        // Determine compensation based on rarity (matching 1.21.1 staff override values)
        // Higher rarities get more compensation since they would have had better durability
        int rarityOrdinal = rarity.ordinal();
        
        // Compensation table based on 1.21.1 staff overrides vs base:
        // - Rare (2): 1 stat
        // - Epic (3): 1 stat + 1 potion
        // - Mythic (4): 1 stat + 1 potion
        // - Ancient (5): 2 stats + 1 potion
        AffixType[][] compensationByRarity = {
            // Common (0) - shouldn't have durability, but just in case
            { AffixType.STAT },
            // Uncommon (1) - shouldn't have durability, but just in case
            { AffixType.STAT },
            // Rare (2)
            { AffixType.STAT },
            // Epic (3)
            { AffixType.STAT, AffixType.POTION },
            // Mythic (4)
            { AffixType.STAT, AffixType.POTION },
            // Ancient (5)
            { AffixType.STAT, AffixType.STAT, AffixType.POTION }
        };
        
        // Get the compensation types for this rarity (default to single stat if ordinal out of range)
        AffixType[] compensationTypes = rarityOrdinal < compensationByRarity.length 
            ? compensationByRarity[rarityOrdinal] 
            : new AffixType[] { AffixType.STAT };
        
        jRand.setSeed(System.nanoTime());
        int addedCount = 0;
        
        for (AffixType targetType : compensationTypes) {
            if (tryAddAffix(stack, rarity, affixes, currentAffixes, targetType)) {
                addedCount++;
            }
        }
        
        if (addedCount == 0) {
            OtherworldApoth.LOGGER.warn("[StaffDurabilityCompensation] No replacement affixes available for staff!");
        } else {
            OtherworldApoth.LOGGER.debug("[StaffDurabilityCompensation] Added {} compensation affix(es) for {} rarity", 
                addedCount, rarity);
        }
    }
    
    /**
     * Attempts to add an affix of the specified type to the affix map.
     * Falls back through STAT -> POTION -> ABILITY if the target type isn't available.
     * 
     * @return true if an affix was added, false otherwise
     */
    @Unique
    private static boolean tryAddAffix(
        ItemStack stack,
        LootRarity rarity,
        Map<DynamicHolder<? extends Affix>, AffixInstance> affixes,
        Set<DynamicHolder<? extends Affix>> currentAffixes,
        AffixType targetType
    ) {
        // Try the target type first, then fallback options
        AffixType[] fallbackOrder = { targetType, AffixType.STAT, AffixType.POTION, AffixType.ABILITY };
        
        for (AffixType type : fallbackOrder) {
            List<DynamicHolder<? extends Affix>> available = LootController.getAvailableAffixes(
                stack, rarity, currentAffixes, type
            );
            
            if (!available.isEmpty()) {
                Collections.shuffle(available, jRand);
                DynamicHolder<? extends Affix> chosen = available.get(0);
                
                OtherworldApoth.LOGGER.debug("[StaffDurabilityCompensation] Adding {} affix: {}", 
                    type, chosen.getId());
                
                AffixInstance replacement = new AffixInstance(
                    chosen, stack, 
                    RarityRegistry.INSTANCE.holder(rarity), 
                    jRand.nextFloat()
                );
                affixes.put(chosen, replacement);
                currentAffixes.add(chosen);
                return true;
            }
        }
        
        return false;
    }
}
