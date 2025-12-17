package com.furasuta.emergencyescape.init;

import com.furasuta.emergencyescape.EmergencyEscapeMod;
import com.furasuta.emergencyescape.item.EmergencyEscapeItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, EmergencyEscapeMod.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EmergencyEscapeMod.MODID);

    public static final RegistryObject<Item> EMERGENCY_ESCAPE_ITEM = ITEMS.register("emergency_escape",
            () -> new EmergencyEscapeItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> EMERGENCY_ESCAPE_TAB = CREATIVE_MODE_TABS.register("emergency_escape_tab",
            () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> EMERGENCY_ESCAPE_ITEM.get().getDefaultInstance())
                    .title(Component.translatable("itemGroup.emergencyescape"))
                    .displayItems((parameters, output) -> {
                        output.accept(EMERGENCY_ESCAPE_ITEM.get());
                    }).build());

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
