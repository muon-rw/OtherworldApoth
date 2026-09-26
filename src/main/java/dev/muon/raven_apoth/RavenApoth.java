package dev.muon.raven_apoth;

import dev.muon.raven_apoth.affix.AffixEvents;
import dev.muon.raven_apoth.affix.AttunementAffix;
import dev.muon.raven_apoth.affix.SkillLevelAffix;
import dev.muon.raven_apoth.affix.SocketBonusAffix;
import dev.muon.raven_apoth.affix.SpellAttunementAffix;
import dev.muon.raven_apoth.affix.TransmutationAffix;
import dev.muon.raven_apoth.command.DumpRegistriesCommand;
import dev.muon.raven_apoth.config.RavenApothConfig;
import dev.muon.raven_apoth.data.OverridePacks;
import dev.muon.raven_apoth.loot.LootEvents;
import dev.muon.raven_apoth.tiers.SpawnTier;
import dev.muon.raven_apoth.loot.RavenApothLootModifiers;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(RavenApoth.MODID)
public class RavenApoth {
    public static final String MODID = "raven_apoth";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public RavenApoth(IEventBus modBus, ModContainer container) {
        RavenApothConfig.init();
        RavenApothComponents.register(modBus);
        RavenApothAttachments.register(modBus);
        RavenApothLootModifiers.register(modBus);
        OverridePacks.register(modBus);
        modBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(new AffixEvents());
        NeoForge.EVENT_BUS.register(new LootEvents());
        NeoForge.EVENT_BUS.register(new SpawnTier());
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> DumpRegistriesCommand.register(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener(DumpRegistriesCommand::onServerStarted);

        LOGGER.info("Loading {} {}", container.getModId(), container.getModInfo().getVersion());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        AffixRegistry.INSTANCE.registerCodec(loc("attunement"), AttunementAffix.CODEC);
        AffixRegistry.INSTANCE.registerCodec(loc("spell_attunement"), SpellAttunementAffix.CODEC);
        AffixRegistry.INSTANCE.registerCodec(loc("socket_bonus"), SocketBonusAffix.CODEC);
        AffixRegistry.INSTANCE.registerCodec(loc("transmutation"), TransmutationAffix.CODEC);
        AffixRegistry.INSTANCE.registerCodec(loc("skill_level"), SkillLevelAffix.CODEC);
    }
}
