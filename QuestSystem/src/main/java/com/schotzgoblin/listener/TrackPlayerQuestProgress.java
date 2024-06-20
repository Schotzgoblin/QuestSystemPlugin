package com.schotzgoblin.listener;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.database.PlayerQuest;
import com.schotzgoblin.dtos.PlayerQuestCoolDown;
import com.schotzgoblin.enums.ObjectiveType;
import com.schotzgoblin.enums.QuestStatus;
import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.QuestSystem;
import com.schotzgoblin.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
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

import java.util.*;

import static com.schotzgoblin.utils.PlayerMoveUtils.playerQuestConfig;


public class TrackPlayerQuestProgress implements Listener {
    private final QuestSystem questSystem;
    private final QuestManager questManager;
    private final DatabaseHandler databaseHandler;
    private final ConfigHandler config;
    private static final Map<UUID, List<PlayerQuestCoolDown>> bossBarUpdateTimestamps = new HashMap<>();

    public TrackPlayerQuestProgress() {
        this.questSystem = QuestSystem.getInstance();
        this.questManager = QuestManager.getInstance();
        this.databaseHandler = DatabaseHandler.getInstance();
        config = ConfigHandler.getInstance();
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        var player = event.getPlayer();
        var playerQuestsFuture = databaseHandler.getPlayerQuestsAsync(player.getUniqueId(), QuestStatus.IN_PROGRESS.name());
        var welcomeMessage = config.getStringAsync("quest-system.welcome.message").join();
        var welcomeMessageColour = config.getStringAsync("quest-system.welcome.colour").join();
        playerQuestsFuture.thenAccept(playerQuests -> {
            if (!playerQuests.isEmpty()) {
                Bukkit.getScheduler().runTaskLater(questSystem, () -> {
                    player.sendMessage(Component.text(
                            Objects.requireNonNull(welcomeMessage), TextColor.fromHexString(welcomeMessageColour))
                            .clickEvent(ClickEvent.callback(audience -> {
                        questManager.setupInventory(player, QuestStatus.IN_PROGRESS.name());
                    })));
                }, 20L);
            }
            playerQuests.forEach(playerQuest -> {
                questManager.createAndShowBossBar(player, playerQuest.getQuest().getName(), Utils.calculateProgress(playerQuest));
            });
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
        var playerQuestsFuture = databaseHandler.getPlayerQuestsAsync(player.getUniqueId(), QuestStatus.IN_PROGRESS.name());
        playerQuestsFuture.thenAccept(playerQuests -> {
            playerQuests.forEach(playerQuest -> {
                if (playerQuest.getQuest().getObjective().getType().equalsIgnoreCase(ObjectiveType.MOVE.name())) {
                    playerQuest.setProgress(Utils.convertLocationToString(player.getLocation()));
                    databaseHandler.updateAsync(playerQuest);
                }
            });
        });
    }

    @EventHandler
    public void onEntityDeathEvent(EntityDeathEvent event) {
        var entity = event.getEntity();
        var player = entity.getKiller();
        if (player == null) {
            return;
        }
        var playerQuestsFuture = databaseHandler.getPlayerQuestsAsync(player.getUniqueId(), QuestStatus.IN_PROGRESS.name());
        playerQuestsFuture.thenAccept(playerQuests -> {
            playerQuests.forEach(playerQuest -> {
                var quest = playerQuest.getQuest();
                if (quest.getObjective().getType().equalsIgnoreCase(ObjectiveType.KILL.name()) && quest.getObjective().getValue().equalsIgnoreCase(entity.getType().name())) {
                    playerQuest.setProgress((Float.parseFloat(playerQuest.getProgress()) + 1f) + "");
                    updatePlayerQuestsFromMemory(player, playerQuest);
                    databaseHandler.updateAsync(playerQuest).thenAccept(x ->
                            questManager.updateBossBar(player, playerQuest, Utils.calculateProgress(playerQuest)));
                }
            });
        });

    }

    private void updatePlayerQuestsFromMemory(Player player, PlayerQuest playerQuest) {
        if (playerQuestConfig.containsKey(player.getUniqueId())) {
            var playerQuestsFromConfig = playerQuestConfig.get(player.getUniqueId());
            if (!playerQuestsFromConfig.contains(playerQuest))
                playerQuestsFromConfig.add(playerQuest);
            else {
                playerQuestsFromConfig.get(playerQuestsFromConfig.indexOf(playerQuest)).setProgress(playerQuest.getProgress());
            }
        } else {
            playerQuestConfig.put(player.getUniqueId(), Collections.synchronizedList(new ArrayList<>(List.of(playerQuest))));
        }
    }

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        var player = event.getPlayer();
        var playerQuests = playerQuestConfig.get(player.getUniqueId());
        if(playerQuests==null) return;

        long currentTime = System.currentTimeMillis();
        long cooldownTime = 200;

        playerQuests.forEach(playerQuest -> {
            var quest = playerQuest.getQuest();
            if (quest.getObjective().getType().equalsIgnoreCase(ObjectiveType.MOVE.name())) {
                var playerLocation = player.getLocation();
                playerQuest.setProgress(Utils.convertLocationToString(playerLocation));
                var playerQuestCoolDowns = bossBarUpdateTimestamps.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());

                PlayerQuestCoolDown playerQuestCoolDown = playerQuestCoolDowns.stream()
                        .filter(pqc -> pqc.getPlayerQuest().equals(playerQuest))
                        .findFirst()
                        .orElseGet(() -> {
                            PlayerQuestCoolDown pqc = new PlayerQuestCoolDown(playerQuest, 0);
                            playerQuestCoolDowns.add(pqc);
                            return pqc;
                        });

                long lastUpdateTime = playerQuestCoolDown.getCoolDown();
                if (currentTime - lastUpdateTime >= cooldownTime) {
                    questManager.updateBossBar(player, playerQuest, Utils.calculateProgress(playerQuest));
                    playerQuestCoolDown.setCoolDown(currentTime);
                }
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
        var playerQuestsFuture = databaseHandler.getPlayerQuestsAsync(player.getUniqueId(), QuestStatus.IN_PROGRESS.name());
        playerQuestsFuture.thenAccept(playerQuests -> {
            playerQuests.forEach(playerQuest -> {
                var quest = playerQuest.getQuest();
                if (quest.getObjective().getType().equalsIgnoreCase(ObjectiveType.PICKUP.name()) && quest.getObjective().getValue().equalsIgnoreCase(entity.getName())) {
                    playerQuest.setProgress((Float.parseFloat(playerQuest.getProgress()) + entity.getItemStack().getAmount()) + "");
                    updatePlayerQuestsFromMemory((Player) player, playerQuest);
                    databaseHandler.updateAsync(playerQuest).thenAccept(x ->
                            questManager.updateBossBar((Player) player, playerQuest, Utils.calculateProgress(playerQuest)));
                }
            });
        });

    }
}
