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
        warmUpGuava();
    }

    /**
     * Forge開発クライアントで、サーバー接続時（ConnectScreen/AddressCheck）に
     * com.google.common.* が稀にClassNotFoundError/NoClassDefFoundErrorになる不具合の緩和。
     * 接続時に使われるGuavaクラスを、負荷の低い起動時に先読みしてキャッシュさせておく。
     * ※本番環境では発生しない問題だが、無害なので常時実行する。
     */
    private static void warmUpGuava() {
        try {
            com.google.common.hash.Hashing.sha1().hashUnencodedChars("warmup");
            com.google.common.net.InetAddresses.forString("127.0.0.1");
            com.google.common.collect.LinkedHashMultimap.create().put("a", "b");
            EmergencyEscapeMod.LOGGER.info("[EmergencyEscape] Guava pre-load OK (connect-crash mitigation)");
        } catch (Throwable t) {
            EmergencyEscapeMod.LOGGER.warn("[EmergencyEscape] Guava pre-load failed: {}", t.toString());
        }
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KeyInputHandler.ESCAPE_KEY);
    }
}
