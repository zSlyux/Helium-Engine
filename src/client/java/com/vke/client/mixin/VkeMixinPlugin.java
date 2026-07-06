package com.vke.client.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class VkeMixinPlugin implements IMixinConfigPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("VKE");
    private static final boolean FORCE = Boolean.getBoolean("vke.force");
    private static final boolean DISABLE = Boolean.getBoolean("vke.disable");

    private static boolean engineActive;

    public static boolean isEngineActive() {
        return engineActive;
    }

    @Override
    public void onLoad(String mixinPackage) {
        FabricLoader loader = FabricLoader.getInstance();
        boolean otherRenderer = loader.isModLoaded("sodium")
                || loader.isModLoaded("iris")
                || loader.isModLoaded("vulkanmod");

        engineActive = !DISABLE && (FORCE || !otherRenderer);

        if (DISABLE) {
            LOGGER.info("[VKE] Motor desactivado por -Dvke.disable=true");
        } else if (otherRenderer && !FORCE) {
            LOGGER.warn("[VKE] Otro renderer detectado (Sodium/Iris/VulkanMod), el motor queda inactivo");
        } else if (otherRenderer) {
            LOGGER.error("[VKE] Motor forzado con otro renderer presente, es probable un crash");
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return engineActive;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
