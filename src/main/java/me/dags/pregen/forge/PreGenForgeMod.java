package me.dags.pregen.forge;

import me.dags.pregen.PreGen;
import me.dags.pregen.command.PreGenCommand;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;

@Mod("pregenforge")
@Mod.EventBusSubscriber
public class PreGenForgeMod {

    @SubscribeEvent
    public static void starting(FMLServerStartingEvent event) {
        // register commands
        event.getCommandDispatcher().register(PreGenCommand.command());

        // initialize pregen with the mc server and a task scheduler implementation
        PreGen.init(event.getServer(), new ForgeTaskScheduler());

        // startup handles loading of saved pregen configs
        PreGen.getInstance().onStartup();
    }

    @SubscribeEvent
    public static void stopping(FMLServerStoppingEvent event) {
        PreGen.getInstance().onShutdown();
    }
}
