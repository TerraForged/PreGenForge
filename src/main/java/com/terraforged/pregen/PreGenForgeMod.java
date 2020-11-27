package com.terraforged.pregen;

import com.terraforged.pregen.command.PreGenCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;

@Mod("pregenforge")
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PreGenForgeMod {

    public PreGenForgeMod() {
        ModLoadingContext.get().getActiveContainer().registerExtensionPoint(
                ExtensionPoint.DISPLAYTEST,
                () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (s, b) -> true)
        );
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        // register commands
        PreGenCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void starting(FMLServerStartingEvent event) {
        // initialize pregen with the mc server and a task scheduler implementation
        // non-forge platforms may wish to use 'me.dags.pregen.task.WorldTaskScheduler.INSTANCE' (tick methods need "wiring up")
        PreGen.init(event.getServer(), new ForgeTaskScheduler());
    }

    @SubscribeEvent
    public static void started(FMLServerStartedEvent event) {
        // startup handles loading of saved pregen configs
        PreGen.getInstance().onStartup();
    }

    @SubscribeEvent
    public static void stopping(FMLServerStoppingEvent event) {
        PreGen.getInstance().onShutdown();
    }
}
