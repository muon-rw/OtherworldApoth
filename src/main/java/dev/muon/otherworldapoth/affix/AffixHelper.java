package dev.muon.otherworldapoth.affix;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class AffixHelper {
    private static final String AFFIX_DATA = "AffixData";
    private static final String AFFIXES = "Affixes";

    public static Map<DynamicHolder<? extends Affix>, AffixInstance> getAffixes(Projectile projectile) {
        Map<DynamicHolder<? extends Affix>, AffixInstance> map = new HashMap<>();
        CompoundTag afxData = projectile.getPersistentData().getCompound(AFFIX_DATA);

        if (afxData != null && afxData.contains(AFFIXES)) {
            CompoundTag affixes = afxData.getCompound(AFFIXES);
            DynamicHolder<LootRarity> rarity = dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper.getRarity(afxData);
            if (!rarity.isBound()) rarity = RarityRegistry.getMinRarity();

            for (String key : affixes.getAllKeys()) {
                DynamicHolder<Affix> affix = AffixRegistry.INSTANCE.holder(new ResourceLocation(key));
                if (!affix.isBound()) continue;
                float lvl = affixes.getFloat(key);
                map.put(affix, new AffixInstance(affix, ItemStack.EMPTY, rarity, lvl));
            }
        }
        return map;
    }

    public static Stream<AffixInstance> streamAffixes(Projectile projectile) {
        return getAffixes(projectile).values().stream();
    }
}