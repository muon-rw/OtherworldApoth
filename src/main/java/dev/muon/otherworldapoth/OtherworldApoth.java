package dev.muon.otherworldapoth;

import dev.muon.otherworldapoth.affix.AffixRegistry;
import dev.muon.otherworldapoth.attribute.AttributeRegistry;
import dev.muon.otherworldapoth.config.LootConfig;
import dev.muon.otherworldapoth.loot.LeveledAffixLootModifier;
import dev.muon.otherworldapoth.loot.LeveledGemLootModifier;
import dev.muon.otherworldapoth.replacer.OWApothSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.event.AddPackFindersEvent;
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
        LootCategories.init();
        AffixRegistry.init();

        IEventBus modEventBus = context.getModEventBus();
        AttributeRegistry.init(modEventBus);
        modEventBus.addListener(this::registerLootModifiers);
        modEventBus.addListener(this::addReplacerPack);
    }

    private void addReplacerPack(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.SERVER_DATA) {
            event.addRepositorySource(new OWApothSource(PackType.SERVER_DATA));
        }
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