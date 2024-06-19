package com.schotzgoblin.listener.edit;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.database.Quest;
import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.QuestSystem;
import com.schotzgoblin.utils.*;
import com.schotzgoblin.utils.edit.EditObjectivesUtils;
import com.schotzgoblin.utils.edit.EditQuestsUtils;
import com.schotzgoblin.utils.edit.EditRewardsUtils;
import com.schotzgoblin.utils.edit.EditUtils;
import net.kyori.adventure.text.TextComponent;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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
    //Normally I would make all those edit classes generic, so I have to do it once, but unfortunately I don't have the time to do it now

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

    @EventHandler
    public void onInventoryClick2(InventoryClickEvent e) {
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
            databaseHandler.deleteAllQuestRewardsAsync(quest);
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
            if (EditRewardsUtils.allRewardsInventory.containsKey(player.getUniqueId())) {
                Bukkit.getScheduler().runTask(questSystem, () -> {
                    player.closeInventory();
                    player.openInventory(EditRewardsUtils.allRewardsInventory.get(player.getUniqueId()));
                });
            }
        });
    }

    private void editQuestObjective(Player player, Inventory inv, Quest quest) {
        EditObjectivesUtils.initInventory(player, quest).thenAccept(v -> {
            if (EditObjectivesUtils.allObjectivesInventory.containsKey(player.getUniqueId())) {
                Bukkit.getScheduler().runTask(questSystem, () -> {
                    player.closeInventory();
                    player.openInventory(EditObjectivesUtils.allObjectivesInventory.get(player.getUniqueId()));
                });
            }
        });
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

        var createQuestNameDefaultValueFuture = configHandler.getStringAsync("inventory.create-quest.default.name").join();
        var createQuestErrorDefaultValueFuture = configHandler.getStringAsync("inventory.create-quest.error-message").join();
        if (quest.getName().equals(createQuestNameDefaultValueFuture)) {
            player.sendMessage(createQuestErrorDefaultValueFuture);
            return;
        }
        EditQuestsUtils.editQuestInventory.remove(player.getUniqueId());
        EditQuestsUtils.editingQuest.remove(player.getUniqueId());
        if (quest.getId() == 0) {
            databaseHandler.saveAsync(quest).thenAccept(v -> {
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
            return;
        } else {
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
}
