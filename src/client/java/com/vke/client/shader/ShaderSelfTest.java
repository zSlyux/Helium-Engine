package com.vke.client.shader;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.vkDestroyShaderModule;

public final class ShaderSelfTest {

    private static final Logger LOGGER = LoggerFactory.getLogger("VKE");

    private ShaderSelfTest() {
    }

    public static void run(VkDevice device) {
        test(device, "/assets/vke/shaders/test_triangle.vert", ShaderCompiler.Stage.VERTEX);
        test(device, "/assets/vke/shaders/test_triangle.frag", ShaderCompiler.Stage.FRAGMENT);
        test(device, "/assets/vke/shaders/test_fullscreen.vert", ShaderCompiler.Stage.VERTEX);
        test(device, "/assets/vke/shaders/test_plasma.frag", ShaderCompiler.Stage.FRAGMENT);
        LOGGER.info("[VKE] Self-test de shaders completado");
    }

    private static void test(VkDevice device, String resource, ShaderCompiler.Stage stage) {
        ByteBuffer spirv = ShaderCompiler.compileToSpirv(resource,
                ShaderCompiler.loadSource(resource), stage);
        try {
            long module = ShaderCompiler.createModule(device, spirv);
            vkDestroyShaderModule(device, module, null);
            LOGGER.info("[VKE] {} -> {} bytes de SPIR-V", resource, spirv.remaining());
        } finally {
            MemoryUtil.memFree(spirv);
        }
    }
}
