package com.jumpresethud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HudConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("jumpresethud.json");

    public int posX = 10;
    public int posY = 10;
    public int offsetX = 0;
    public int offsetY = 0;
    public float scale = 1.0f;
    public boolean enabled = true;

    public static HudConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                HudConfig config = GSON.fromJson(json, HudConfig.class);
                return config != null ? config : new HudConfig();
            } catch (IOException ignored) {
            }
        }
        HudConfig config = new HudConfig();
        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException ignored) {
        }
    }

    public int finalX() {
        return posX + offsetX;
    }

    public int finalY() {
        return posY + offsetY;
    }
}
