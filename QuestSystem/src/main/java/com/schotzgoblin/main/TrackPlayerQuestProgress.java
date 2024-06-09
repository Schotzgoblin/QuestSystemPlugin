package com.schotzgoblin.main;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;


public class TrackPlayerQuestProgress implements Listener {
    private final QuestSystem questSystem;
    private final QuestManager questManager;
    private final DatabaseHandler databaseHandler;

    public TrackPlayerQuestProgress(QuestSystem questSystem, QuestManager questManager, DatabaseHandler databaseHandler) {
        this.questSystem = questSystem;
        this.questManager = questManager;
        this.databaseHandler = databaseHandler;
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        var player = event.getPlayer();
        var playerQuests = databaseHandler.getPlayerQuests(player.getUniqueId(), "IN_PROGRESS");
        playerQuests.forEach(playerQuest ->{
            questManager.createAndShowBossBar(player, playerQuest.getQuest().getName(), Utils.calculateProgress(playerQuest.getQuest().getObjective(),playerQuest.getProgress()));
        });
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        var player = event.getPlayer();
        questManager.bossBars.remove(player.getUniqueId());
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
                playerQuest.setProgress((Float.parseFloat(playerQuest.getProgress())+1f)+"");
                databaseHandler.update(playerQuest);
                questManager.updateBossBar(player, quest.getName(), Utils.calculateProgress(quest.getObjective(), playerQuest.getProgress()));
            }
        });
    }

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        // Handle player move event
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
                playerQuest.setProgress((Float.parseFloat(playerQuest.getProgress())+entity.getItemStack().getAmount())+"");
                databaseHandler.update(playerQuest);
                questManager.updateBossBar((Player)player, quest.getName(), Utils.calculateProgress(quest.getObjective(), playerQuest.getProgress()));
            }
        });
    }
}
