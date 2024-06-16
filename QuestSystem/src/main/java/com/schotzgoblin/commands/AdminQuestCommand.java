package com.schotzgoblin.commands;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.database.Quest;
import com.schotzgoblin.database.Reward;
import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.QuestManager;
import com.schotzgoblin.main.QuestSystem;
import com.schotzgoblin.utils.EditQuestsUtils;
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


            if (content.equals(nextPageName.replace("%page%", EditQuestsUtils.getNextPage(player)))) {
                int currentPage = EditQuestsUtils.playerPage.get(player.getUniqueId());
                int totalPages = EditQuestsUtils.getMaxPage();
                if (currentPage == totalPages) {
                    Bukkit.getScheduler().runTask(questSystem, () -> {
                        player.playSound(player.getLocation(), Sound.valueOf(errorSound), Float.parseFloat(errorVolume), Float.parseFloat(errorPitch));
                    });
                    return;
                }
                Bukkit.getScheduler().runTask(questSystem, () -> {
                    player.playSound(player.getLocation(), Sound.valueOf(successSound), Float.parseFloat(successVolume), Float.parseFloat(successPitch));
                });
                EditQuestsUtils.playerPage.put(player.getUniqueId(), currentPage + 1);
                EditQuestsUtils.refreshInventory(EditQuestsUtils.quests, inv, player);
            } else if (content.equals(prevPageName.replace("%page%", EditQuestsUtils.getPrevPage(player)))) {
                int currentPage = EditQuestsUtils.playerPage.get(player.getUniqueId());
                if (currentPage == 1) {
                    Bukkit.getScheduler().runTask(questSystem, () -> {
                        player.playSound(player.getLocation(), Sound.valueOf(errorSound), Float.parseFloat(errorVolume), Float.parseFloat(errorPitch));
                    });
                    return;
                }
                Bukkit.getScheduler().runTask(questSystem, () -> {
                    player.playSound(player.getLocation(), Sound.valueOf(successSound), Float.parseFloat(successVolume), Float.parseFloat(successPitch));
                });

                EditQuestsUtils.playerPage.put(player.getUniqueId(), currentPage - 1);
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
        CompletableFuture<String> editTitleFuture = configHandler.getStringAsync("quest-manager.quest.edit.title");
        CompletableFuture<String> editColourFuture = configHandler.getStringAsync("quest-manager.quest.edit.colour");
        var questFuture = databaseHandler.getQuestByNameAsync(displayName.content());

        CompletableFuture.allOf(editTitleFuture, questFuture, editColourFuture).thenAccept(x -> {
            var editTitle = editTitleFuture.join();
            var editColour = editColourFuture.join();
            var quest = questFuture.join();
            var inv = createInventory(editTitle, editColour, 27);
            editQuestInventory(inv, quest, player).thenAccept(v -> {
                EditQuestsUtils.editQuestInventory.put(player.getUniqueId(), inv);
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

    public Inventory createInventory(String title, String colour, int size) {
        return Bukkit.createInventory(null, size, Component.text(title, TextColor.fromCSSHexString(colour)));
    }

    private CompletableFuture<Void> editQuestInventory(Inventory inv, Quest quest, Player player) {
        CompletableFuture<String> nameTitleFuture = configHandler.getStringAsync("inventory.edit-quest.name.title");
        CompletableFuture<String> nameColourFuture = configHandler.getStringAsync("inventory.edit-quest.name.colour");
        CompletableFuture<String> nameMaterialFuture = configHandler.getStringAsync("inventory.edit-quest.name.material");

        CompletableFuture<String> descriptionTitleFuture = configHandler.getStringAsync("inventory.edit-quest.description.title");
        CompletableFuture<String> descriptionColourFuture = configHandler.getStringAsync("inventory.edit-quest.description.colour");
        CompletableFuture<String> descriptionMaterialFuture = configHandler.getStringAsync("inventory.edit-quest.description.material");

        CompletableFuture<String> objectiveTitleFuture = configHandler.getStringAsync("inventory.edit-quest.objective.title");
        CompletableFuture<String> objectiveColourFuture = configHandler.getStringAsync("inventory.edit-quest.objective.colour");
        CompletableFuture<String> objectiveMaterialFuture = configHandler.getStringAsync("inventory.edit-quest.objective.material");

        CompletableFuture<String> timeLimitTitleFuture = configHandler.getStringAsync("inventory.edit-quest.time-limit.title");
        CompletableFuture<String> timeLimitColourFuture = configHandler.getStringAsync("inventory.edit-quest.time-limit.colour");
        CompletableFuture<String> timeLimitMaterialFuture = configHandler.getStringAsync("inventory.edit-quest.time-limit.material");

        CompletableFuture<String> rewardsTitleFuture = configHandler.getStringAsync("inventory.edit-quest.rewards.title");
        CompletableFuture<String> rewardsColourFuture = configHandler.getStringAsync("inventory.edit-quest.rewards.colour");
        CompletableFuture<String> rewardsMaterialFuture = configHandler.getStringAsync("inventory.edit-quest.rewards.material");

        CompletableFuture<String> saveTitleFuture = configHandler.getStringAsync("inventory.edit-quest.save.title");
        CompletableFuture<String> saveColourFuture = configHandler.getStringAsync("inventory.edit-quest.save.colour");
        CompletableFuture<String> saveMaterialFuture = configHandler.getStringAsync("inventory.edit-quest.save.material");

        CompletableFuture<String> cancelTitleFuture = configHandler.getStringAsync("inventory.edit-quest.cancel.title");
        CompletableFuture<String> cancelColourFuture = configHandler.getStringAsync("inventory.edit-quest.cancel.colour");
        CompletableFuture<String> cancelMaterialFuture = configHandler.getStringAsync("inventory.edit-quest.cancel.material");

        return CompletableFuture.allOf(
                nameTitleFuture, nameColourFuture, nameMaterialFuture,
                descriptionTitleFuture, descriptionColourFuture, descriptionMaterialFuture,
                objectiveTitleFuture, objectiveColourFuture, objectiveMaterialFuture,
                timeLimitTitleFuture, timeLimitColourFuture, timeLimitMaterialFuture,
                rewardsTitleFuture, rewardsColourFuture, rewardsMaterialFuture,
                saveTitleFuture, saveColourFuture, saveMaterialFuture,
                cancelTitleFuture, cancelColourFuture, cancelMaterialFuture
        ).thenAcceptAsync(v -> {
            try {
                String nameTitle = nameTitleFuture.join();
                String nameColour = nameColourFuture.join();
                String nameMaterialName = nameMaterialFuture.join();

                String descriptionTitle = descriptionTitleFuture.join();
                String descriptionColour = descriptionColourFuture.join();
                String descriptionMaterialName = descriptionMaterialFuture.join();

                String objectiveTitle = objectiveTitleFuture.join();
                String objectiveColour = objectiveColourFuture.join();
                String objectiveMaterialName = objectiveMaterialFuture.join();

                String timeLimitTitle = timeLimitTitleFuture.join();
                String timeLimitColour = timeLimitColourFuture.join();
                String timeLimitMaterialName = timeLimitMaterialFuture.join();

                String rewardsTitle = rewardsTitleFuture.join();
                String rewardsColour = rewardsColourFuture.join();
                String rewardsMaterialName = rewardsMaterialFuture.join();

                String saveTitle = saveTitleFuture.join();
                String saveColour = saveColourFuture.join();
                String saveMaterialName = saveMaterialFuture.join();

                String cancelTitle = cancelTitleFuture.join();
                String cancelColour = cancelColourFuture.join();
                String cancelMaterialName = cancelMaterialFuture.join();

                ItemStack nameItem = createItem(nameMaterialName, nameTitle, nameColour);
                ItemStack descriptionItem = createItem(descriptionMaterialName, descriptionTitle, descriptionColour);
                ItemStack objectiveItem = createItem(objectiveMaterialName, objectiveTitle, objectiveColour);
                ItemStack timeLimitItem = createItem(timeLimitMaterialName, timeLimitTitle, timeLimitColour);
                ItemStack rewardsItem = createItem(rewardsMaterialName, rewardsTitle, rewardsColour);
                ItemStack saveItem = createItem(saveMaterialName, saveTitle, saveColour);
                ItemStack cancelItem = createItem(cancelMaterialName, cancelTitle, cancelColour);

                inv.setItem(0, nameItem);
                inv.setItem(4, descriptionItem);
                inv.setItem(8, timeLimitItem);
                inv.setItem(11, objectiveItem);
                inv.setItem(15, rewardsItem);
                inv.setItem(21, saveItem);
                inv.setItem(23, cancelItem);

                questManager.finaliseInventory(inv,27);
            } catch (CompletionException ex) {
                ex.printStackTrace();
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    private ItemStack createItem(String materialName, String displayName, String colorHex) {
        Material material = Material.getMaterial(materialName);
        if (material == null) {
            throw new IllegalArgumentException("Invalid material name: " + materialName);
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(displayName, TextColor.fromCSSHexString(colorHex)));
        item.setItemMeta(meta);
        return item;
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
