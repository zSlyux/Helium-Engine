package com.vke.client.mixin;

import com.mojang.blaze3d.platform.Window;
import com.vke.client.core.VulkanContext;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Window.class)
public abstract class WindowMixin {

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"
            )
    )
    private long vke$createWindow(int width, int height, CharSequence title, long monitor, long share) {
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
        long handle = GLFW.glfwCreateWindow(width, height, title, monitor, share);
        if (handle == MemoryUtil.NULL) {
            throw new IllegalStateException("GLFW no pudo crear la ventana NO_API");
        }
        VulkanContext.init(handle);
        return handle;
    }

    @Redirect(
            method = "<init>",
            require = 0,
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/glfw/GLFW;glfwMakeContextCurrent(J)V"
            )
    )
    private void vke$skipMakeContextCurrent(long window) {
    }

    @Redirect(
            method = "<init>",
            require = 0,
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/GL;createCapabilities()Lorg/lwjgl/opengl/GLCapabilities;"
            )
    )
    private GLCapabilities vke$skipGlCapabilities() {
        return null;
    }

    @Redirect(
            method = "updateVsync(Z)V",
            require = 0,
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/glfw/GLFW;glfwSwapInterval(I)V"
            )
    )
    private void vke$vsync(int interval) {
        VulkanContext.requestVsync(interval > 0);
    }
}
