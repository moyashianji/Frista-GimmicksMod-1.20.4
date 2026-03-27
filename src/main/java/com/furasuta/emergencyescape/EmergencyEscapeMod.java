package com.furasuta.emergencyescape;

import com.furasuta.emergencyescape.capability.BodyPartHealthCapability;
import com.furasuta.emergencyescape.capability.DamageConsumptionCapability;
import com.furasuta.emergencyescape.capability.EmergencyEscapeCapability;
import com.furasuta.emergencyescape.client.ClientSetup;
import com.furasuta.emergencyescape.config.ModConfig;
import com.furasuta.emergencyescape.init.ModItems;
import com.furasuta.emergencyescape.init.ModSounds;
import com.furasuta.emergencyescape.network.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(EmergencyEscapeMod.MODID)
public class EmergencyEscapeMod {
    public static final String MODID = "emergencyescape";
    public static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("removal")
    public EmergencyEscapeMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(Type.COMMON, ModConfig.SPEC);
        ModItems.register(modEventBus);
        ModSounds.register(modEventBus);
        modEventBus.addListener(this::commonSetup);

        modEventBus.addListener(BodyPartHealthCapability::register);
        modEventBus.addListener(DamageConsumptionCapability::register);
        modEventBus.addListener(EmergencyEscapeCapability::register);

        MinecraftForge.EVENT_BUS.register(this);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modEventBus.addListener(ClientSetup::init);
        });
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NetworkHandler.register();
        });
    }
}
