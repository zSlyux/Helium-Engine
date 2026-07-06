package com.vke.client.shader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.lwjgl.vulkan.VK10.vkDestroyShaderModule;

public final class ShaderPackLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger("VKE");
    private static final Map<String, ShaderProgram> PROGRAMS = new HashMap<>();

    private ShaderPackLoader() {
    }

    public static void loadAll(VkDevice device) {
        Path dir = FabricLoader.getInstance().getGameDir().resolve("shaderpacks");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.error("[VKE] No se pudo crear {}", dir, e);
            return;
        }

        try (DirectoryStream<Path> zips = Files.newDirectoryStream(dir, "*.zip")) {
            for (Path zip : zips) {
                loadPack(device, zip);
            }
        } catch (IOException e) {
            LOGGER.error("[VKE] Error listando shaderpacks", e);
        }

        if (PROGRAMS.isEmpty()) {
            LOGGER.info("[VKE] Sin shader packs en {}", dir);
        }
    }

    private static void loadPack(VkDevice device, Path zipPath) {
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            ZipEntry manifest = zip.getEntry("pack.json");
            if (manifest == null) {
                LOGGER.warn("[VKE] {} no contiene pack.json, ignorado", zipPath.getFileName());
                return;
            }

            JsonObject root = new Gson().fromJson(read(zip, manifest), JsonObject.class);
            String packName = root.has("name")
                    ? root.get("name").getAsString()
                    : zipPath.getFileName().toString();
            JsonObject programs = root.getAsJsonObject("programs");
            if (programs == null) {
                LOGGER.warn("[VKE] {} no define programas", packName);
                return;
            }

            int loaded = 0;
            for (Map.Entry<String, JsonElement> entry : programs.entrySet()) {
                JsonObject program = entry.getValue().getAsJsonObject();
                byte[] vert = compile(device, zip, program.get("vertex").getAsString(),
                        ShaderCompiler.Stage.VERTEX);
                byte[] frag = compile(device, zip, program.get("fragment").getAsString(),
                        ShaderCompiler.Stage.FRAGMENT);
                PROGRAMS.put(entry.getKey(), new ShaderProgram(entry.getKey(), vert, frag));
                loaded++;
            }
            LOGGER.info("[VKE] Pack '{}' cargado: {} programas compilados a SPIR-V", packName, loaded);
        } catch (Exception e) {
            LOGGER.error("[VKE] Fallo cargando {}", zipPath.getFileName(), e);
        }
    }

    private static byte[] compile(VkDevice device, ZipFile zip, String entryPath,
                                  ShaderCompiler.Stage stage) throws IOException {
        ZipEntry entry = zip.getEntry(entryPath);
        if (entry == null) {
            throw new IOException("Falta " + entryPath + " dentro del pack");
        }
        String glsl = read(zip, entry);
        ByteBuffer spirv = ShaderCompiler.compileToSpirv(entryPath, glsl, stage);
        try {
            long module = ShaderCompiler.createModule(device, spirv);
            vkDestroyShaderModule(device, module, null);
            byte[] bytes = new byte[spirv.remaining()];
            spirv.get(bytes);
            return bytes;
        } finally {
            MemoryUtil.memFree(spirv);
        }
    }

    private static String read(ZipFile zip, ZipEntry entry) throws IOException {
        try (InputStream in = zip.getInputStream(entry)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static Map<String, ShaderProgram> programs() {
        return Collections.unmodifiableMap(PROGRAMS);
    }
}
