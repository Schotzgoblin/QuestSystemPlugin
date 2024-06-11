package com.schotzgoblin.main;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Objects;


public class TrackPlayerQuestProgress implements Listener {
    private final QuestSystem questSystem;
    private final QuestManager questManager;
    private final DatabaseHandler databaseHandler;
    private final FileConfiguration config;

    public TrackPlayerQuestProgress(QuestSystem questSystem, QuestManager questManager, DatabaseHandler databaseHandler) {
        this.questSystem = questSystem;
        this.questManager = questManager;
        this.databaseHandler = databaseHandler;
        config = questSystem.getConfig();
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        var player = event.getPlayer();
        var playerQuests = databaseHandler.getPlayerQuests(player.getUniqueId(), "IN_PROGRESS");
        if (!playerQuests.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(questSystem, () -> {
                player.sendMessage(Component.text(Objects.requireNonNull(config.getString("quest-system.welcome-message"))).clickEvent(ClickEvent.callback(audience -> {
                    questManager.setupInventory(player, "IN_PROGRESS");
                })));
            }, 20L);
        }
        playerQuests.forEach(playerQuest -> {
            questManager.createAndShowBossBar(player, playerQuest.getQuest().getName(), Utils.calculateProgress(playerQuest));
        });
    }

    @EventHandler
    public void onPlayerChangedWorldEvent(PlayerChangedWorldEvent event) {
        var player = event.getPlayer();
        updatePlayerQuests(player);
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        var player = event.getPlayer();
        updatePlayerQuests(player);
        questManager.bossBars.remove(player.getUniqueId());
    }

    private void updatePlayerQuests(Player player) {
        var playerQuests = databaseHandler.getPlayerQuests(player.getUniqueId(), "IN_PROGRESS");
        playerQuests.forEach(playerQuest -> {
            if (playerQuest.getQuest().getObjective().getType().equalsIgnoreCase("MOVE")) {
                playerQuest.setProgress(Utils.convertLocationToString(player.getLocation()));
                databaseHandler.update(playerQuest);
            }
        });
    }

    @EventHandler
    public void onEntityDeathEvent(EntityDeathEvent event) {
        var entity = event.getEntity();
        var player = entity.getKiller();
        if (player == null) {
            return;
        }
        var playerQuests = databaseHandler.getPlayerQuests(player.getUniqueId(), "IN_PROGRESS");
        playerQuests.forEach(playerQuest -> {
            var quest = playerQuest.getQuest();
            if (quest.getObjective().getType().equalsIgnoreCase("KILL") && quest.getObjective().getValue().equalsIgnoreCase(entity.getType().name())) {
                playerQuest.setProgress((Float.parseFloat(playerQuest.getProgress()) + 1f) + "");
                databaseHandler.update(playerQuest);
                questManager.updateBossBar(player, playerQuest, Utils.calculateProgress(playerQuest));
            }
        });
    }

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        var player = event.getPlayer();
        var playerQuests = databaseHandler.getPlayerQuests(player.getUniqueId(), "IN_PROGRESS");
        playerQuests.forEach(playerQuest -> {
            var quest = playerQuest.getQuest();
            if (quest.getObjective().getType().equalsIgnoreCase("MOVE")) {
                var playerLocation = player.getLocation();
                playerQuest.setProgress(Utils.convertLocationToString(playerLocation));
                questManager.updateBossBar(player, playerQuest, Utils.calculateProgress(playerQuest));
            }
        });
    }

    @EventHandler
    public void onEntityPickupItemEvent(EntityPickupItemEvent event) {
        var entity = event.getItem();
        var player = event.getEntity();
        if (!player.getType().equals(EntityType.PLAYER)) {
            return;
        }
        var playerQuests = databaseHandler.getPlayerQuests(player.getUniqueId(), "IN_PROGRESS");
        playerQuests.forEach(playerQuest -> {
            var quest = playerQuest.getQuest();
            if (quest.getObjective().getType().equalsIgnoreCase("PICKUP") && quest.getObjective().getValue().equalsIgnoreCase(entity.getName())) {
                playerQuest.setProgress((Float.parseFloat(playerQuest.getProgress()) + entity.getItemStack().getAmount()) + "");
                databaseHandler.update(playerQuest);
                questManager.updateBossBar((Player) player, playerQuest, Utils.calculateProgress(playerQuest));
            }
        });
    }
}
