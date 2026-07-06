package com.vke.client.core;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceVulkan13Features;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;

public final class DeviceSelector {

    public record Selection(VkPhysicalDevice physicalDevice,
                            String deviceName,
                            int graphicsQueueFamily,
                            int presentQueueFamily,
                            boolean uma,
                            int umaMemoryTypeIndex) {
    }

    private DeviceSelector() {
    }

    public static Selection pick(VkInstance instance, long surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.mallocInt(1);
            vkEnumeratePhysicalDevices(instance, count, null);
            if (count.get(0) == 0) {
                throw new IllegalStateException("Sin dispositivos Vulkan disponibles");
            }

            PointerBuffer devices = stack.mallocPointer(count.get(0));
            vkEnumeratePhysicalDevices(instance, count, devices);

            Selection best = null;
            long bestScore = Long.MIN_VALUE;
            for (int i = 0; i < count.get(0); i++) {
                VkPhysicalDevice dev = new VkPhysicalDevice(devices.get(i), instance);
                Selection s = evaluate(dev, surface, stack);
                if (s == null) {
                    continue;
                }
                long score = score(dev, s, stack);
                if (score > bestScore) {
                    bestScore = score;
                    best = s;
                }
            }
            if (best == null) {
                throw new IllegalStateException("Ningun dispositivo cumple graphics, present y VK_KHR_swapchain");
            }
            return best;
        }
    }

    private static Selection evaluate(VkPhysicalDevice dev, long surface, MemoryStack stack) {
        if (!supportsSwapchain(dev)) {
            return null;
        }

        IntBuffer qCount = stack.mallocInt(1);
        vkGetPhysicalDeviceQueueFamilyProperties(dev, qCount, null);
        VkQueueFamilyProperties.Buffer families = VkQueueFamilyProperties.malloc(qCount.get(0), stack);
        vkGetPhysicalDeviceQueueFamilyProperties(dev, qCount, families);

        int graphics = -1;
        int present = -1;
        IntBuffer supported = stack.mallocInt(1);
        for (int i = 0; i < families.capacity(); i++) {
            boolean hasGraphics = (families.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0;
            vkGetPhysicalDeviceSurfaceSupportKHR(dev, i, surface, supported);
            boolean canPresent = supported.get(0) == VK_TRUE;

            if (hasGraphics && canPresent) {
                graphics = i;
                present = i;
                break;
            }
            if (hasGraphics && graphics == -1) {
                graphics = i;
            }
            if (canPresent && present == -1) {
                present = i;
            }
        }
        if (graphics == -1 || present == -1) {
            return null;
        }

        VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.malloc(stack);
        vkGetPhysicalDeviceProperties(dev, props);
        VkPhysicalDeviceMemoryProperties mem = VkPhysicalDeviceMemoryProperties.malloc(stack);
        vkGetPhysicalDeviceMemoryProperties(dev, mem);

        boolean integrated = props.deviceType() == VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU;
        int umaType = -1;
        int wanted = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
                | VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
        for (int i = 0; i < mem.memoryTypeCount(); i++) {
            if ((mem.memoryTypes(i).propertyFlags() & wanted) == wanted) {
                umaType = i;
                break;
            }
        }

        return new Selection(dev, props.deviceNameString(), graphics, present,
                integrated || umaType != -1, umaType);
    }

    private static long score(VkPhysicalDevice dev, Selection s, MemoryStack stack) {
        VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.malloc(stack);
        vkGetPhysicalDeviceProperties(dev, props);

        long score = 0;
        if (props.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
            score += 800;
        }
        if (props.deviceType() == VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU) {
            score += 400;
        }
        if (s.uma()) {
            score += 600;
        }
        if (s.graphicsQueueFamily() == s.presentQueueFamily()) {
            score += 50;
        }
        return score;
    }

    private static boolean supportsSwapchain(VkPhysicalDevice dev) {
        int[] count = new int[1];
        vkEnumerateDeviceExtensionProperties(dev, (ByteBuffer) null, count, null);
        VkExtensionProperties.Buffer exts = VkExtensionProperties.malloc(count[0]);
        try {
            vkEnumerateDeviceExtensionProperties(dev, (ByteBuffer) null, count, exts);
            for (VkExtensionProperties e : exts) {
                if (VK_KHR_SWAPCHAIN_EXTENSION_NAME.equals(e.extensionNameString())) {
                    return true;
                }
            }
            return false;
        } finally {
            exts.free();
        }
    }

    public static int findMemoryType(VkPhysicalDevice dev, int typeFilterBits, boolean umaDirect) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceMemoryProperties mem = VkPhysicalDeviceMemoryProperties.malloc(stack);
            vkGetPhysicalDeviceMemoryProperties(dev, mem);

            int dlhvhc = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
                    | VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                    | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
            int hvhc = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
            int dl = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;

            int[] preference = umaDirect ? new int[]{dlhvhc, hvhc, dl} : new int[]{dl, hvhc};

            for (int flags : preference) {
                for (int i = 0; i < mem.memoryTypeCount(); i++) {
                    if ((typeFilterBits & (1 << i)) != 0
                            && (mem.memoryTypes(i).propertyFlags() & flags) == flags) {
                        return i;
                    }
                }
            }
            throw new IllegalStateException("Sin memory type compatible con el filtro " + typeFilterBits);
        }
    }

    public static VkDevice createLogicalDevice(Selection sel) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int[] uniqueFamilies = sel.graphicsQueueFamily() == sel.presentQueueFamily()
                    ? new int[]{sel.graphicsQueueFamily()}
                    : new int[]{sel.graphicsQueueFamily(), sel.presentQueueFamily()};

            VkDeviceQueueCreateInfo.Buffer queueInfos =
                    VkDeviceQueueCreateInfo.calloc(uniqueFamilies.length, stack);
            for (int i = 0; i < uniqueFamilies.length; i++) {
                queueInfos.get(i).sType$Default()
                        .queueFamilyIndex(uniqueFamilies[i])
                        .pQueuePriorities(stack.floats(1.0f));
            }

            VkPhysicalDeviceVulkan13Features vk13 = VkPhysicalDeviceVulkan13Features.calloc(stack)
                    .sType$Default()
                    .dynamicRendering(true)
                    .synchronization2(true);
            VkPhysicalDeviceFeatures2 features2 = VkPhysicalDeviceFeatures2.calloc(stack)
                    .sType$Default()
                    .pNext(vk13.address());

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack)
                    .sType$Default()
                    .pNext(features2.address())
                    .pQueueCreateInfos(queueInfos)
                    .ppEnabledExtensionNames(stack.pointers(stack.UTF8(VK_KHR_SWAPCHAIN_EXTENSION_NAME)));

            PointerBuffer pDevice = stack.mallocPointer(1);
            VulkanContext.check(vkCreateDevice(sel.physicalDevice(), createInfo, null, pDevice),
                    "vkCreateDevice");
            return new VkDevice(pDevice.get(0), sel.physicalDevice(), createInfo);
        }
    }
}
