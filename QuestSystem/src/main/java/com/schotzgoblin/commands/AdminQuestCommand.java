package com.schotzgoblin.commands;

import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.QuestManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class AdminQuestCommand implements CommandExecutor, Listener {

    private final QuestManager questManager;
    private final DatabaseHandler databaseHandler;
    private final Inventory questCreationUI;

    public AdminQuestCommand() {
        this.questManager = QuestManager.getInstance();
        this.databaseHandler = DatabaseHandler.getInstance();
        this.questCreationUI = Bukkit.createInventory(null, 27, Component.text("Quest Creation"));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if(!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }
        if (args[0].equalsIgnoreCase("create") && sender.hasPermission("quest.admin")) {

        }else if(args[0].equalsIgnoreCase("delete") && sender.hasPermission("quest.admin")) {
            // Create quest logic
        } else {
            sender.sendMessage("Unknown subcommand: " + args[0]);
        }

        return true;
    }
}
