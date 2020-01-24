package com.terraforged.pregen.forge;

import com.terraforged.pregen.PreGen;
import com.terraforged.pregen.command.PreGenCommand;
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
        // non-forge platforms may wish to use 'me.dags.pregen.task.WorldTaskScheduler.INSTANCE' (tick methods need "wiring up")
        PreGen.init(event.getServer(), new ForgeTaskScheduler());

        // startup handles loading of saved pregen configs
        PreGen.getInstance().onStartup();
    }

    @SubscribeEvent
    public static void stopping(FMLServerStoppingEvent event) {
        PreGen.getInstance().onShutdown();
    }
}
