package com.schotzgoblin.listener.edit.objective;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.database.Objective;
import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.QuestSystem;
import com.schotzgoblin.utils.edit.EditQuestsUtils;
import com.schotzgoblin.utils.edit.EditObjectivesUtils;
import com.schotzgoblin.utils.edit.EditUtils;
import com.schotzgoblin.utils.Utils;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EditAllObjectivesListener implements Listener {
    private static final Logger logger = LoggerFactory.getLogger(EditAllObjectivesListener.class);
    private final QuestSystem questSystem = QuestSystem.getInstance();
    private final DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
    private final ConfigHandler configHandler = ConfigHandler.getInstance();
    //Normally I would make all those edit classes generic, so I have to do it once, but unfortunately I don't have the time to do it now
    public EditAllObjectivesListener() {
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        UUID playerId = player.getUniqueId();
        if (!EditObjectivesUtils.allObjectivesInventory.containsKey(playerId)) return;
        Inventory inv = EditObjectivesUtils.allObjectivesInventory.get(playerId);
        if (!Objects.equals(e.getClickedInventory(), inv)) return;
        e.setCancelled(true);
        try {
            ItemStack clickedItem = e.getCurrentItem();
            if (clickedItem == null) {
                return;
            }

            ItemMeta itemMeta = clickedItem.getItemMeta();
            if (itemMeta == null || !(itemMeta.displayName() instanceof TextComponent displayName)) {
                return;
            }
            var createObjectiveMaterial = configHandler.getMaterialAsync("inventory.create-objective.material").join();
            if (clickedItem.getType().equals(Material.PLAYER_HEAD)) {
                EditUtils.handlePageSwitch(player, inv, displayName.content());
            } else if (clickedItem.getType().equals(Material.EMERALD)) {
                allObjectivesInventory(e, player, playerId);
            } else if (clickedItem.getType().equals(Material.BARRIER)) {
                Utils.navigateToQuestsInventory(player);
            } else if (clickedItem.getType().equals(createObjectiveMaterial)) {
                createObjective(player);
            }
        } catch (Exception ignored) {

        }
    }

    private void createObjective(Player player) {
        var createObjectiveNameDefaultValueFuture = configHandler.getStringAsync("inventory.create-objective.default.name");
        var createObjectiveCountDefaultValueFuture = configHandler.getIntAsync("inventory.create-objective.default.count");
        var createObjectiveValueDefaultValueFuture = configHandler.getStringAsync("inventory.create-objective.default.value");
        var createObjectiveTypeDefaultValueFuture = configHandler.getStringAsync("inventory.create-objective.default.type");

        CompletableFuture.allOf(
                createObjectiveNameDefaultValueFuture,
                createObjectiveCountDefaultValueFuture,
                createObjectiveValueDefaultValueFuture,
                createObjectiveTypeDefaultValueFuture
        ).thenAcceptAsync(v -> {
            var createObjectiveNameDefaultValue = createObjectiveNameDefaultValueFuture.join();
            var createObjectiveCountDefaultValue = createObjectiveCountDefaultValueFuture.join();
            var createObjectiveValueDefaultValue = createObjectiveValueDefaultValueFuture.join();
            var createObjectiveTypeDefaultValue = createObjectiveTypeDefaultValueFuture.join();
            var objective = new Objective(createObjectiveNameDefaultValue, createObjectiveTypeDefaultValue,
                    createObjectiveValueDefaultValue, createObjectiveCountDefaultValue);
            EditObjectivesUtils.editingObjective.put(player.getUniqueId(), objective);
            EditObjectivesUtils.editObjectivesInventory(objective, player).thenAccept(inv -> Bukkit.getScheduler().runTask(questSystem, () -> {
                player.closeInventory();
                player.openInventory(inv);
            }));
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(), ex);
            return null;
        });
    }


    private void allObjectivesInventory(InventoryClickEvent e, Player player, UUID playerId) {
        Inventory inv = EditObjectivesUtils.allObjectivesInventory.get(playerId);
        if (!Objects.equals(e.getClickedInventory(), inv)) return;
        try {
            ItemStack clickedItem = e.getCurrentItem();
            if (clickedItem == null) {
                return;
            }

            ItemMeta itemMeta = clickedItem.getItemMeta();
            if (itemMeta == null || !(itemMeta.displayName() instanceof TextComponent displayName)) {
                return;
            }

            if (clickedItem.getType().equals(Material.EMERALD)) {
                if (e.isShiftClick())
                    EditUtils.handleQuestItemDelete(player, displayName, inv, "objective");
                else if (e.isRightClick())
                    handleQuestItemEdit(player, displayName);
                else if (e.isLeftClick()) {
                    handleObjectiveChange(player, displayName);
                }
            }

        } catch (Exception ignored) {
            // Exception ignored, because it is not necessary to handle it (Only happens if player spam accepts EditQuestsUtils.quests)
        }
    }

    private void handleObjectiveChange(Player player, TextComponent displayName) {
        var objectiveFuture = databaseHandler.getObjectiveByNameAsync(displayName.content());

        CompletableFuture.allOf(objectiveFuture).thenAccept(x -> {
            var objective = objectiveFuture.join();
            var quest = EditQuestsUtils.editingQuest.get(player.getUniqueId());
            quest.setObjectiveId(objective.getId());
            quest.setObjective(objective);
            databaseHandler.updateAsync(quest).thenAccept(v -> EditObjectivesUtils.refreshObjectives(quest).join()).exceptionally(ex -> {
                logger.error(ex.getMessage(), ex);
                return null;
            });
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(), ex);
            return null;
        });
    }

    private void handleQuestItemEdit(Player player, TextComponent displayName) {
        var objectiveFuture = databaseHandler.getObjectiveByNameAsync(displayName.content());

        CompletableFuture.allOf(objectiveFuture).thenAccept(x -> {
            var objective = objectiveFuture.join();
            EditObjectivesUtils.editingObjective.put(player.getUniqueId(), objective);
            EditObjectivesUtils.editObjectivesInventory(objective, player).thenAccept(inv -> Bukkit.getScheduler().runTask(questSystem, () -> {
                player.closeInventory();
                player.openInventory(inv);
            })).exceptionally(ex -> {
                logger.error(ex.getMessage(), ex);
                return null;
            });
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(), ex);
            return null;
        });

    }
}
