package com.vke.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.vke.client.core.VulkanContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {

    @Redirect(
            method = "flipFrame",
            require = 0,
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V"
            )
    )
    private static void vke$present(long window) {
        VulkanContext.presentFrame();
    }
}
