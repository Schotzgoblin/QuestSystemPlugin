package com.schotzgoblin.commands;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.main.QuestManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class QuestCommand implements CommandExecutor {

    private final QuestManager questManager;
    private final ConfigHandler config = ConfigHandler.getInstance();

    public QuestCommand() {
        this.questManager = QuestManager.getInstance();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if(!(sender instanceof Player player)) {
            sender.sendMessage(Objects.requireNonNull(config.getStringAsync("command.no-player").join()));
            return true;
        }
        if (args.length == 0) {

            var allQuests = config.getStringAsync("quest-manager.quest.all").join();
            questManager.setupInventory(player, allQuests);
            return true;
        }

        // Handle subcommands (create, accept, complete, etc.)


        return true;
    }
}
