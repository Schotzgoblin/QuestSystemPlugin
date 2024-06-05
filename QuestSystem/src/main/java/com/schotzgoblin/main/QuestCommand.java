package com.schotzgoblin.main;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class QuestCommand implements CommandExecutor {

    private final QuestManager questManager;

    public QuestCommand(QuestManager questManager) {
        this.questManager = questManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /quest <subcommand>");
            return true;
        }

        // Handle subcommands (create, accept, complete, etc.)
        if (args[0].equalsIgnoreCase("create") && sender.hasPermission("quest.admin")) {
            // Create quest logic
        } else if (args[0].equalsIgnoreCase("accept")) {
            // Accept quest logic
        } else if (args[0].equalsIgnoreCase("status")) {
            // Display quest status
        }

        return true;
    }
}
