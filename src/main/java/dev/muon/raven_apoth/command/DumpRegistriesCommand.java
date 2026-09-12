package dev.muon.raven_apoth.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.muon.raven_apoth.RavenApoth;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.nio.file.Path;

public final class DumpRegistriesCommand {

    private static final String OUT_DIR = "raven_apoth_dumps";
    private static final String AUTO_DUMP_PROPERTY = "raven_apoth.dump";
    private static final String AUTO_DUMP_ENV = "RAVEN_APOTH_DUMP";

    private DumpRegistriesCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> dump = Commands.literal("dump")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("all").executes(ctx -> dump(ctx, null)));
        for (String target : RegistryDumper.targets()) {
            dump.then(Commands.literal(target).executes(ctx -> dump(ctx, target)));
        }
        dispatcher.register(Commands.literal("raven_apoth").then(dump));
    }

    public static void onServerStarted(ServerStartedEvent event) {
        if (!Boolean.getBoolean(AUTO_DUMP_PROPERTY) && !"true".equalsIgnoreCase(System.getenv(AUTO_DUMP_ENV))) return;
        MinecraftServer server = event.getServer();
        Path dir = outputDir();
        try {
            RegistryDumper dumper = new RegistryDumper(server, dir);
            int count = dumper.dumpAll();
            RavenApoth.LOGGER.info("Auto-dumped {} registry entries ({} errors) to {}", count, dumper.errors(), dir.toAbsolutePath());
        } catch (Exception e) {
            RavenApoth.LOGGER.error("Auto registry dump failed", e);
        }
        RavenApoth.LOGGER.info("Halting server after registry dump ({} was set)", AUTO_DUMP_PROPERTY);
        server.halt(false);
    }

    private static int dump(CommandContext<CommandSourceStack> context, String target) {
        CommandSourceStack source = context.getSource();
        Path dir = outputDir();
        try {
            RegistryDumper dumper = new RegistryDumper(source.getServer(), dir);
            int count = target == null ? dumper.dumpAll() : dumper.dump(target);
            String what = target == null ? "all registries" : target;
            source.sendSuccess(() -> Component.literal("Dumped " + count + " entries from " + what
                + " (" + dumper.errors() + " errors) to " + dir.toAbsolutePath()), true);
            RavenApoth.LOGGER.info("Registry dump of {} complete: {}", what, dir.toAbsolutePath());
            return count;
        } catch (Exception e) {
            RavenApoth.LOGGER.error("Registry dump failed", e);
            source.sendFailure(Component.literal("Dump failed: " + e.getMessage()));
            return 0;
        }
    }

    private static Path outputDir() {
        return FMLPaths.GAMEDIR.get().resolve(OUT_DIR);
    }
}
