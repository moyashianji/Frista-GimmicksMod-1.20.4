package com.furasuta.emergencyescape.init;

import com.furasuta.emergencyescape.EmergencyEscapeMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, EmergencyEscapeMod.MODID);

    public static final RegistryObject<SoundEvent> EXPLOSION = SOUNDS.register("explosion",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(EmergencyEscapeMod.MODID, "explosion")));

    public static final RegistryObject<SoundEvent> ZERO = SOUNDS.register("zero",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(EmergencyEscapeMod.MODID, "zero")));

    public static final RegistryObject<SoundEvent> ALERT1 = SOUNDS.register("alert1",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(EmergencyEscapeMod.MODID, "alert1")));

    public static final RegistryObject<SoundEvent> ALERT2 = SOUNDS.register("alert2",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(EmergencyEscapeMod.MODID, "alert2")));

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
