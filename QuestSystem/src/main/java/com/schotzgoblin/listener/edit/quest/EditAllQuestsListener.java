package com.schotzgoblin.listener.edit.quest;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.database.Quest;
import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.QuestSystem;
import com.schotzgoblin.utils.edit.EditQuestsUtils;
import com.schotzgoblin.utils.edit.EditUtils;
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

public class EditAllQuestsListener implements Listener {
    private static final Logger logger = LoggerFactory.getLogger(EditAllQuestsListener.class);
    private final QuestSystem questSystem = QuestSystem.getInstance();
    private final DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
    private final ConfigHandler configHandler = ConfigHandler.getInstance();
    //Normally I would make all those edit classes generic, so I have to do it once, but unfortunately I don't have the time to do it now

    public EditAllQuestsListener() {
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        UUID playerId = player.getUniqueId();
        if (!EditQuestsUtils.allQuestsInventory.containsKey(playerId)) return;
        Inventory inv = EditQuestsUtils.allQuestsInventory.get(playerId);
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
            var createQuestMaterial = configHandler.getMaterialAsync("inventory.create-quest.material").join();
            if (clickedItem.getType().equals(Material.PLAYER_HEAD)) {
                EditUtils.handlePageSwitch(player, inv, displayName.content());
            } else if (clickedItem.getType().equals(Material.PAPER)) {
                allQuestsInventory(e, player, playerId);
            } else if (clickedItem.getType().equals(createQuestMaterial)) {
                createQuest(player);
            }

        } catch (Exception ignored) {
            // Exception ignored, because it is not necessary to handle it (Only happens if player spam accepts EditQuestsUtils.quests)
        }
    }

    private void createQuest(Player player) {
        var createQuestNameDefaultValueFuture = configHandler.getStringAsync("inventory.create-quest.default.name");
        var createQuestTimeLimitDefaultValueFuture = configHandler.getIntAsync("inventory.create-quest.default.time-limit");
        var createQuestDescriptionDefaultValueFuture = configHandler.getStringAsync("inventory.create-quest.default.description");

        CompletableFuture.allOf(
                createQuestNameDefaultValueFuture,
                createQuestTimeLimitDefaultValueFuture,
                createQuestDescriptionDefaultValueFuture
        ).thenAcceptAsync(v -> {
            var createQuestNameDefaultValue = createQuestNameDefaultValueFuture.join();
            var createQuestTimeLimitDefaultValue = createQuestTimeLimitDefaultValueFuture.join();
            var createQuestDescriptionDefaultValue = createQuestDescriptionDefaultValueFuture.join();
            var quest = new Quest(createQuestNameDefaultValue, createQuestDescriptionDefaultValue, createQuestTimeLimitDefaultValue);
            EditQuestsUtils.editingQuest.put(player.getUniqueId(), quest);
            EditQuestsUtils.editQuestInventory(quest, player).thenAccept(inv -> Bukkit.getScheduler().runTask(questSystem, () -> {
                player.closeInventory();
                player.openInventory(inv);
            }));
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(), ex);
            return null;
        });
    }

    private void allQuestsInventory(InventoryClickEvent e, Player player, UUID playerId) {
        Inventory inv = EditQuestsUtils.allQuestsInventory.get(playerId);
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

            if (clickedItem.getType().equals(Material.PAPER)) {
                if (e.isShiftClick())
                    EditUtils.handleQuestItemDelete(player, displayName, inv, "quest");
                else if (e.isRightClick())
                    handleQuestItemEdit(player, displayName);
            }

        } catch (Exception ignored) {
            // Exception ignored, because it is not necessary to handle it (Only happens if player spam accepts EditQuestsUtils.quests)
        }
    }

    private void handleQuestItemEdit(Player player, TextComponent displayName) {
        var questFuture = databaseHandler.getQuestByNameAsync(displayName.content());

        CompletableFuture.allOf(questFuture).thenAccept(x -> {
            var quest = questFuture.join();
            EditQuestsUtils.editingQuest.put(player.getUniqueId(), quest);
            EditQuestsUtils.editQuestInventory(quest, player).thenAccept(inv -> Bukkit.getScheduler().runTask(questSystem, () -> {
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
