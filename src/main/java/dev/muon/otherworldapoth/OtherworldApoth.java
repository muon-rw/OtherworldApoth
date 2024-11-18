package dev.muon.otherworldapoth;

import dev.muon.otherworldapoth.config.LootConfig;
import dev.muon.otherworldapoth.loot.LeveledAffixLootModifier;
import dev.muon.otherworldapoth.loot.LeveledGemLootModifier;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
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

        LootConfig.init();
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::registerLootModifiers);
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