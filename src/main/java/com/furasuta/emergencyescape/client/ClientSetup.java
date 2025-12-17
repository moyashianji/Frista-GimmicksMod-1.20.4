package com.furasuta.emergencyescape.client;

import com.furasuta.emergencyescape.EmergencyEscapeMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = EmergencyEscapeMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    public static void init(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(HealthBarRenderer.class);
        MinecraftForge.EVENT_BUS.register(KeyInputHandler.class);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KeyInputHandler.ESCAPE_KEY);
    }
}
