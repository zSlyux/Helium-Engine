package com.vke.client.shader;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.VK10.*;

public final class ShaderCompiler {

    public enum Stage {
        VERTEX(shaderc_vertex_shader),
        FRAGMENT(shaderc_fragment_shader),
        COMPUTE(shaderc_compute_shader);

        final int kind;

        Stage(int kind) {
            this.kind = kind;
        }
    }

    private ShaderCompiler() {
    }

    public static ByteBuffer compileToSpirv(String name, String glsl, Stage stage) {
        long compiler = shaderc_compiler_initialize();
        long options = shaderc_compile_options_initialize();
        if (compiler == 0L || options == 0L) {
            throw new IllegalStateException("shaderc no inicializo");
        }
        try {
            shaderc_compile_options_set_target_env(options, shaderc_target_env_vulkan,
                    shaderc_env_version_vulkan_1_3);
            shaderc_compile_options_set_optimization_level(options, shaderc_optimization_level_performance);

            long result = shaderc_compile_into_spv(compiler, glsl, stage.kind, name, "main", options);
            try {
                if (shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
                    throw new RuntimeException("Error compilando " + name + ":\n"
                            + shaderc_result_get_error_message(result));
                }
                ByteBuffer spirv = shaderc_result_get_bytes(result);
                if (spirv == null) {
                    throw new IllegalStateException("shaderc devolvio SPIR-V vacio para " + name);
                }
                ByteBuffer copy = MemoryUtil.memAlloc(spirv.remaining());
                copy.put(spirv).flip();
                return copy;
            } finally {
                shaderc_result_release(result);
            }
        } finally {
            shaderc_compile_options_release(options);
            shaderc_compiler_release(compiler);
        }
    }

    public static long createModule(VkDevice device, ByteBuffer spirv) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack)
                    .sType$Default()
                    .pCode(spirv);
            LongBuffer pModule = stack.mallocLong(1);
            int err = vkCreateShaderModule(device, createInfo, null, pModule);
            if (err != VK_SUCCESS) {
                throw new RuntimeException("vkCreateShaderModule fallo con VkResult " + err);
            }
            return pModule.get(0);
        }
    }

    public static String loadSource(String resourcePath) {
        try (InputStream in = ShaderCompiler.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Recurso no encontrado: " + resourcePath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
