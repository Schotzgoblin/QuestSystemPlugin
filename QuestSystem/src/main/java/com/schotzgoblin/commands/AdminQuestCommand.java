package com.schotzgoblin.commands;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.QuestManager;
import com.schotzgoblin.main.QuestSystem;
import com.schotzgoblin.utils.edit.EditQuestsUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class AdminQuestCommand implements CommandExecutor, Listener {

    private final QuestSystem questSystem = QuestSystem.getInstance();
    private final DatabaseHandler databaseHandler;
    private final QuestManager questManager = QuestManager.getInstance();
    private final ConfigHandler configHandler = ConfigHandler.getInstance();

    public AdminQuestCommand() {
        this.databaseHandler = DatabaseHandler.getInstance();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Objects.requireNonNull(configHandler.getStringAsync("command.no-player").join()));
            return true;
        }
        if (args.length == 0) return true;
        if (!args[0].equalsIgnoreCase("manage") && !args[0].equalsIgnoreCase("m")) return true;
        if (!player.hasPermission("quest.manage.admin")) {
            player.sendMessage("You do not have permission to manage quests.");
            return true;
        }
        EditQuestsUtils.initInventory(player).thenAccept(v -> {
            if (EditQuestsUtils.allQuestsInventory.containsKey(player.getUniqueId())) {
                Bukkit.getScheduler().runTask(questSystem, () -> {
                    player.openInventory(EditQuestsUtils.allQuestsInventory.get(player.getUniqueId()));
                });
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });

        return true;
    }
}
