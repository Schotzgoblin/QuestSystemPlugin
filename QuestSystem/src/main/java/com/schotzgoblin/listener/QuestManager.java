package com.schotzgoblin.listener;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.database.PlayerQuest;
import com.schotzgoblin.database.Quest;
import com.schotzgoblin.database.Reward;
import com.schotzgoblin.dtos.InventoryMapping;
import com.schotzgoblin.enums.QuestStatus;
import com.schotzgoblin.enums.RewardType;
import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.QuestSystem;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.schotzgoblin.utils.PlayerMoveUtils.playerQuestConfig;
import static com.schotzgoblin.utils.Utils.getTimeStringFromSecs;

public class QuestManager implements Listener {

    private static final Logger logger = LoggerFactory.getLogger(QuestManager.class);
    public final QuestSystem plugin;
    private static QuestManager instance;
    private final DatabaseHandler databaseHandler;
    public Map<UUID, InventoryMapping> inventories = Collections.synchronizedMap(new HashMap<>());
    public Map<UUID, List<BossBar>> bossBars = Collections.synchronizedMap(new HashMap<>());
    private final Map<PlayerQuest, BukkitRunnable> runnable = Collections.synchronizedMap(new HashMap<>());
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
        Inventory inventory = createInventory().join();
        var allQuests = configHandler.getStringAsync("quest-manager.quest.all").join();
        ItemStack blueGlassPane = createColoredGlassPane(Material.BLUE_STAINED_GLASS_PANE, allQuests);
        ItemStack whiteGlassPane = createColoredGlassPane(Material.WHITE_STAINED_GLASS_PANE, QuestStatus.NOT_STARTED.name());
        ItemStack redGlassPane = createColoredGlassPane(Material.RED_STAINED_GLASS_PANE, QuestStatus.CANCELED.name());
        ItemStack orangeGlassPane = createColoredGlassPane(Material.ORANGE_STAINED_GLASS_PANE, QuestStatus.IN_PROGRESS.name());
        ItemStack greenGlassPane = createColoredGlassPane(Material.GREEN_STAINED_GLASS_PANE, QuestStatus.COMPLETED.name());

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

    private CompletableFuture<Inventory> createInventory() {
        var title = configHandler.getStringAsync("quest-inv.title");
        var colour = configHandler.getStringAsync("quest-inv.colour");
        return CompletableFuture.allOf(title, colour).thenApply(v -> {
            var titleString = title.join();
            var colourString = colour.join();
            return Bukkit.createInventory(null, 54, Component.text(titleString, TextColor.fromCSSHexString(colourString)));
        });
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
        var allQuestsFuture = configHandler.getStringAsync("quest-manager.quest.all");
        return CompletableFuture.allOf(allQuestsFuture, questsFuture, playerQuestsFuture).thenCompose(v -> {
            var questsConfig = questsFuture.join();
            var playerQuests = playerQuestsFuture.join();
            var allQuests = allQuestsFuture.join();
            var inventoryMappingConfig = new InventoryMapping();
            if (inventories.containsKey(player.getUniqueId())) {
                inventoryMappingConfig = inventories.get(player.getUniqueId());
            } else {
                inventoryMappingConfig = new InventoryMapping(inventory, type, getMenuSlotFromType(type));
            }
            var inventoryMapping = inventoryMappingConfig;
            inventoryMapping.setType(type);
            resetInventory(inventory);
            if (!type.equals(allQuests)) {
                if (type.equals(QuestStatus.NOT_STARTED.name())) {
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
            finaliseInventory(inventory,54);
            return CompletableFuture.completedFuture(inventoryMapping);
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(),ex);
            return null;
        });
    }

    private int getMenuSlotFromType(String type) {
        QuestStatus status;
        try {
            status = QuestStatus.valueOf(type);
        } catch (IllegalArgumentException e) {
            return 2;
        }

        return switch (status) {
            case NOT_STARTED -> 3;
            case IN_PROGRESS -> 4;
            case COMPLETED -> 5;
            case CANCELED -> 6;
        };
    }

    public CompletableFuture<ItemStack> createQuestItemStackAsync(Quest quest, String type, Player player) {
        List<Component> lore = new ArrayList<>();

        CompletableFuture<TextComponent> questStatusFuture = getQuestStatusComponentAsync(type, quest, player);
        CompletableFuture<PlayerQuest> playerQuestFuture = databaseHandler.getPlayerQuestByQuestIdAsync(player.getUniqueId(), quest.getId());
        CompletableFuture<List<Reward>> rewardsFuture = databaseHandler.getQuestRewardsAsync(quest);
        CompletableFuture<String> notStartedMsgFuture = configHandler.getStringAsync("quest-manager.quest.click-not-started");
        CompletableFuture<String> inProgressMsgFuture = configHandler.getStringAsync("quest-manager.quest.click-in-progress");
        CompletableFuture<String> completedMsgFuture = configHandler.getStringAsync("quest-manager.quest.click-completed");
        CompletableFuture<String> canceledMsgFuture = configHandler.getStringAsync("quest-manager.quest.click-canceled");
        CompletableFuture<String> mainQuestColourHexString = configHandler.getStringAsync("quest-manager.quest.main-colour");
        CompletableFuture<String> statusMsgInProgressFuture = configHandler.getStringAsync("quest-manager.quest.status.in-progress.message");
        CompletableFuture<String> statusMsgCompletedFuture = configHandler.getStringAsync("quest-manager.quest.status.completed.message");
        CompletableFuture<String> statusMsgCanceledFuture = configHandler.getStringAsync("quest-manager.quest.status.canceled.message");
        CompletableFuture<String> statusMsgNotStartedFuture = configHandler.getStringAsync("quest-manager.quest.status.not-started.message");
        CompletableFuture<String> emptyLoreFuture = configHandler.getStringAsync("quest-manager.lore-entries.empty");
        CompletableFuture<String> statusLabelFuture = configHandler.getStringAsync("quest-manager.lore-entries.status-label");
        CompletableFuture<String> descriptionLabelFuture = configHandler.getStringAsync("quest-manager.lore-entries.description-label");
        CompletableFuture<String> objectiveLabelFuture = configHandler.getStringAsync("quest-manager.lore-entries.objective-label");
        CompletableFuture<String> timeLimitLabelFuture = configHandler.getStringAsync("quest-manager.lore-entries.time-limit-label");
        CompletableFuture<String> timeLeftLabelFuture = configHandler.getStringAsync("quest-manager.lore-entries.time-left-label");
        CompletableFuture<String> rewardsLabelFuture = configHandler.getStringAsync("quest-manager.lore-entries.rewards-label");
        CompletableFuture<String> rewardsLabelColourFuture = configHandler.getStringAsync("quest-manager.lore-entries.rewards-colour");

        return CompletableFuture.allOf(
                questStatusFuture, rewardsFuture, playerQuestFuture, notStartedMsgFuture, inProgressMsgFuture,
                completedMsgFuture, canceledMsgFuture, mainQuestColourHexString, statusMsgInProgressFuture,
                statusMsgCompletedFuture, statusMsgCanceledFuture, statusMsgNotStartedFuture, emptyLoreFuture,
                statusLabelFuture, descriptionLabelFuture, objectiveLabelFuture, timeLimitLabelFuture,
                timeLeftLabelFuture, rewardsLabelFuture, rewardsLabelColourFuture).thenCompose(v -> {
            try {
                ItemStack itemStack = new ItemStack(Material.PAPER);
                var questStatusComponent = questStatusFuture.get();
                var playerQuest = playerQuestFuture.get();
                var rewards = rewardsFuture.get();
                var statusMsgInProgress = statusMsgInProgressFuture.join();
                var statusMsgCompleted = statusMsgCompletedFuture.join();
                var statusMsgCanceled = statusMsgCanceledFuture.join();
                var statusMsgNotStarted = statusMsgNotStartedFuture.join();

                String notStartedMsg = notStartedMsgFuture.get();
                String inProgressMsg = inProgressMsgFuture.get();
                String completedMsg = completedMsgFuture.get();
                String canceledMsg = canceledMsgFuture.get();
                String mainQuestColorHexString = mainQuestColourHexString.get();

                String emptyLore = emptyLoreFuture.get();
                String statusLabel = statusLabelFuture.get();
                String descriptionLabel = descriptionLabelFuture.get();
                String objectiveLabel = objectiveLabelFuture.get();
                String timeLimitLabel = timeLimitLabelFuture.get();
                String timeLeftLabel = timeLeftLabelFuture.get();
                String rewardsLabel = rewardsLabelFuture.get();
                String rewardsLabelColour = rewardsLabelColourFuture.get();

                var mainQuestColor = TextColor.fromCSSHexString(mainQuestColorHexString);
                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.displayName(Component.text(quest.getName(), mainQuestColor));

                String questStatusContent = questStatusComponent.content();
                String statusMsg;
                if (questStatusContent.equals(statusMsgNotStarted)) {
                    statusMsg = notStartedMsg;
                } else if (questStatusContent.equals(statusMsgInProgress)) {
                    statusMsg = inProgressMsg;
                } else if (questStatusContent.equals(statusMsgCompleted)) {
                    statusMsg = completedMsg;
                } else if (questStatusContent.equals(statusMsgCanceled)) {
                    statusMsg = canceledMsg;
                } else {
                    statusMsg = "";
                }
                if (!statusMsg.isEmpty()) {
                    lore.add(Component.text(statusMsg));
                    lore.add(Component.text(emptyLore));
                }

                lore.add(Component.text(statusLabel, mainQuestColor));
                lore.add(questStatusComponent);
                lore.add(Component.text(emptyLore));
                lore.add(Component.text(descriptionLabel, mainQuestColor));
                lore.add(Component.text(quest.getDescription()));
                lore.add(Component.text(emptyLore));
                lore.add(Component.text(objectiveLabel, mainQuestColor));
                lore.add(Component.text(quest.getObjective().getObjective()));
                lore.add(Component.text(emptyLore));
                lore.add(Component.text(timeLimitLabel, mainQuestColor));
                lore.add(Component.text(Utils.getTimeStringFromSecs(quest.getTimeLimit())));

                if (questStatusComponent.content().equals(statusMsgInProgress)) {
                    lore.add(Component.text(emptyLore));
                    lore.add(Component.text(timeLeftLabel, mainQuestColor));
                    lore.add(Component.text(Utils.getTimeStringFromSecs(quest.getTimeLimit() - playerQuest.getTime())));
                }

                lore.add(Component.text(emptyLore));
                lore.add(Component.text(rewardsLabel, mainQuestColor));
                for (Reward reward : rewards) {
                    lore.add(Component.text(reward.getName(), TextColor.fromHexString(rewardsLabelColour)));
                }
                itemMeta.lore(lore);
                itemStack.setItemMeta(itemMeta);
                return CompletableFuture.completedFuture(itemStack);
            } catch (Exception e) {
                logger.error(e.getMessage(),e);
            }
            return CompletableFuture.completedFuture(new ItemStack(Material.AIR));
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(),ex);
            return null;
        });
    }

    public void finaliseInventory(Inventory inventory, int size) {
        if (inventory == null) return;
        for (int i = 0; i < size; i++) {
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
        var allQuests = configHandler.getStringAsync("quest-manager.quest.all").join();
        if (!type.equals(allQuests)) {
            return CompletableFuture.supplyAsync(() -> getComponentFromType(type));
        } else {
            CompletableFuture<List<PlayerQuest>> allPlayerQuestsFuture = databaseHandler.getPlayerQuestsAsync(player.getUniqueId(), QuestStatus.NOT_STARTED.name());

            return allPlayerQuestsFuture.thenApply(allPlayerQuests -> {
                PlayerQuest playerQuest = allPlayerQuests.stream()
                        .filter(playerQuest1 -> playerQuest1.getQuest().getId() == quest.getId())
                        .findFirst()
                        .orElse(null);
                if (playerQuest == null) return getComponentFromType(QuestStatus.NOT_STARTED.name());
                return getComponentFromType(playerQuest.getQuestStatus().getStatus());
            }).exceptionally(ex -> {
                logger.error(ex.getMessage(),ex);
                return Component.text("");
            });
        }
    }


    private TextComponent getComponentFromType(String type) {
        CompletableFuture<String> statusMsgInProgressFuture = configHandler.getStringAsync("quest-manager.quest.status.in-progress.message");
        CompletableFuture<String> statusMsgCompletedFuture = configHandler.getStringAsync("quest-manager.quest.status.completed.message");
        CompletableFuture<String> statusMsgCanceledFuture = configHandler.getStringAsync("quest-manager.quest.status.canceled.message");
        CompletableFuture<String> statusMsgNotStartedFuture = configHandler.getStringAsync("quest-manager.quest.status.not-started.message");

        CompletableFuture<String> statusColorInProgressFuture = configHandler.getStringAsync("quest-manager.quest.status.in-progress.color");
        CompletableFuture<String> statusColorCompletedFuture = configHandler.getStringAsync("quest-manager.quest.status.completed.color");
        CompletableFuture<String> statusColorCanceledFuture = configHandler.getStringAsync("quest-manager.quest.status.canceled.color");
        CompletableFuture<String> statusColorNotStartedFuture = configHandler.getStringAsync("quest-manager.quest.status.not-started.color");

        return CompletableFuture.allOf(
                statusMsgInProgressFuture, statusMsgCompletedFuture, statusMsgCanceledFuture, statusMsgNotStartedFuture,
                statusColorInProgressFuture, statusColorCompletedFuture, statusColorCanceledFuture, statusColorNotStartedFuture
        ).thenApply(v -> {
            try {
                var statusMsgInProgress = statusMsgInProgressFuture.get();
                var statusMsgCompleted = statusMsgCompletedFuture.get();
                var statusMsgCanceled = statusMsgCanceledFuture.get();
                var statusMsgNotStarted = statusMsgNotStartedFuture.get();

                var statusColorInProgress = TextColor.fromCSSHexString(statusColorInProgressFuture.get());
                var statusColorCompleted = TextColor.fromCSSHexString(statusColorCompletedFuture.get());
                var statusColorCanceled = TextColor.fromCSSHexString(statusColorCanceledFuture.get());
                var statusColorNotStarted = TextColor.fromCSSHexString(statusColorNotStartedFuture.get());

                QuestStatus questStatus;
                try {
                    questStatus = QuestStatus.valueOf(type.toUpperCase());
                } catch (IllegalArgumentException e) {
                    questStatus = QuestStatus.NOT_STARTED;
                }

                if (questStatus == QuestStatus.CANCELED) {
                    return Component.text(statusMsgCanceled, statusColorCanceled);
                } else if (questStatus == QuestStatus.IN_PROGRESS) {
                    return Component.text(statusMsgInProgress, statusColorInProgress);
                } else if (questStatus == QuestStatus.COMPLETED) {
                    return Component.text(statusMsgCompleted, statusColorCompleted);
                }
                return Component.text(statusMsgNotStarted, statusColorNotStarted);
            } catch (Exception e) {
                logger.error(e.getMessage(),e);
                return Component.text("");
            }
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(),ex);
            return Component.text("");
        }).join();
    }


    public CompletableFuture<Void> reactivateQuest(Player player, TextComponent displayname) {
        var questFuture = databaseHandler.getQuestByNameAsync(displayname.content());
        return questFuture.thenCompose(quest -> {
            var playerQuestFuture = databaseHandler.getPlayerQuestByQuestIdAsync(player.getUniqueId(), quest.getId());
            CompletableFuture<String> reactivationMessageFuture = configHandler.getStringAsync("quest-manager.quest.reactivated.message");
            CompletableFuture<String> reactivationMessageColourFuture = configHandler.getStringAsync("quest-manager.quest.reactivated.colour");
            CompletableFuture<String> reactivationSoundNameFuture = configHandler.getStringAsync("quest-manager.quest.reactivated.sound.name");
            CompletableFuture<Integer> reactivationSoundVolumeFuture = configHandler.getIntAsync("quest-manager.quest.reactivated.sound.volume");
            CompletableFuture<Integer> reactivationSoundPitchFuture = configHandler.getIntAsync("quest-manager.quest.reactivated.sound.pitch");
            var changePlayerQuestFuture =
                    databaseHandler.changePlayerQuestType(player.getUniqueId(), displayname.content(), QuestStatus.IN_PROGRESS.name(), player.getLocation());
            return CompletableFuture.allOf(reactivationMessageColourFuture, reactivationSoundNameFuture,
                    reactivationSoundVolumeFuture, reactivationSoundPitchFuture,
                    playerQuestFuture, reactivationMessageFuture, changePlayerQuestFuture).thenAccept(x -> {
                try {
                    var playerQuest = playerQuestFuture.get();
                    var reactivationMessage = reactivationMessageFuture.get();
                    var reactivationMessageColour = TextColor.fromHexString(reactivationMessageColourFuture.get());
                    var reactivationSoundName = reactivationSoundNameFuture.get();
                    var reactivationSoundVolume = reactivationSoundVolumeFuture.get();
                    var reactivationSoundPitch = reactivationSoundPitchFuture.get();
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (PlayerMoveUtils.playerQuestConfig.containsKey(player.getUniqueId()))
                            playerQuestConfig.get(player.getUniqueId()).add(playerQuest);
                        else
                            playerQuestConfig.put(player.getUniqueId(), Collections.synchronizedList(new ArrayList<>(List.of(playerQuest))));
                        player.sendMessage(Component.text(reactivationMessage + displayname.content(), reactivationMessageColour));
                        player.playSound(player.getLocation(),
                                Sound.valueOf(reactivationSoundName),
                                reactivationSoundVolume,
                                reactivationSoundPitch);
                        createAndShowBossBar(player, displayname.content(), 0.0f);
                    });
                    if (runnable.containsKey(playerQuest)) {
                        runnable.get(playerQuest).cancel();
                        runnable.remove(playerQuest);
                    }
                    BukkitRunnable task = new QuestTimerTask(player, displayname.content(), quest.getId());
                    runnable.put(playerQuest, task);
                    task.runTaskTimer(plugin, 0L, 20L);

                } catch (Exception e) {
                    logger.error(e.getMessage(),e);
                }
            }).exceptionally(ex -> {
                logger.error(ex.getMessage(),ex);
                return null;
            });
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(),ex);
            return null;
        });


    }

    public CompletableFuture<Void> cancelQuest(Player player, TextComponent displayname) {
        var questFuture = databaseHandler.getQuestByNameAsync(displayname.content());
        return questFuture.thenAccept(quest -> {
            var playerQuestFuture = databaseHandler.getPlayerQuestByQuestIdAsync(player.getUniqueId(), quest.getId());
            CompletableFuture<String> cancellationMessageFuture = configHandler.getStringAsync("quest-manager.quest.canceled.message");
            CompletableFuture<String> cancellationMessageColourFuture = configHandler.getStringAsync("quest-manager.quest.canceled.colour");
            CompletableFuture<String> cancellationSoundNameFuture = configHandler.getStringAsync("quest-manager.quest.canceled.sound.name");
            CompletableFuture<Integer> cancellationSoundVolumeFuture = configHandler.getIntAsync("quest-manager.quest.canceled.sound.volume");
            CompletableFuture<Integer> cancellationSoundPitchFuture = configHandler.getIntAsync("quest-manager.quest.canceled.sound.pitch");
            var changePlayerQuestFuture =
                    databaseHandler.changePlayerQuestType(player.getUniqueId(), displayname.content(), QuestStatus.CANCELED.name(), player.getLocation());
            CompletableFuture.allOf(
                    cancellationSoundPitchFuture, cancellationSoundVolumeFuture, cancellationSoundNameFuture,
                    cancellationMessageColourFuture,
                    playerQuestFuture, cancellationMessageFuture, changePlayerQuestFuture).thenAccept(x -> {
                try {
                    var playerQuest = playerQuestFuture.get();
                    var cancellationMessage = cancellationMessageFuture.get();
                    var cancellationMessageColour = TextColor.fromHexString(cancellationMessageColourFuture.get());
                    var cancellationSoundName = cancellationSoundNameFuture.get();
                    var cancellationSoundVolume = cancellationSoundVolumeFuture.get();
                    var cancellationSoundPitch = cancellationSoundPitchFuture.get();

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (PlayerMoveUtils.playerQuestConfig.containsKey(player.getUniqueId()))
                            PlayerMoveUtils.playerQuestConfig.get(player.getUniqueId()).remove(playerQuest);
                        if (runnable.containsKey(playerQuest)) {
                            runnable.get(playerQuest).cancel();
                            runnable.remove(playerQuest);
                        }
                        player.sendMessage(Component.text(cancellationMessage + displayname.content(), cancellationMessageColour));
                        player.playSound(player.getLocation(), Sound.valueOf(cancellationSoundName), cancellationSoundVolume, cancellationSoundPitch);
                        hideBossBar(player, displayname.content());
                    });
                } catch (Exception e) {
                    logger.error(e.getMessage(),e);
                }
            }).exceptionally(ex -> {
                logger.error(ex.getMessage(),ex);
                return null;
            });
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(),ex);
            return null;
        });
    }

    public void acceptQuest(Player player, TextComponent displayname, String objective) {
        var questFuture = databaseHandler.getQuestByNameAsync(displayname.content());
        var addFuture = databaseHandler.addPlayerQuest(player.getUniqueId(), displayname.content(), player.getLocation());
        CompletableFuture.allOf(questFuture, addFuture).thenAccept(v -> {
            try {
                var quest = questFuture.get();

                CompletableFuture<String> acceptMessageFuture = configHandler.getStringAsync("quest-manager.quest.accepted.message");
                CompletableFuture<String> acceptMessageColourFuture = configHandler.getStringAsync("quest-manager.quest.accepted.colour");
                CompletableFuture<String> acceptSoundNameFuture = configHandler.getStringAsync("quest-manager.quest.accepted.sound.name");
                CompletableFuture<Integer> acceptSoundVolumeFuture = configHandler.getIntAsync("quest-manager.quest.accepted.sound.volume");
                CompletableFuture<Integer> acceptSoundPitchFuture = configHandler.getIntAsync("quest-manager.quest.accepted.sound.pitch");
                CompletableFuture<String> objectiveMessageFuture = configHandler.getStringAsync("quest-manager.quest.objective");
                var playerQuestFuture = databaseHandler.getPlayerQuestByQuestIdAsync(player.getUniqueId(), quest.getId());
                CompletableFuture.allOf(
                        acceptMessageColourFuture, acceptSoundPitchFuture, acceptSoundVolumeFuture, acceptSoundNameFuture,
                        acceptMessageFuture, objectiveMessageFuture, playerQuestFuture).thenAccept(x -> {
                    try {
                        var playerQuest = playerQuestFuture.get();
                        var acceptMessage = acceptMessageFuture.get();
                        var objectiveMessage = objectiveMessageFuture.get();
                        var acceptMessageColour = TextColor.fromHexString(acceptMessageColourFuture.get());
                        var acceptSoundName = acceptSoundNameFuture.get();
                        var acceptSoundVolume = acceptSoundVolumeFuture.get();
                        var acceptSoundPitch = acceptSoundPitchFuture.get();
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (PlayerMoveUtils.playerQuestConfig.containsKey(player.getUniqueId()))
                                playerQuestConfig.get(player.getUniqueId()).add(playerQuest);
                            else
                                playerQuestConfig.put(player.getUniqueId(), Collections.synchronizedList(new ArrayList<>(List.of(playerQuest))));
                            player.sendMessage(Component.text(acceptMessage + displayname.content(), acceptMessageColour));
                            player.sendMessage(Component.text(objectiveMessage + objective, acceptMessageColour));
                            player.playSound(player.getLocation(), Sound.valueOf(acceptSoundName), acceptSoundVolume, acceptSoundPitch);
                            createAndShowBossBar(player, displayname.content(), 0.0f);
                        });
                        BukkitRunnable task = new QuestTimerTask(player, displayname.content(), quest.getId());
                        runnable.put(playerQuest, task);
                        task.runTaskTimer(plugin, 0L, 20L);

                    } catch (Exception e) {
                        logger.error(e.getMessage(),e);
                    }
                }).exceptionally(ex -> {
                    logger.error(ex.getMessage(),ex);
                    return null;
                });
            } catch (Exception e) {
                logger.error(e.getMessage(),e);
            }
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(),ex);
            return null;
        });
    }

    public void createAndShowBossBar(Player player, String title, float progress) {
        var bossBarColour = configHandler.getStringAsync("quest-manager.boss-bar.init.colour").join();
        var bossBarOverlay = configHandler.getStringAsync("quest-manager.boss-bar.init.overlay").join();
        var bossbar = BossBar.bossBar(Component.text(title), progress, BossBar.Color.valueOf(bossBarColour), BossBar.Overlay.valueOf(bossBarOverlay));
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
        if (bossbar == null) return;
        bossbar.progress(progress);
        if (progress == 1.0f) {
            var bossBarColour = configHandler.getStringAsync("quest-manager.boss-bar.complete.colour").join();
            bossbar.color(BossBar.Color.valueOf(bossBarColour));
            completeQuest(player, title, playerQuest);
        }
    }

    private void completeQuest(Player player, String title, PlayerQuest playerQuest) {
        CompletableFuture<String> completedMessageFuture = configHandler.getStringAsync("quest-manager.quest.completed.message");
        CompletableFuture<String> completedMessageColourFuture = configHandler.getStringAsync("quest-manager.quest.completed.colour");
        CompletableFuture<String> completedSoundNameFuture = configHandler.getStringAsync("quest-manager.quest.completed.sound.name");
        CompletableFuture<Integer> completedSoundVolumeFuture = configHandler.getIntAsync("quest-manager.quest.completed.sound.volume");
        CompletableFuture<Integer> completedSoundPitchFuture = configHandler.getIntAsync("quest-manager.quest.completed.sound.pitch");
        CompletableFuture<String> completedAlertMessageFuture = configHandler.getStringAsync("quest-manager.quest.completed.alert.message");
        CompletableFuture<Integer> completedAlertColourFuture = configHandler.getIntAsync("quest-manager.quest.completed.alert.colour");
        CompletableFuture<Integer> completedAlertFadeInFuture = configHandler.getIntAsync("quest-manager.quest.completed.alert.fade-in");
        CompletableFuture<Integer> completedAlertStayFuture = configHandler.getIntAsync("quest-manager.quest.completed.alert.stay");
        CompletableFuture<Integer> completedAlertFadeOutFuture = configHandler.getIntAsync("quest-manager.quest.completed.alert.fade-out");
        var changePlayerQuestFuture =
                databaseHandler.changePlayerQuestType(player.getUniqueId(), title, "COMPLETED", player.getLocation());
        CompletableFuture.allOf(
                completedAlertMessageFuture, completedAlertColourFuture, completedAlertFadeInFuture,
                completedAlertStayFuture, completedAlertFadeOutFuture,
                completedMessageFuture, completedMessageColourFuture, completedSoundPitchFuture,
                completedSoundVolumeFuture, completedSoundNameFuture,
                completedMessageFuture, changePlayerQuestFuture).thenAccept(x -> {
            try {
                var completedMessage = completedMessageFuture.get();
                var completedMessageColour = TextColor.fromHexString(completedMessageColourFuture.get());
                var completedSoundName = completedSoundNameFuture.get();
                var completedSoundVolume = completedSoundVolumeFuture.get();
                var completedSoundPitch = completedSoundPitchFuture.get();
                var completedAlertMessage = completedAlertMessageFuture.get();
                var completedAlertColour = completedAlertColourFuture.get();
                var completedAlertFadeIn = completedAlertFadeInFuture.get();
                var completedAlertStay = completedAlertStayFuture.get();
                var completedAlertFadeOut = completedAlertFadeOutFuture.get();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (PlayerMoveUtils.playerQuestConfig.containsKey(player.getUniqueId()))
                        PlayerMoveUtils.playerQuestConfig.get(player.getUniqueId()).remove(playerQuest);
                    player.sendMessage(Component.text(completedMessage + title, completedMessageColour));
                    MessageUtils.sendAlertToPlayer(completedAlertMessage,
                            completedMessage + title,
                            completedAlertFadeIn,
                            completedAlertStay,
                            completedAlertFadeOut,
                            Color.fromRGB(completedAlertColour), player);
                    player.playSound(player.getLocation(), Sound.valueOf(completedSoundName), completedSoundVolume, completedSoundPitch);
                    if (runnable.containsKey(playerQuest)) {
                        runnable.get(playerQuest).cancel();
                        runnable.remove(playerQuest);
                    }
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        hideBossBar(player, title);
                    }, 100);
                });
                giveRewards(player, playerQuest);
            } catch (Exception e) {
                logger.error(e.getMessage(),e);
            }
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(),ex);
            return null;
        });
    }

    private void giveRewards(Player player, PlayerQuest playerQuest) {
        var quest = playerQuest.getQuest();
        var rewardsFuture = databaseHandler.getQuestRewardsAsync(quest);
        var rewardsMessageFuture = configHandler.getStringAsync("quest-manager.quest.rewards.message");
        var rewardsColourFuture = configHandler.getStringAsync("quest-manager.quest.rewards.colour");
        CompletableFuture.allOf(rewardsFuture, rewardsColourFuture, rewardsMessageFuture).thenAccept(v -> {
            try {
                var rewards = rewardsFuture.get();
                var rewardsMessage = rewardsMessageFuture.get();
                var rewardsColour = TextColor.fromHexString(rewardsColourFuture.get());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Reward reward : rewards) {
                        var rewardTypeName = reward.getRewardType().getName();

                        if (rewardTypeName.equals(RewardType.XP.name())) {
                            player.giveExp(reward.getAmount());
                        } else if (rewardTypeName.equals(RewardType.ITEM.name())) {
                            ItemStack itemStack = new ItemStack(Objects.requireNonNull(Material.getMaterial(reward.getValue())), reward.getAmount());
                            player.getInventory().addItem(itemStack);
                        }
                        player.sendMessage(Component.text(rewardsMessage + reward.getName(), rewardsColour));
                    }
                });
            } catch (Exception e) {
                logger.error(e.getMessage(),e);
            }
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(),ex);
            return null;
        });

    }

    public void hideBossBar(Player player, String title) {
        var bossBar = getBossBarFromTitle(player, title);
        if (bossBar == null) return;
        player.hideBossBar(bossBar);
        bossBars.get(player.getUniqueId()).remove(bossBar);
    }

    public void setupInventory(Player player, String type) {
        initInventory(player, type).thenAccept(x -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory inventory = inventories.get(player.getUniqueId()).getInventory();
                player.openInventory(inventory);
                inventories.get(player.getUniqueId()).setInventory(inventory);
            });
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(),ex);
            return null;
        });
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent e) {
        var playerQuestsFuture = databaseHandler.getPlayerQuestsAsync(e.getPlayer().getUniqueId(), QuestStatus.IN_PROGRESS.name());
        playerQuestsFuture.thenAccept(playerQuests -> {
            var allQuests = configHandler.getStringAsync("quest-manager.quest.all").join();
            if (playerQuests.isEmpty()) {
                initInventory(e.getPlayer(), allQuests);
            } else {
                initInventory(e.getPlayer(), QuestStatus.IN_PROGRESS.name());
            }
            for (PlayerQuest playerQuest : playerQuests) {
                BukkitRunnable task = new QuestTimerTask(e.getPlayer(), playerQuest.getQuest().getName(), playerQuest.getQuest().getId());
                runnable.put(playerQuest, task);
                task.runTaskTimer(plugin, 0L, 20L);
            }
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(),ex);
            return null;
        });
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent e) {
        var playerQuestsFuture = databaseHandler.getPlayerQuestsAsync(e.getPlayer().getUniqueId(), QuestStatus.IN_PROGRESS.name());
        playerQuestsFuture.thenAccept(playerQuests -> {
            for (PlayerQuest playerQuest : playerQuests) {
                if (runnable.containsKey(playerQuest)) {
                    runnable.get(playerQuest).cancel();
                    runnable.remove(playerQuest);
                }
            }
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(),ex);
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
            var failedMessageFuture = configHandler.getStringAsync("quest-manager.quest.failed.message");
            var failedMessageColourFuture = configHandler.getStringAsync("quest-manager.quest.failed.colour");
            var failedSoundNameFuture = configHandler.getStringAsync("quest-manager.quest.failed.sound.name");
            var failedSoundVolumeFuture = configHandler.getIntAsync("quest-manager.quest.failed.sound.volume");
            var failedSoundPitchFuture = configHandler.getIntAsync("quest-manager.quest.failed.sound.pitch");
            var failedAlertMessageFuture = configHandler.getStringAsync("quest-manager.quest.failed.alert.message");
            var failedAlertColourFuture = configHandler.getIntAsync("quest-manager.quest.failed.alert.colour");
            var failedAlertFadeInFuture = configHandler.getIntAsync("quest-manager.quest.failed.alert.fade-in");
            var failedAlertStayFuture = configHandler.getIntAsync("quest-manager.quest.failed.alert.stay");
            var failedAlertFadeOutFuture = configHandler.getIntAsync("quest-manager.quest.failed.alert.fade-out");
            var timeLimitMessageFuture = configHandler.getStringAsync("quest-manager.quest.time-limit");
            playerQuestFuture.thenAccept(playerQuest -> {
                try {
                    var copy = (PlayerQuest) playerQuest.clone();
                    if (copy.getQuestStatus().getStatus().equals(QuestStatus.IN_PROGRESS.name())) {
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
            CompletableFuture.allOf(
                    playerQuestFuture, failedMessageFuture, failedMessageColourFuture, failedSoundPitchFuture,
                    failedSoundVolumeFuture, failedSoundNameFuture, failedAlertMessageFuture, failedAlertColourFuture,
                    failedAlertFadeInFuture, failedAlertStayFuture, failedAlertFadeOutFuture,
                    playerQuestFuture, failedMessageFuture, timeLimitMessageFuture).thenAccept(v -> {
                try {
                    var playerQuest = playerQuestFuture.get();
                    var failedMessage = failedMessageFuture.get();
                    var timeLimitMessage = timeLimitMessageFuture.get();
                    var failedMessageColour = TextColor.fromHexString(failedMessageColourFuture.get());
                    var failedSoundName = failedSoundNameFuture.get();
                    var failedSoundVolume = failedSoundVolumeFuture.get();
                    var failedSoundPitch = failedSoundPitchFuture.get();
                    var failedAlertMessage = failedAlertMessageFuture.get();
                    var failedAlertColour = failedAlertColourFuture.get();
                    var failedAlertFadeIn = failedAlertFadeInFuture.get();
                    var failedAlertStay = failedAlertStayFuture.get();
                    var failedAlertFadeOut = failedAlertFadeOutFuture.get();
                    var invConfig = new InventoryMapping();
                    if (inventories.containsKey(player.getUniqueId())) {
                        invConfig = inventories.get(player.getUniqueId());
                    }
                    var inv = invConfig;
                    if (playerQuest.getQuestStatus().getStatus().equals(QuestStatus.IN_PROGRESS.name())) {
                        var questFuture = databaseHandler.getQuestByNameAsync(questName);
                        questFuture.thenAccept(x -> {
                            try {
                                var quest = questFuture.get();
                                if (playerQuest.getTime() >= quest.getTimeLimit()) {
                                    Bukkit.getScheduler().runTask(plugin, () -> {
                                        MessageUtils.sendAlertToPlayer(
                                                failedAlertMessage,
                                                failedMessage + questName + timeLimitMessage + getTimeStringFromSecs(quest.getTimeLimit()),
                                                failedAlertFadeIn, failedAlertStay, failedAlertFadeOut, Color.fromRGB(failedAlertColour), player);
                                        player.sendMessage(Component.text(
                                                failedMessage + questName + timeLimitMessage + getTimeStringFromSecs(quest.getTimeLimit()), failedMessageColour));
                                        player.playSound(player.getLocation(), Sound.valueOf(failedSoundName), failedSoundVolume, failedSoundPitch);
                                        cancel();
                                        runnable.remove(playerQuest);
                                        hideBossBar(player, questName);
                                    });
                                    if (PlayerMoveUtils.playerQuestConfig.containsKey(player.getUniqueId()))
                                        PlayerMoveUtils.playerQuestConfig.get(player.getUniqueId()).remove(playerQuest);
                                    databaseHandler.changePlayerQuestType(player.getUniqueId(), questName, QuestStatus.CANCELED.name(), player.getLocation());
                                }
                                if (inv.getInventory() != null)
                                    addQuestsToInventory(inv.getType(), player, inv.getInventory());
                            } catch (Exception e) {
                                logger.error(e.getMessage(),e);
                            }
                        });
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(),e);
                }
            }).exceptionally(ex -> {
                logger.error(ex.getMessage(),ex);
                return null;
            });

        }
    }
}
