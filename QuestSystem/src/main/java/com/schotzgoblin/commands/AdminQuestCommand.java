package com.schotzgoblin.commands;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.database.Quest;
import com.schotzgoblin.database.Reward;
import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.QuestManager;
import com.schotzgoblin.main.QuestSystem;
import com.schotzgoblin.utils.EditQuestsUtils;
import com.schotzgoblin.utils.EditUtils;
import com.schotzgoblin.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import tsp.headdb.core.api.HeadAPI;
import tsp.headdb.implementation.head.Head;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class AdminQuestCommand implements CommandExecutor, Listener {

    private final QuestSystem questSystem = QuestSystem.getInstance();
    private final DatabaseHandler databaseHandler;
    private final QuestManager questManager = QuestManager.getInstance();
    private final ConfigHandler configHandler = ConfigHandler.getInstance();

    public AdminQuestCommand() {
        this.databaseHandler = DatabaseHandler.getInstance();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Objects.requireNonNull(configHandler.getStringAsync("command.no-player").join()));
            return true;
        }
        if (args.length == 0) return false;
        if (!args[0].equalsIgnoreCase("manage") && !args[0].equalsIgnoreCase("m")) return false;
        EditQuestsUtils.initInventory(player).thenAccept(v -> {
            if (EditQuestsUtils.allQuestsInventory.containsKey(player.getUniqueId())) {
                Bukkit.getScheduler().runTask(questSystem, () -> {
                    player.openInventory(EditQuestsUtils.allQuestsInventory.get(player.getUniqueId()));
                });
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });

        return true;
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

            if (clickedItem.getType().equals(Material.PLAYER_HEAD)) {
                handlePageSwitch(player, inv, displayName.content());
            }else if(clickedItem.getType().equals(Material.PAPER)){
                allQuestsInventory(e, player, playerId);
            }

        } catch (Exception ignored) {
            // Exception ignored, because it is not necessary to handle it (Only happens if player spam accepts EditQuestsUtils.quests)
        }
    }

    public void handlePageSwitch(Player player, Inventory inv, String content) {
        CompletableFuture<String> nextPageNameFuture = configHandler.getStringAsync("inventory.next-page.display-name");;
        CompletableFuture<String> prevPageNameFuture = configHandler.getStringAsync("inventory.prev-page.display-name");
        var successSoundFuture = configHandler.getStringAsync("inventory.switch-page.success.sound");
        var successVolumeFuture = configHandler.getStringAsync("inventory.switch-page.success.volume");
        var successPitchFuture = configHandler.getStringAsync("inventory.switch-page.success.pitch");
        var errorSoundFuture = configHandler.getStringAsync("inventory.switch-page.error.sound");
        var errorVolumeFuture = configHandler.getStringAsync("inventory.switch-page.error.volume");
        var errorPitchFuture = configHandler.getStringAsync("inventory.switch-page.error.pitch");
        CompletableFuture.allOf(
                nextPageNameFuture, prevPageNameFuture,
                successSoundFuture, successVolumeFuture,
                successPitchFuture, errorSoundFuture, errorVolumeFuture, errorPitchFuture
        ).thenAccept(x -> {
            var nextPageName = nextPageNameFuture.join();
            var prevPageName = prevPageNameFuture.join();
            var successSound = successSoundFuture.join();
            var successVolume = successVolumeFuture.join();
            var successPitch = successPitchFuture.join();
            var errorSound = errorSoundFuture.join();
            var errorVolume = errorVolumeFuture.join();
            var errorPitch = errorPitchFuture.join();


            if (content.equals(nextPageName.replace("%page%", EditUtils.getNextPage(player, EditQuestsUtils.quests.size())))) {
                int currentPage = EditUtils.playerPage.get(player.getUniqueId());
                int totalPages = EditUtils.getMaxPage(EditQuestsUtils.quests.size());
                if (currentPage == totalPages) {
                    Bukkit.getScheduler().runTask(questSystem, () -> {
                        player.playSound(player.getLocation(), Sound.valueOf(errorSound), Float.parseFloat(errorVolume), Float.parseFloat(errorPitch));
                    });
                    return;
                }
                Bukkit.getScheduler().runTask(questSystem, () -> {
                    player.playSound(player.getLocation(), Sound.valueOf(successSound), Float.parseFloat(successVolume), Float.parseFloat(successPitch));
                });
                EditUtils.playerPage.put(player.getUniqueId(), currentPage + 1);
                EditQuestsUtils.refreshInventory(EditQuestsUtils.quests, inv, player);
            } else if (content.equals(prevPageName.replace("%page%", EditUtils.getPrevPage(player)))) {
                int currentPage = EditUtils.playerPage.get(player.getUniqueId());
                if (currentPage == 1) {
                    Bukkit.getScheduler().runTask(questSystem, () -> {
                        player.playSound(player.getLocation(), Sound.valueOf(errorSound), Float.parseFloat(errorVolume), Float.parseFloat(errorPitch));
                    });
                    return;
                }
                Bukkit.getScheduler().runTask(questSystem, () -> {
                    player.playSound(player.getLocation(), Sound.valueOf(successSound), Float.parseFloat(successVolume), Float.parseFloat(successPitch));
                });

                EditUtils.playerPage.put(player.getUniqueId(), currentPage - 1);
                EditQuestsUtils.refreshInventory(EditQuestsUtils.quests, inv, player);
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
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
                    handleQuestItemDelete(player, displayName, inv);
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
            EditQuestsUtils.editQuestInventory(quest, player).thenAccept(inv -> {
                Bukkit.getScheduler().runTask(questSystem, () -> {
                    player.closeInventory();
                    player.openInventory(inv);
                });
            });
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });

    }

    private void handleQuestItemDelete(Player player, TextComponent displayName, Inventory inv) {
        var questFuture = databaseHandler.getQuestByNameAsync(displayName.content());
        CompletableFuture<String> deleteSoundFuture = configHandler.getStringAsync("quest-manager.lore-entries.delete.sound");
        CompletableFuture<Integer> deleteVolumeFuture = configHandler.getIntAsync("quest-manager.lore-entries.delete.volume");
        CompletableFuture<Integer> deletePitchFuture = configHandler.getIntAsync("quest-manager.lore-entries.delete.pitch");
        CompletableFuture.allOf(
                questFuture, deleteSoundFuture, deleteVolumeFuture, deletePitchFuture
        ).thenAccept(x -> {
            var quest = questFuture.join();
            var deleteSound = deleteSoundFuture.join();
            var deleteVolume = deleteVolumeFuture.join();
            var deletePitch = deletePitchFuture.join();
            if (quest == null) return;
            databaseHandler.deleteAsync(quest);
            EditQuestsUtils.quests.remove(quest);
            Bukkit.getScheduler().runTask(questSystem, () -> {
                player.playSound(player.getLocation(), Sound.valueOf(deleteSound), deleteVolume, deletePitch);
                player.sendMessage("Quest deleted");
                EditQuestsUtils.refreshInventory(EditQuestsUtils.quests, inv, player);
            });
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }
}
