# VKE Shader Engine

A Fabric client-side mod for Minecraft 26.2 that replaces the graphics context with Vulkan and loads SPIR-V shader packs.

## Building the .jar

Requirements: JDK 25 installed.

```
gradlew build          (Windows)
./gradlew build        (Linux / macOS)
```

The JAR file is located at `build/libs/vke-engine-0.1.0.jar`.

## Installation

1. Copy `vke-engine-0.1.0.jar` to the game's `mods/` folder (requires Fabric Loader 0.19.3+).
2. Copy the shader pack `.zip` file to the `shaderpacks/` folder (this folder is created automatically upon the first launch).

## Optional JVM Flags

- `-Dvke.disable=true` disables the engine without uninstalling the mod.
- `-Dvke.force=true` forces the engine to run even if another renderer is installed.
- `-Dvke.validation=true` enables `VK_LAYER_KHRONOS_validation` (requires Vulkan SDK).

## Notes

- If Sodium, Iris, or VulkanMod is detected, the engine automatically disables itself, and the game launches using its standard pipeline.
- The `lwjgl_version` in `gradle.properties` must match the LWJGL version bundled with the game (visible in the version JSON within the launcher).
- If a mixin target does not exist in your specific game build, check the names using `-Dmixin.debug.verbose=true`.
- Current engine status: `GLFW_NO_API` window, `VkInstance`, `VkSurfaceKHR`, device selection with UMA detection, `VkDevice` with `dynamicRendering` and `synchronization2`, GLSL-to-SPIR-V compilation, and shader pack loading. The swapchain and drawing pipelines are the next milestone.
