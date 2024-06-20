package com.schotzgoblin.listener.edit.quest;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.database.Quest;
import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.QuestSystem;
import com.schotzgoblin.utils.Utils;
import com.schotzgoblin.utils.edit.EditObjectivesUtils;
import com.schotzgoblin.utils.edit.EditQuestsUtils;
import com.schotzgoblin.utils.edit.EditRewardsUtils;
import com.schotzgoblin.utils.edit.EditUtils;
import net.kyori.adventure.text.TextComponent;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class EditSingleQuestListener implements Listener {
    private static final Logger logger = LoggerFactory.getLogger(EditSingleQuestListener.class);
    private final QuestSystem questSystem = QuestSystem.getInstance();
    private final DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
    private final ConfigHandler configHandler = ConfigHandler.getInstance();
    private static final Map<String, BiConsumer<Player, Quest>> actionMap = new HashMap<>();

    {
        actionMap.put("inventory.edit-quest.name.material", this::editQuestName);
        actionMap.put("inventory.edit-quest.description.material", this::editQuestDescription);
        actionMap.put("inventory.edit-quest.objective.material", this::editQuestObjective);
        actionMap.put("inventory.edit-quest.time-limit.material", this::editQuestTimeLimit);
        actionMap.put("inventory.edit-quest.rewards.material", this::editQuestRewards);
        actionMap.put("inventory.edit-quest.save.material", this::saveQuest);
        actionMap.put("inventory.edit-quest.cancel.material", (player, quest) -> cancelEdit(player));
    }
    //Normally I would make all those edit classes generic, so I have to do
    // it once, but unfortunately I don't have the time to do it now

    public EditSingleQuestListener() {
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        UUID playerId = player.getUniqueId();
        if (!EditQuestsUtils.editQuestInventory.containsKey(playerId)) return;
        Inventory inv = EditQuestsUtils.editQuestInventory.get(playerId);
        if (!Objects.equals(e.getClickedInventory(), inv)) return;
        e.setCancelled(true);
        try {
            ItemStack clickedItem = e.getCurrentItem();
            if (clickedItem == null) {
                return;
            }

            ItemMeta itemMeta = clickedItem.getItemMeta();
            if (itemMeta == null || !(itemMeta.displayName() instanceof TextComponent)) {
                return;
            }
            editQuestAttributeInventory(player, playerId, clickedItem);

        } catch (Exception ignored) {
            // Exception ignored, because it is not necessary to handle it
            // (Only happens if player spam accepts EditQuestsUtils.quests)
        }
    }


    private void editQuestAttributeInventory(Player player, UUID playerId, ItemStack clickedItem) {
        Map<String, CompletableFuture<String>> futures = new HashMap<>();
        actionMap.keySet().forEach(key -> futures.put(key, configHandler.getStringAsync(key)));

        EditUtils.handleClickedItem(player, clickedItem, futures, actionMap,
                () -> EditQuestsUtils.editingQuest.get(playerId));
    }

    private void editQuestRewards(Player player, Quest quest) {
        EditRewardsUtils.initInventory(player, quest).thenAccept(v -> {
            if (EditRewardsUtils.allRewardsInventory.containsKey(player.getUniqueId())) {
                Bukkit.getScheduler().runTask(questSystem, () -> {
                    player.closeInventory();
                    player.openInventory(EditRewardsUtils.allRewardsInventory.get(player.getUniqueId()));
                });
            }
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(), ex);
            return null;
        });
    }

    private void editQuestObjective(Player player, Quest quest) {
        EditObjectivesUtils.initInventory(player, quest).thenAccept(v -> {
            if (EditObjectivesUtils.allObjectivesInventory.containsKey(player.getUniqueId())) {
                Bukkit.getScheduler().runTask(questSystem, () -> {
                    player.closeInventory();
                    player.openInventory(EditObjectivesUtils.allObjectivesInventory.get(player.getUniqueId()));
                });
            }
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(), ex);
            return null;
        });
    }

    private void editQuestProperty(Player player, Quest quest, String configKey, String initialValue, BiConsumer<Quest, String> onValueChange) {
        CompletableFuture<String> changeMsgFuture = configHandler.getStringAsync(configKey);
        changeMsgFuture.thenAcceptAsync(changeMsg -> {
            Bukkit.getScheduler().runTask(questSystem, () -> {
                new AnvilGUI.Builder().onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }
                    return Arrays.asList(AnvilGUI.ResponseAction.close(), AnvilGUI.ResponseAction.run(() -> {
                        onValueChange.accept(quest, stateSnapshot.getText());
                    }));
                }).onClose(player1 -> {
                    EditQuestsUtils.editQuestInventory(quest, player).thenAccept(v -> {
                        Bukkit.getScheduler().runTask(questSystem, () -> {
                            player.openInventory(EditQuestsUtils.editQuestInventory.get(player.getUniqueId()));
                        });
                    }).exceptionally(ex -> {
                        logger.error(ex.getMessage(), ex);
                        return null;
                    });
                }).text(initialValue).title(changeMsg).plugin(questSystem).open(player);
            });
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(), ex);
            return null;
        });
    }

    private void editQuestTimeLimit(Player player, Quest quest) {
        editQuestProperty(player, quest, "inventory.edit-quest.time-limit.change-message",
                Utils.getTimeStringFromSecs(quest.getTimeLimit()), (q, newValue) -> q.setTimeLimit(Utils.getSecondsFromTimeString(newValue)));
    }

    private void editQuestDescription(Player player, Quest quest) {
        editQuestProperty(player, quest, "inventory.edit-quest.description.change-message",
                quest.getDescription(), Quest::setDescription);
    }

    private void editQuestName(Player player, Quest quest) {
        editQuestProperty(player, quest, "inventory.edit-quest.name.change-message",
                quest.getName(), Quest::setName);
    }

    private void cancelEdit(Player player) {
        EditQuestsUtils.editQuestInventory.remove(player.getUniqueId());
        EditQuestsUtils.editingQuest.remove(player.getUniqueId());
        EditQuestsUtils.refreshQuests().thenAccept(v -> {
            Bukkit.getScheduler().runTask(questSystem, () -> {
                player.closeInventory();
                player.openInventory(EditQuestsUtils.allQuestsInventory.get(player.getUniqueId()));
            });
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(), ex);
            return null;
        });
    }

    private void saveQuest(Player player, Quest quest) {
        var createQuestNameDefaultValueFuture = configHandler.getStringAsync("inventory.create-quest.default.name").join();
        var createQuestErrorDefaultValueFuture = configHandler.getStringAsync("inventory.create-quest.error-message").join();

        if (quest.getName().equals(createQuestNameDefaultValueFuture)) {
            player.sendMessage(createQuestErrorDefaultValueFuture);
            return;
        }

        EditQuestsUtils.editQuestInventory.remove(player.getUniqueId());
        EditQuestsUtils.editingQuest.remove(player.getUniqueId());

        CompletableFuture<Void> saveOrUpdateFuture;
        if (quest.getId() == 0) {
            saveOrUpdateFuture = databaseHandler.saveAsync(quest);
        } else {
            saveOrUpdateFuture = databaseHandler.updateAsync(quest);
        }

        saveOrUpdateFuture.thenCompose(v -> EditQuestsUtils.refreshQuests()).thenAccept(v -> Bukkit.getScheduler().runTask(questSystem, () -> {
            player.closeInventory();
            player.openInventory(EditQuestsUtils.allQuestsInventory.get(player.getUniqueId()));
        })).exceptionally(ex -> {
            logger.error(ex.getMessage(), ex);
            return null;
        });
    }

}
