package com.jumpresethud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("jump-reset-hud.json");

    private ConfigManager() {
    }

    public static HudConfig load() {
        if (Files.notExists(PATH)) {
            return new HudConfig();
        }

        try {
            String json = Files.readString(PATH);
            HudConfig config = GSON.fromJson(json, HudConfig.class);
            return config == null ? new HudConfig() : config;
        } catch (IOException e) {
            return new HudConfig();
        }
    }

    public static void save(HudConfig config) {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(config));
        } catch (IOException ignored) {
        }
    }
}
