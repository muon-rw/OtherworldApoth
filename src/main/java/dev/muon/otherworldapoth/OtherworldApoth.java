package dev.muon.otherworldapoth;

import dev.muon.otherworldapoth.affix.*;
import dev.muon.otherworldapoth.config.OWApothConfig;
import dev.muon.otherworldapoth.loot.LeveledAffixLootModifier;
import dev.muon.otherworldapoth.loot.LeveledGemLootModifier;
import dev.muon.otherworldapoth.loot.LootEvents;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Mod(OtherworldApoth.MODID)
public class OtherworldApoth {
    public static final String MODID = "otherworldapoth";
    public static final Logger LOGGER = LogManager.getLogger();

    public static ResourceLocation loc(String path) {
        return new ResourceLocation(OtherworldApoth.MODID, path);
    }


    public OtherworldApoth(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerLootModifiers);

        OWApothConfig.init();
        LootCategories.init();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            MinecraftForge.EVENT_BUS.register(new AffixEvents());
            MinecraftForge.EVENT_BUS.register(new LootEvents());
            AffixRegistry.INSTANCE.registerCodec(loc("attribute"), SchoolAttributeAffix.CODEC);
            AffixRegistry.INSTANCE.registerCodec(loc("spell_effect"), SpellEffectAffix.CODEC);
            AffixRegistry.INSTANCE.registerCodec(loc("magic_telepathic"), MagicTelepathicAffix.CODEC);
            AffixRegistry.INSTANCE.registerCodec(loc("spell_level"), SpellLevelAffix.CODEC);
            AffixRegistry.INSTANCE.registerCodec(loc("spell_trigger"), SpellTriggerAffix.CODEC);
            AffixRegistry.INSTANCE.registerCodec(loc("mana_cost"), ManaCostAffix.CODEC);
            AffixRegistry.INSTANCE.registerCodec(loc("socket_bonus"), SocketBonusAffix.CODEC);
        });
    }

    private void registerLootModifiers(RegisterEvent event) {
        if (event.getRegistryKey().equals(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS)) {
            event.register(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    loc("leveled_affixes"),
                    () -> LeveledAffixLootModifier.CODEC);
            event.register(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    loc("leveled_gems"),
                    () -> LeveledGemLootModifier.CODEC);
        }
    }

}