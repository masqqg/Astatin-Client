package dev.astatin.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("astatin/config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("astatin.json");

    private ConfigManager() {
    }

    public static ConfigData load() {
        try {
            if (Files.isRegularFile(PATH)) {
                String json = Files.readString(PATH);
                ConfigData data = GSON.fromJson(json, ConfigData.class);
                if (data != null) {
                    if (data.hud == null) {
                        data.hud = new java.util.ArrayList<>();
                    }
                    if (data.enabled == null) {
                        data.enabled = new java.util.LinkedHashMap<>();
                    }
                    if (data.numbers == null) {
                        data.numbers = new java.util.LinkedHashMap<>();
                    }
                    if (data.colors == null) {
                        data.colors = new java.util.LinkedHashMap<>();
                    }
                    return data;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Could not read {} — using defaults", PATH, e);
        }
        return new ConfigData();
    }

    public static void save(ConfigData data) {
        try {
            Files.createDirectories(PATH.getParent());
            Path temp = PATH.resolveSibling("astatin.json.tmp");
            Files.writeString(temp, GSON.toJson(data));
            Files.move(temp, PATH, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            LOGGER.warn("Could not write {}", PATH, e);
        }
    }
}
