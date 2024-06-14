package com.schotzgoblin.main;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.database.PlayerQuest;
import com.schotzgoblin.database.Quest;
import com.schotzgoblin.database.Reward;
import com.schotzgoblin.dtos.InventoryMapping;
import com.schotzgoblin.utils.MessageUtils;
import com.schotzgoblin.utils.PlayerMoveUtils;
import com.schotzgoblin.utils.Utils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.schotzgoblin.utils.PlayerMoveUtils.playerQuestConfig;
import static com.schotzgoblin.utils.Utils.getTimeStringFromSecs;

public class QuestManager implements Listener {
    public final QuestSystem plugin;
    private static QuestManager instance;
    private final DatabaseHandler databaseHandler;
    public Map<UUID, InventoryMapping> inventories = Collections.synchronizedMap(new HashMap<>());
    public Map<UUID, List<BossBar>> bossBars = Collections.synchronizedMap(new HashMap<>());
    private final Map<PlayerQuest, BukkitRunnable> runnables = Collections.synchronizedMap(new HashMap<>());
    private final ConfigHandler configHandler = ConfigHandler.getInstance();

    public QuestManager() {
        this.plugin = QuestSystem.getInstance();
        this.databaseHandler = DatabaseHandler.getInstance();
    }

    public static QuestManager getInstance() {
        if (instance == null) {
            instance = new QuestManager();
        }
        return instance;
    }

    public CompletableFuture<Void> initInventory(Player player, String type) {
        Inventory inventory = createInventory();
        ItemStack blueGlassPane = createColoredGlassPane(Material.BLUE_STAINED_GLASS_PANE, "All Quests");
        ItemStack whiteGlassPane = createColoredGlassPane(Material.WHITE_STAINED_GLASS_PANE, "NOT_STARTED");
        ItemStack redGlassPane = createColoredGlassPane(Material.RED_STAINED_GLASS_PANE, "CANCELED");
        ItemStack orangeGlassPane = createColoredGlassPane(Material.ORANGE_STAINED_GLASS_PANE, "IN_PROGRESS");
        ItemStack greenGlassPane = createColoredGlassPane(Material.GREEN_STAINED_GLASS_PANE, "COMPLETED");

        inventory.setItem(2, blueGlassPane);
        inventory.setItem(3, whiteGlassPane);
        inventory.setItem(4, orangeGlassPane);
        inventory.setItem(5, greenGlassPane);
        inventory.setItem(6, redGlassPane);
        var addQuestsToInventoryFuture = addQuestsToInventory(type, player, inventory);
        return addQuestsToInventoryFuture.thenCompose(inventoryMapping -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!inventories.containsKey(player.getUniqueId()))
                    inventories.put(player.getUniqueId(), inventoryMapping);
            });
            return CompletableFuture.completedFuture(null);
        });
    }

    private Inventory createInventory() {
        return Bukkit.createInventory(null, 54, Component.text("Quests", TextColor.color(0, 170, 255)));
    }

    private ItemStack createColoredGlassPane(Material material, String displayName) {
        ItemStack glassPane = new ItemStack(material, 1);
        ItemMeta glassPaneMeta = glassPane.getItemMeta();
        glassPaneMeta.displayName(Component.text(displayName));
        glassPane.setItemMeta(glassPaneMeta);
        return glassPane;
    }

    public CompletableFuture<InventoryMapping> addQuestsToInventory(String type, Player player, Inventory inventory) {

        CompletableFuture<List<Quest>> questsFuture = databaseHandler.getAllQuestsAsync();
        CompletableFuture<List<PlayerQuest>> playerQuestsFuture = databaseHandler.getPlayerQuestsAsync(player.getUniqueId(), type);
        return CompletableFuture.allOf(questsFuture, playerQuestsFuture).thenCompose(v -> {
            var questsConfig = questsFuture.join();
            var playerQuests = playerQuestsFuture.join();
            var inventoryMappingConfig = new InventoryMapping();
            if (inventories.containsKey(player.getUniqueId())) {
                inventoryMappingConfig = inventories.get(player.getUniqueId());
            } else {
                inventoryMappingConfig = new InventoryMapping(inventory, type, getMenuSlotFromType(type));
            }
            var inventoryMapping = inventoryMappingConfig;
            inventoryMapping.setType(type);
            resetInventory(inventory);
            if (!type.equals("All Quests")) {
                if (type.equals("NOT_STARTED")) {
                    questsConfig = questsConfig.stream()
                            .filter(quest -> !playerQuests.stream().map(PlayerQuest::getQuestId).toList().contains(quest.getId())).toList();
                } else {
                    questsConfig = playerQuests.stream()
                            .map(PlayerQuest::getQuest)
                            .toList();
                }
            }
            var quests = questsConfig;
            inventory.setItem(inventoryMapping.getMenuSlot() + 9, new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
            for (int i = 0; i < quests.size(); i++) {
                Quest quest = quests.get(i);
                var itemStack = createQuestItemStackAsync(quest, type, player).join();
                inventory.setItem(i + 27, itemStack);


            }
            finaliseInventory(inventory);
            return CompletableFuture.completedFuture(inventoryMapping);
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    private int getMenuSlotFromType(String type) {
        return switch (type) {
            case "NOT_STARTED" -> 3;
            case "IN_PROGRESS" -> 4;
            case "COMPLETED" -> 5;
            case "CANCELED" -> 6;
            default -> 2;
        };
    }

    private CompletableFuture<ItemStack> createQuestItemStackAsync(Quest quest, String type, Player player) {
        List<Component> lore = new ArrayList<>();

        CompletableFuture<TextComponent> questStatusFuture = getQuestStatusComponentAsync(type, quest, player);
        CompletableFuture<PlayerQuest> playerQuestFuture = databaseHandler.getPlayerQuestByQuestIdAsync(player.getUniqueId(), quest.getId());
        CompletableFuture<List<Reward>> rewardsFuture = databaseHandler.getQuestRewardsAsync(quest);
        CompletableFuture<String> notStartedMsgFuture = configHandler.getStringAsync("quest-manager.quest-click-not-started");
        CompletableFuture<String> inProgressMsgFuture = configHandler.getStringAsync("quest-manager.quest-click-in-progress");
        CompletableFuture<String> completedMsgFuture = configHandler.getStringAsync("quest-manager.quest-click-completed");
        CompletableFuture<String> canceledMsgFuture = configHandler.getStringAsync("quest-manager.quest-click-canceled");
        CompletableFuture<String> mainQuestColourHexString = configHandler.getStringAsync("quest-manager.quest-main-colour");

        return CompletableFuture.allOf(questStatusFuture, rewardsFuture, playerQuestFuture, notStartedMsgFuture,
                inProgressMsgFuture,
                completedMsgFuture,
                canceledMsgFuture, mainQuestColourHexString).thenCompose(v -> {
            try {
                ItemStack itemStack = new ItemStack(Material.PAPER);
                var questStatusComponent = questStatusFuture.get();
                var playerQuest = playerQuestFuture.get();
                var rewards = rewardsFuture.get();

                String notStartedMsg = notStartedMsgFuture.get();
                String inProgressMsg = inProgressMsgFuture.get();
                String completedMsg = completedMsgFuture.get();
                String canceledMsg = canceledMsgFuture.get();
                String mainQuestColorHexString = mainQuestColourHexString.get();
                var mainQuestColor = TextColor.fromCSSHexString(mainQuestColorHexString);
                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.displayName(Component.text(quest.getName(), mainQuestColor));

                String statusMsg = switch (questStatusComponent.content()) {
                    case "Not Started" -> notStartedMsg;
                    case "In Progress" -> inProgressMsg;
                    case "Completed" -> completedMsg;
                    case "Canceled" -> canceledMsg;
                    default -> "";
                };
                lore.add(Component.text(statusMsg));
                lore.add(Component.text(""));
                lore.add(Component.text("Status:", mainQuestColor));
                lore.add(questStatusComponent);
                lore.add(Component.text(""));
                lore.add(Component.text("Description:", mainQuestColor));
                lore.add(Component.text(quest.getDescription()));
                lore.add(Component.text(""));
                lore.add(Component.text("Objective:", mainQuestColor));
                lore.add(Component.text(quest.getObjective().getObjective()));
                lore.add(Component.text(""));
                lore.add(Component.text("Time limit:", mainQuestColor));
                lore.add(Component.text(Utils.getTimeStringFromSecs(quest.getTimeLimit())));
                if (questStatusComponent.content().equals("In Progress")) {
                    lore.add(Component.text(""));
                    lore.add(Component.text("Time left:", mainQuestColor));
                    lore.add(Component.text(Utils.getTimeStringFromSecs(quest.getTimeLimit() - playerQuest.getTime())));
                }
                lore.add(Component.text(""));
                lore.add(Component.text("Rewards: ", mainQuestColor));
                for (Reward reward : rewards) {
                    lore.add(Component.text(reward.getName(), TextColor.color(0, 255, 0)));
                }
                itemMeta.lore(lore);
                itemStack.setItemMeta(itemMeta);
                return CompletableFuture.completedFuture(itemStack);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return CompletableFuture.completedFuture(new ItemStack(Material.AIR));
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    private void finaliseInventory(Inventory inventory) {
        if (inventory == null) return;
        for (int i = 0; i < 54; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
            }
        }
    }

    private void resetInventory(Inventory inventory) {
        if (inventory == null) return;
        for (int i = 9; i < 54; i++) {
            inventory.setItem(i, new ItemStack(Material.AIR));
        }
    }

    private CompletableFuture<TextComponent> getQuestStatusComponentAsync(String type, Quest quest, Player player) {
        if (!type.equals("All Quests")) {
            return CompletableFuture.supplyAsync(() -> getComponentFromType(type));
        } else {
            CompletableFuture<List<PlayerQuest>> allPlayerQuestsFuture = databaseHandler.getPlayerQuestsAsync(player.getUniqueId(), "NOT_STARTED");

            return allPlayerQuestsFuture.thenApply(allPlayerQuests -> {
                PlayerQuest playerQuest = allPlayerQuests.stream()
                        .filter(playerQuest1 -> playerQuest1.getQuest().getId() == quest.getId())
                        .findFirst()
                        .orElse(null);
                if (playerQuest == null) return getComponentFromType("NOT_STARTED");
                return getComponentFromType(playerQuest.getQuestStatus().getStatus());
            }).exceptionally(ex -> {
                ex.printStackTrace();
                return Component.text("something went wrong");
            });
        }
    }


    private TextComponent getComponentFromType(String type) {
        return switch (type) {
            case "CANCELED" -> Component.text("Canceled", TextColor.color(255, 0, 0)); // Red
            case "IN_PROGRESS" -> Component.text("In Progress", TextColor.color(255, 165, 0)); // Orange
            case "COMPLETED" -> Component.text("Completed", TextColor.color(0, 255, 0)); // Green
            default -> Component.text("Not Started", TextColor.color(100, 255, 255)); // White
        };
    }

    public CompletableFuture<Void> reactivateQuest(Player player, TextComponent displayname) {
        var questFuture = databaseHandler.getQuestByNameAsync(displayname.content());
        return questFuture.thenCompose(quest -> {
            var playerQuestFuture = databaseHandler.getPlayerQuestByQuestIdAsync(player.getUniqueId(), quest.getId());
            CompletableFuture<String> reactivationMessageFuture = configHandler.getStringAsync("quest-manager.quest-reactivated");
            var changePlayerQuestFuture =
                    databaseHandler.changePlayerQuestType(player.getUniqueId(), displayname.content(), "IN_PROGRESS", player.getLocation());
            return CompletableFuture.allOf(playerQuestFuture, reactivationMessageFuture, changePlayerQuestFuture).thenAccept(x -> {
                try {
                    var playerQuest = playerQuestFuture.get();
                    var reactivationMessage = reactivationMessageFuture.get();
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (PlayerMoveUtils.playerQuestConfig.containsKey(player.getUniqueId()))
                            playerQuestConfig.get(player.getUniqueId()).add(playerQuest);
                        else
                            playerQuestConfig.put(player.getUniqueId(), Collections.synchronizedList(new ArrayList<>(List.of(playerQuest))));
                        player.sendMessage(reactivationMessage + displayname.content());
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.0f);
                        createAndShowBossBar(player, displayname.content(), 0.0f);
                    });
                    if (runnables.containsKey(playerQuest)) {
                        runnables.get(playerQuest).cancel();
                        runnables.remove(playerQuest);
                    }
                    BukkitRunnable task = new QuestTimerTask(player, displayname.content(), quest.getId());
                    runnables.put(playerQuest, task);
                    task.runTaskTimer(plugin, 0L, 20L);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).exceptionally(ex -> {
                ex.printStackTrace();
                return null;
            });
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });


    }

    public CompletableFuture<Void> cancelQuest(Player player, TextComponent displayname) {
        var questFuture = databaseHandler.getQuestByNameAsync(displayname.content());
        return questFuture.thenAccept(quest -> {
            var playerQuestFuture = databaseHandler.getPlayerQuestByQuestIdAsync(player.getUniqueId(), quest.getId());
            CompletableFuture<String> cancellationMessageFuture = configHandler.getStringAsync("quest-manager.quest-canceled");
            var changePlayerQuestFuture =
                    databaseHandler.changePlayerQuestType(player.getUniqueId(), displayname.content(), "CANCELED", player.getLocation());
            CompletableFuture.allOf(playerQuestFuture, cancellationMessageFuture, changePlayerQuestFuture).thenAccept(x -> {
                try {
                    var playerQuest = playerQuestFuture.get();
                    var cancellationMessage = cancellationMessageFuture.get();
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (PlayerMoveUtils.playerQuestConfig.containsKey(player.getUniqueId()))
                            PlayerMoveUtils.playerQuestConfig.get(player.getUniqueId()).remove(playerQuest);
                        if (runnables.containsKey(playerQuest)) {
                            runnables.get(playerQuest).cancel();
                            runnables.remove(playerQuest);
                        }
                        player.sendMessage(cancellationMessage + displayname.content());
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.0f);
                        hideBossBar(player, displayname.content());
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).exceptionally(ex -> {
                ex.printStackTrace();
                return null;
            });
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    public void acceptQuest(Player player, TextComponent displayname, String objective) {
        var questFuture = databaseHandler.getQuestByNameAsync(displayname.content());
        var addFuture = databaseHandler.addPlayerQuest(player.getUniqueId(), displayname.content(), player.getLocation());
        CompletableFuture.allOf(questFuture, addFuture).thenAccept(v -> {
            try {
                var quest = questFuture.get();

                CompletableFuture<String> acceptMessageFuture = configHandler.getStringAsync("quest-manager.quest-accepted");
                CompletableFuture<String> objectiveMessageFuture = configHandler.getStringAsync("quest-manager.quest-objective");
                var playerQuestFuture = databaseHandler.getPlayerQuestByQuestIdAsync(player.getUniqueId(), quest.getId());
                CompletableFuture.allOf(acceptMessageFuture, objectiveMessageFuture, playerQuestFuture).thenAccept(x -> {
                    try {
                        var playerQuest = playerQuestFuture.get();
                        var acceptMessage = acceptMessageFuture.get();
                        var objectiveMessage = objectiveMessageFuture.get();
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (PlayerMoveUtils.playerQuestConfig.containsKey(player.getUniqueId()))
                                playerQuestConfig.get(player.getUniqueId()).add(playerQuest);
                            else
                                playerQuestConfig.put(player.getUniqueId(), Collections.synchronizedList(new ArrayList<>(List.of(playerQuest))));
                            player.sendMessage(acceptMessage + displayname.content());
                            player.sendMessage(objectiveMessage + objective);
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.0f);
                            createAndShowBossBar(player, displayname.content(), 0.0f);
                        });
                        BukkitRunnable task = new QuestTimerTask(player, displayname.content(), quest.getId());
                        runnables.put(playerQuest, task);
                        task.runTaskTimer(plugin, 0L, 20L);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    public void createAndShowBossBar(Player player, String title, float progress) {
        var bossbar = BossBar.bossBar(Component.text(title), progress, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
        if (bossBars.containsKey(player.getUniqueId())) {
            bossBars.get(player.getUniqueId()).add(bossbar);
        } else
            bossBars.put(player.getUniqueId(), new ArrayList<>(List.of(bossbar)));
        player.showBossBar(bossbar);
    }

    private BossBar getBossBarFromTitle(Player player, String title) {
        if (!bossBars.containsKey(player.getUniqueId())) return null;
        return bossBars.get(player.getUniqueId()).stream().filter(bossBar -> ((TextComponent) bossBar.name()).content().equals(title)).findFirst().orElse(null);
    }

    public void updateBossBar(Player player, PlayerQuest playerQuest, float progress) {
        var title = playerQuest.getQuest().getName();
        var bossbar = getBossBarFromTitle(player, title);
        bossbar.progress(progress);
        if (progress == 1.0f) {
            bossbar.color(BossBar.Color.GREEN);
            completeQuest(player, title, playerQuest);
        }
    }

    private void completeQuest(Player player, String title, PlayerQuest playerQuest) {
        CompletableFuture<String> completedMessageFuture = configHandler.getStringAsync("quest-manager.quest-completed");
        var changePlayerQuestFuture =
                databaseHandler.changePlayerQuestType(player.getUniqueId(), title, "COMPLETED", player.getLocation());
        CompletableFuture.allOf(completedMessageFuture, changePlayerQuestFuture).thenAccept(x -> {
            try {
                var completedMessage = completedMessageFuture.get();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (PlayerMoveUtils.playerQuestConfig.containsKey(player.getUniqueId()))
                        PlayerMoveUtils.playerQuestConfig.get(player.getUniqueId()).remove(playerQuest);
                    player.sendMessage(completedMessage + title);
                    MessageUtils.sendAlertToPlayer("Quest Completed", completedMessage + title, 1000, 4000, 1000, Color.fromRGB(0, 255, 0), player);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    if (runnables.containsKey(playerQuest)) {
                        runnables.get(playerQuest).cancel();
                        runnables.remove(playerQuest);
                    }
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        hideBossBar(player, title);
                    }, 100);
                });
                giveRewards(player, playerQuest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    private void giveRewards(Player player, PlayerQuest playerQuest) {
        var quest = playerQuest.getQuest();
        var rewardsFuture = databaseHandler.getQuestRewardsAsync(quest);
        var rewardsMessageFuture = configHandler.getStringAsync("quest-manager.quest-rewards");
        CompletableFuture.allOf(rewardsFuture, rewardsMessageFuture).thenAccept(v -> {
            try {
                var rewards = rewardsFuture.get();
                var rewardsMessage = rewardsMessageFuture.get();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Reward reward : rewards) {
                        var rewardTypeName = reward.getRewardType().getName();
                        switch (rewardTypeName) {
                            case "XP" -> player.giveExp(reward.getAmount());
                            case "ITEM" -> {
                                ItemStack itemStack = new ItemStack(Objects.requireNonNull(Material.getMaterial(reward.getValue())), reward.getAmount());
                                player.getInventory().addItem(itemStack);
                            }
                        }
                        player.sendMessage(rewardsMessage + reward.getName());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });

    }

    public void hideBossBar(Player player, String title) {
        var bossBar = getBossBarFromTitle(player, title);
        if (bossBar == null) return;
        player.hideBossBar(bossBar);
        bossBars.get(player.getUniqueId()).remove(bossBar);
    }

    public CompletableFuture<Void> setupInventory(Player player, String type) {
        return initInventory(player, type).thenAccept(x -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory inventory = inventories.get(player.getUniqueId()).getInventory();
                player.openInventory(inventory);
                inventories.get(player.getUniqueId()).setInventory(inventory);
            });

        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent e) {
        var playerQuestsFuture = databaseHandler.getPlayerQuestsAsync(e.getPlayer().getUniqueId(), "IN_PROGRESS");
        playerQuestsFuture.thenAccept(playerQuests -> {
            if (playerQuests.isEmpty()) {
                initInventory(e.getPlayer(), "All Quests");
            } else {
                initInventory(e.getPlayer(), "IN_PROGRESS");
            }
            for (PlayerQuest playerQuest : playerQuests) {
                BukkitRunnable task = new QuestTimerTask(e.getPlayer(), playerQuest.getQuest().getName(), playerQuest.getQuest().getId());
                runnables.put(playerQuest, task);
                task.runTaskTimer(plugin, 0L, 20L);
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent e) {
        var playerQuestsFuture = databaseHandler.getPlayerQuestsAsync(e.getPlayer().getUniqueId(), "IN_PROGRESS");
        playerQuestsFuture.thenAccept(playerQuests -> {
            for (PlayerQuest playerQuest : playerQuests) {
                if (runnables.containsKey(playerQuest)) {
                    runnables.get(playerQuest).cancel();
                    runnables.remove(playerQuest);
                }
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    private class QuestTimerTask extends BukkitRunnable {
        private final Player player;
        private final String questName;
        private final int questId;

        public QuestTimerTask(Player player, String questName, int questId) {
            this.player = player;
            this.questName = questName;
            this.questId = questId;
        }

        @Override
        public void run() {
            var playerQuestFuture = databaseHandler.getPlayerQuestByQuestIdAsync(player.getUniqueId(), questId);
            var failedMessageFuture = configHandler.getStringAsync("quest-manager.quest-failed");
            var timeLimitMessageFuture = configHandler.getStringAsync("quest-manager.quest-time-limit");
            playerQuestFuture.thenAccept(playerQuest -> {
                try {
                    var copy = (PlayerQuest) playerQuest.clone();
                    if (copy.getQuestStatus().getStatus().equals("IN_PROGRESS")) {
                        copy.setTime(playerQuest.getTime() + 1);
                        if (playerQuestConfig.containsKey(player.getUniqueId())) {
                            var playerQuestsFromConfig = playerQuestConfig.get(player.getUniqueId());
                            if (!playerQuestsFromConfig.contains(playerQuest))
                                playerQuestsFromConfig.add(playerQuest);
                            else {
                                playerQuestsFromConfig.get(playerQuestsFromConfig.indexOf(playerQuest)).setTime(playerQuest.getTime());
                            }
                        }
                        databaseHandler.updateAsync(copy);
                    }
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }

            });
            CompletableFuture.allOf(playerQuestFuture, failedMessageFuture, timeLimitMessageFuture).thenAccept(v -> {
                try {
                    var playerQuest = playerQuestFuture.get();
                    var failedMessage = failedMessageFuture.get();
                    var timeLimitMessage = timeLimitMessageFuture.get();
                    var invConfig = new InventoryMapping();
                    if (inventories.containsKey(player.getUniqueId())) {
                        invConfig = inventories.get(player.getUniqueId());
                    }
                    var inv = invConfig;
                    if (playerQuest.getQuestStatus().getStatus().equals("IN_PROGRESS")) {
                        var questFuture = databaseHandler.getQuestByNameAsync(questName);
                        questFuture.thenAccept(x -> {
                            try {
                                var quest = questFuture.get();
                                if (playerQuest.getTime() >= quest.getTimeLimit()) {
                                    Bukkit.getScheduler().runTask(plugin, () -> {
                                        MessageUtils.sendAlertToPlayer("Quest Failed", failedMessage + questName + timeLimitMessage + getTimeStringFromSecs(quest.getTimeLimit()), 1000, 4000, 1000, Color.fromRGB(255, 0, 0), player);
                                        player.sendMessage(failedMessage + questName + timeLimitMessage + getTimeStringFromSecs(quest.getTimeLimit()));
                                        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 0.0f);
                                        cancel();
                                        runnables.remove(playerQuest);
                                        hideBossBar(player, questName);
                                    });
                                    if (PlayerMoveUtils.playerQuestConfig.containsKey(player.getUniqueId()))
                                        PlayerMoveUtils.playerQuestConfig.get(player.getUniqueId()).remove(playerQuest);
                                    databaseHandler.changePlayerQuestType(player.getUniqueId(), questName, "CANCELED", player.getLocation());
                                }
                                if (inv.getInventory() != null)
                                    addQuestsToInventory(inv.getType(), player, inv.getInventory());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).exceptionally(ex -> {
                ex.printStackTrace();
                return null;
            });

        }
    }
}
