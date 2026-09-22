package dev.muon.raven_apoth.compat.jade;

import dev.muon.raven_apoth.RavenApoth;
import dev.muon.raven_apoth.loot.DropProfile;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum MobLootProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = RavenApoth.loc("mob_loot");
    private static final String DATA = "raven_apoth_loot";

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (!(accessor.getEntity() instanceof LivingEntity mob) || !DropProfile.isEligible(mob)) {
            return;
        }
        DropProfile profile = DropProfile.of(mob, accessor.getPlayer().getLuck());
        CompoundTag tag = new CompoundTag();
        tag.putString("min_rarity", RarityRegistry.INSTANCE.getKey(profile.rarities().getFirst()).toString());
        tag.putString("max_rarity", RarityRegistry.INSTANCE.getKey(profile.rarities().getLast()).toString());
        tag.putInt("min_purity", profile.purities().stream().mapToInt(Enum::ordinal).min().orElse(0));
        tag.putInt("max_purity", profile.purities().stream().mapToInt(Enum::ordinal).max().orElse(0));
        tag.putFloat("affix_chance", profile.affixChance());
        tag.putFloat("gem_chance", profile.gemChance());
        tag.putInt("tier", profile.tier().ordinal());
        tag.putInt("champion_rank", profile.championRank());
        data.put(DATA, tag);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (!accessor.getServerData().contains(DATA)) {
            return;
        }
        CompoundTag tag = accessor.getServerData().getCompound(DATA);
        MutableComponent gear = range(rarity(tag.getString("min_rarity")), rarity(tag.getString("max_rarity")));
        if (!accessor.showDetails()) {
            tooltip.add(Component.translatable("jade.raven_apoth.drop_tier", gear));
            return;
        }
        Purity[] purities = Purity.values();
        tooltip.add(Component.translatable("jade.raven_apoth.gear", gear));
        tooltip.add(Component.translatable("jade.raven_apoth.gems",
                range(purities[tag.getInt("min_purity")].toComponent(), purities[tag.getInt("max_purity")].toComponent())));
        tooltip.add(Component.translatable("jade.raven_apoth.chances",
                percent(tag.getFloat("affix_chance")), percent(tag.getFloat("gem_chance"))).withStyle(ChatFormatting.GRAY));
        Component tier = WorldTier.values()[tag.getInt("tier")].toComponent();
        int rank = tag.getInt("champion_rank");
        tooltip.add((rank > 0 ? Component.translatable("jade.raven_apoth.champion_tier", rank, tier) : tier.copy()).withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    private static Component rarity(String id) {
        DynamicHolder<LootRarity> holder = RarityRegistry.INSTANCE.holder(ResourceLocation.parse(id));
        return holder.isBound() ? holder.get().toComponent() : Component.literal(id);
    }

    private static MutableComponent range(Component low, Component high) {
        if (low.getString().equals(high.getString())) {
            return low.copy();
        }
        return Component.translatable("jade.raven_apoth.range", low, high);
    }

    private static String percent(float chance) {
        return Math.round(chance * 100) + "%";
    }
}
