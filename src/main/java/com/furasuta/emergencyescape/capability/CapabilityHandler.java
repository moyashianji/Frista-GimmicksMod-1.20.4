package com.furasuta.emergencyescape.capability;

import com.furasuta.emergencyescape.EmergencyEscapeMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EmergencyEscapeMod.MODID)
public class CapabilityHandler {

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(BodyPartHealthCapability.ID, new BodyPartHealthCapability.Provider());
            event.addCapability(DamageConsumptionCapability.ID, new DamageConsumptionCapability.Provider());
            event.addCapability(EmergencyEscapeCapability.ID, new EmergencyEscapeCapability.Provider());
        }
    }
}
