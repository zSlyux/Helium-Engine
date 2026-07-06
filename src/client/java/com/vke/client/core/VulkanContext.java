package com.vke.client.core;

import com.vke.client.shader.ShaderPackLoader;
import com.vke.client.shader.ShaderSelfTest;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3;

public final class VulkanContext {

    private static final Logger LOGGER = LoggerFactory.getLogger("VKE");
    private static final boolean VALIDATION = Boolean.getBoolean("vke.validation");

    private static VkInstance instance;
    private static long surface = VK_NULL_HANDLE;
    private static long windowHandle;
    private static DeviceSelector.Selection gpu;
    private static VkDevice device;
    private static VkQueue graphicsQueue;
    private static VkQueue presentQueue;
    private static volatile boolean vsync = true;

    private VulkanContext() {
    }

    public static void init(long glfwWindow) {
        windowHandle = glfwWindow;

        if (!GLFWVulkan.glfwVulkanSupported()) {
            throw new IllegalStateException("No hay loader ni driver Vulkan disponible");
        }

        createInstance();
        createSurface();

        gpu = DeviceSelector.pick(instance, surface);
        LOGGER.info("[VKE] GPU: {} | UMA: {} | graphics: {} | present: {}",
                gpu.deviceName(), gpu.uma(), gpu.graphicsQueueFamily(), gpu.presentQueueFamily());

        device = DeviceSelector.createLogicalDevice(gpu);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pQueue = stack.mallocPointer(1);
            vkGetDeviceQueue(device, gpu.graphicsQueueFamily(), 0, pQueue);
            graphicsQueue = new VkQueue(pQueue.get(0), device);
            vkGetDeviceQueue(device, gpu.presentQueueFamily(), 0, pQueue);
            presentQueue = new VkQueue(pQueue.get(0), device);
        }

        ShaderSelfTest.run(device);
        ShaderPackLoader.loadAll(device);
    }

    private static void createInstance() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                    .sType$Default()
                    .pApplicationName(stack.UTF8("Minecraft"))
                    .applicationVersion(VK_MAKE_VERSION(1, 0, 0))
                    .pEngineName(stack.UTF8("VKE"))
                    .engineVersion(VK_MAKE_VERSION(0, 1, 0))
                    .apiVersion(VK_API_VERSION_1_3);

            PointerBuffer glfwExt = GLFWVulkan.glfwGetRequiredInstanceExtensions();
            if (glfwExt == null) {
                throw new IllegalStateException("GLFW no reporta extensiones de instancia Vulkan");
            }

            PointerBuffer extensions = glfwExt;
            PointerBuffer layers = null;
            if (VALIDATION) {
                extensions = stack.mallocPointer(glfwExt.remaining() + 1);
                extensions.put(glfwExt)
                        .put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME))
                        .flip();
                layers = stack.pointers(stack.UTF8("VK_LAYER_KHRONOS_validation"));
            }

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack)
                    .sType$Default()
                    .pApplicationInfo(appInfo)
                    .ppEnabledExtensionNames(extensions)
                    .ppEnabledLayerNames(layers);

            PointerBuffer pInstance = stack.mallocPointer(1);
            check(vkCreateInstance(createInfo, null, pInstance), "vkCreateInstance");
            instance = new VkInstance(pInstance.get(0), createInfo);
        }
    }

    private static void createSurface() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pSurface = stack.mallocLong(1);
            check(GLFWVulkan.glfwCreateWindowSurface(instance, windowHandle, null, pSurface),
                    "glfwCreateWindowSurface");
            surface = pSurface.get(0);
        }
    }

    public static void requestVsync(boolean on) {
        vsync = on;
    }

    public static void presentFrame() {
    }

    public static void check(int result, String op) {
        if (result != VK_SUCCESS) {
            throw new RuntimeException(op + " fallo con VkResult " + result);
        }
    }

    public static VkInstance instance() {
        return instance;
    }

    public static long surface() {
        return surface;
    }

    public static VkDevice device() {
        return device;
    }

    public static VkQueue graphicsQueue() {
        return graphicsQueue;
    }

    public static VkQueue presentQueue() {
        return presentQueue;
    }

    public static boolean vsyncRequested() {
        return vsync;
    }

    public static DeviceSelector.Selection gpu() {
        return gpu;
    }
}
