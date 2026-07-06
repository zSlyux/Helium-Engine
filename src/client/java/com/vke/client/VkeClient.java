package com.vke.client;

import com.vke.client.mixin.VkeMixinPlugin;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VkeClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("VKE");

    @Override
    public void onInitializeClient() {
        if (VkeMixinPlugin.isEngineActive()) {
            LOGGER.info("[VKE] Motor Vulkan activo");
        } else {
            LOGGER.info("[VKE] Motor Vulkan inactivo, el juego usa su pipeline normal");
        }
    }
}
