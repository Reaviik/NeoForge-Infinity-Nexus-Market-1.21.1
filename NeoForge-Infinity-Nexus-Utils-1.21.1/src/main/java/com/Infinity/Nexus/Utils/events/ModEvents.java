package com.Infinity.Nexus.Utils.events;

import com.Infinity.Nexus.Utils.InfinityNexusUtils;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.server.command.ConfigCommand;

@EventBusSubscriber(modid = InfinityNexusUtils.MOD_ID)
public class ModEvents {
    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        //new Reload(event.getDispatcher());
        ConfigCommand.register(event.getDispatcher());
    }
}
