package com.schotzgoblin.main;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

public class QuestCommand implements CommandExecutor {

    private final QuestManager questManager;
    private final FileConfiguration config;

    public QuestCommand(QuestManager questManager) {
        this.questManager = questManager;
        config = questManager.plugin.getConfig();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if(!(sender instanceof Player player)) {
            sender.sendMessage(Objects.requireNonNull(config.getString("command.no-player")));
            return true;
        }
        if (args.length == 0) {
            questManager.setupInventory(player, "All Quests");
            return true;
        }

        // Handle subcommands (create, accept, complete, etc.)


        return true;
    }
}
