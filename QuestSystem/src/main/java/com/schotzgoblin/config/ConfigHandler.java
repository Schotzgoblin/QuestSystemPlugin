package com.schotzgoblin.config;

import com.schotzgoblin.main.QuestSystem;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.FileHandler;

public class ConfigHandler extends FileHandler {
    private final FileConfiguration config;
    private static ConfigHandler instance;

    public ConfigHandler() throws IOException, SecurityException {
        super("config.yml");
        config = QuestSystem.getInstance().getConfig();
    }

    public static ConfigHandler getInstance() {
        if(instance == null){
            try {
                instance = new ConfigHandler();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return instance;
    }

    public CompletableFuture<String> getStringAsync(String key) {
        return CompletableFuture.supplyAsync(() -> config.getString(key));
    }

    public CompletableFuture<Integer> getIntAsync(String key) {
        return CompletableFuture.supplyAsync(() -> config.getInt(key));
    }

    public CompletableFuture<Boolean> getBooleanAsync(String key) {
        return CompletableFuture.supplyAsync(() -> config.getBoolean(key));
    }

    public CompletableFuture<Double> getDoubleAsync(String key) {
        return CompletableFuture.supplyAsync(() -> config.getDouble(key));
    }

    public CompletableFuture<Long> getLongAsync(String key) {
        return CompletableFuture.supplyAsync(() -> config.getLong(key));
    }

    public CompletableFuture<Void> setAsync(String key, Object value) {
        return CompletableFuture.runAsync(() -> config.set(key, value));
    }

    public CompletableFuture<Void> saveAsync() {
        return CompletableFuture.runAsync(() -> QuestSystem.getInstance().saveConfig());
    }
}
