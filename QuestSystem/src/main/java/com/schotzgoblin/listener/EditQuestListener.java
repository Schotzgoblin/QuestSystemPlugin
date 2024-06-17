package com.schotzgoblin.listener;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.database.Quest;
import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.QuestSystem;
import com.schotzgoblin.utils.EditQuestsUtils;
import com.schotzgoblin.utils.EditRewardsUtils;
import com.schotzgoblin.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class EditQuestListener implements Listener {
    private final QuestSystem questSystem = QuestSystem.getInstance();
    private final DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
    private final ConfigHandler configHandler = ConfigHandler.getInstance();

    public EditQuestListener() {
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
            if (itemMeta == null || !(itemMeta.displayName() instanceof TextComponent displayName)) {
                return;
            }
            editQuestAttributeInventory(inv, player, playerId, displayName, clickedItem);

        } catch (Exception ignored) {
            // Exception ignored, because it is not necessary to handle it (Only happens if player spam accepts EditQuestsUtils.quests)
        }
    }

    private void editQuestAttributeInventory(Inventory inv, Player player, UUID playerId, TextComponent displayName, ItemStack clickedItem) {
        CompletableFuture<String> nameMaterialFuture = configHandler.getStringAsync("inventory.edit-quest.name.material");
        CompletableFuture<String> descriptionMaterialFuture = configHandler.getStringAsync("inventory.edit-quest.description.material");
        CompletableFuture<String> objectiveMaterialFuture = configHandler.getStringAsync("inventory.edit-quest.objective.material");
        CompletableFuture<String> timeLimitMaterialFuture = configHandler.getStringAsync("inventory.edit-quest.time-limit.material");
        CompletableFuture<String> rewardsMaterialFuture = configHandler.getStringAsync("inventory.edit-quest.rewards.material");
        CompletableFuture<String> saveMaterialFuture = configHandler.getStringAsync("inventory.edit-quest.save.material");
        CompletableFuture<String> cancelMaterialFuture = configHandler.getStringAsync("inventory.edit-quest.cancel.material");

        CompletableFuture.allOf(
                nameMaterialFuture,
                descriptionMaterialFuture,
                objectiveMaterialFuture,
                timeLimitMaterialFuture,
                rewardsMaterialFuture,
                saveMaterialFuture,
                cancelMaterialFuture
        ).thenAcceptAsync(v -> {
            try {
                Quest quest = EditQuestsUtils.editingQuest.get(playerId);
                String nameMaterialName = nameMaterialFuture.join();
                String descriptionMaterialName = descriptionMaterialFuture.join();
                String objectiveMaterialName = objectiveMaterialFuture.join();
                String timeLimitMaterialName = timeLimitMaterialFuture.join();
                String rewardsMaterialName = rewardsMaterialFuture.join();
                String saveMaterialName = saveMaterialFuture.join();
                String cancelMaterialName = cancelMaterialFuture.join();
                if (clickedItem.getType().equals(Material.getMaterial(nameMaterialName))) {
                    editQuestName(player, inv, quest);
                } else if (clickedItem.getType().equals(Material.getMaterial(descriptionMaterialName))) {
                    editQuestDescription(player, inv, quest);
                } else if (clickedItem.getType().equals(Material.getMaterial(objectiveMaterialName))) {
                    editQuestObjective(player, inv, quest);
                } else if (clickedItem.getType().equals(Material.getMaterial(timeLimitMaterialName))) {
                    editQuestTimeLimit(player, inv, quest);
                } else if (clickedItem.getType().equals(Material.getMaterial(rewardsMaterialName))) {
                    editQuestRewards(player, inv, quest);
                } else if (clickedItem.getType().equals(Material.getMaterial(saveMaterialName))) {
                    saveQuest(player, inv, quest);
                } else if (clickedItem.getType().equals(Material.getMaterial(cancelMaterialName))) {
                    cancelEdit(player, inv, quest);
                }
            } catch (CompletionException ex) {
                ex.printStackTrace();
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    private void editQuestRewards(Player player, Inventory inv, Quest quest) {
        EditRewardsUtils.initInventory(player, quest).thenAccept(v -> {
            if(EditRewardsUtils.allRewardsInventory.containsKey(player.getUniqueId())) {
                Bukkit.getScheduler().runTask(questSystem, () -> {
                    player.closeInventory();
                    player.openInventory(EditRewardsUtils.allRewardsInventory.get(player.getUniqueId()));
                });
            }
        });
    }

    private void editQuestObjective(Player player, Inventory inv, Quest quest) {

    }

    private void editQuestTimeLimit(Player player, Inventory inv, Quest quest) {
        CompletableFuture<String> timeLimitChangeMsgFuture = configHandler.getStringAsync("inventory.edit-quest.time-limit.change-message");
        timeLimitChangeMsgFuture.thenAcceptAsync(timeLimitChangeMsg -> {
            Bukkit.getScheduler().runTask(questSystem, () -> {
                new AnvilGUI.Builder()
                        .onClick((slot, stateSnapshot) -> {
                            if (slot != AnvilGUI.Slot.OUTPUT) {
                                return Collections.emptyList();
                            }
                            return Arrays.asList(
                                    AnvilGUI.ResponseAction.close(),
                                    AnvilGUI.ResponseAction.run(() -> quest.setTimeLimit(Utils.getSecondsFromTimeString(stateSnapshot.getText())))
                            );
                        })
                        .onClose(player1 -> {
                            EditQuestsUtils.editQuestInventory(quest, player).thenAccept(v -> {
                                Bukkit.getScheduler().runTask(questSystem, () -> {
                                    player.openInventory(EditQuestsUtils.editQuestInventory.get(player.getUniqueId()));
                                });
                            });
                        })
                        .text(Utils.getTimeStringFromSecs(quest.getTimeLimit()))
                        .title(timeLimitChangeMsg)
                        .plugin(questSystem)
                        .open(player);
            });
        });
    }

    private void editQuestDescription(Player player, Inventory inv, Quest quest) {
        CompletableFuture<String> descriptionChangeMsgFuture = configHandler.getStringAsync("inventory.edit-quest.description.change-message");
        descriptionChangeMsgFuture.thenAcceptAsync(descriptionChangeMsg -> {
            Bukkit.getScheduler().runTask(questSystem, () -> {
                new AnvilGUI.Builder()
                        .onClick((slot, stateSnapshot) -> {
                            if (slot != AnvilGUI.Slot.OUTPUT) {
                                return Collections.emptyList();
                            }
                            return Arrays.asList(
                                    AnvilGUI.ResponseAction.close(),
                                    AnvilGUI.ResponseAction.run(() -> quest.setDescription(stateSnapshot.getText()))
                            );
                        })
                        .onClose(player1 -> {
                            EditQuestsUtils.editQuestInventory(quest, player).thenAccept(v -> {
                                Bukkit.getScheduler().runTask(questSystem, () -> {
                                    player.openInventory(EditQuestsUtils.editQuestInventory.get(player.getUniqueId()));
                                });
                            });
                        })
                        .text(quest.getDescription())
                        .title(descriptionChangeMsg)
                        .plugin(questSystem)
                        .open(player);
            });
        });
    }

    private void editQuestName(Player player, Inventory inv, Quest quest) {
        CompletableFuture<String> nameChangeMsgFuture = configHandler.getStringAsync("inventory.edit-quest.name.change-message");
        nameChangeMsgFuture.thenAcceptAsync(nameChangeMsg -> {
            Bukkit.getScheduler().runTask(questSystem, () -> {
                new AnvilGUI.Builder()
                        .onClick((slot, stateSnapshot) -> {
                            if (slot != AnvilGUI.Slot.OUTPUT) {
                                return Collections.emptyList();
                            }
                            return Arrays.asList(
                                    AnvilGUI.ResponseAction.close(),
                                    AnvilGUI.ResponseAction.run(() -> quest.setName(stateSnapshot.getText()))
                            );
                        })
                        .onClose(player1 -> {
                            EditQuestsUtils.editQuestInventory(quest, player).thenAccept(v -> {
                                Bukkit.getScheduler().runTask(questSystem, () -> {
                                    player.sendMessage(quest.getName());
                                    player.openInventory(EditQuestsUtils.editQuestInventory.get(player.getUniqueId()));
                                });
                            });
                        })
                        .text(quest.getName())
                        .title(nameChangeMsg)
                        .plugin(questSystem)
                        .open(player);
            });
        });
    }

    private void cancelEdit(Player player, Inventory inv, Quest quest) {
        EditQuestsUtils.editQuestInventory.remove(player.getUniqueId());
        EditQuestsUtils.editingQuest.remove(player.getUniqueId());
        EditQuestsUtils.refreshQuests().thenAccept(v -> {
            Bukkit.getScheduler().runTask(questSystem, () -> {
                player.closeInventory();
                player.openInventory(EditQuestsUtils.allQuestsInventory.get(player.getUniqueId()));
            });
        });
    }

    private void saveQuest(Player player, Inventory inv, Quest quest) {
        EditQuestsUtils.editQuestInventory.remove(player.getUniqueId());
        EditQuestsUtils.editingQuest.remove(player.getUniqueId());
        databaseHandler.updateAsync(quest).thenAccept(v -> {
            EditQuestsUtils.refreshQuests().thenAccept(v2 -> {
                Bukkit.getScheduler().runTask(questSystem, () -> {
                    player.closeInventory();
                    player.openInventory(EditQuestsUtils.allQuestsInventory.get(player.getUniqueId()));
                });
            });
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

}
