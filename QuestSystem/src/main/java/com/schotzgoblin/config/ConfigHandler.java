package com.schotzgoblin.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
import java.util.logging.FileHandler;

public class ConfigHandler extends FileHandler {

    public ConfigHandler() throws IOException, SecurityException {
        super("config.yml");
    }
}
