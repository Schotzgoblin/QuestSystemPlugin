package com.schotzgoblin.listener.edit;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.database.Quest;
import com.schotzgoblin.database.Objective;
import com.schotzgoblin.database.Reward;
import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.QuestSystem;
import com.schotzgoblin.utils.edit.EditQuestsUtils;
import com.schotzgoblin.utils.edit.EditObjectivesUtils;
import com.schotzgoblin.utils.edit.EditRewardsUtils;
import com.schotzgoblin.utils.edit.EditUtils;
import com.schotzgoblin.utils.Utils;
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

public class EditObjectivesListener implements Listener {
    private final QuestSystem questSystem = QuestSystem.getInstance();
    private final DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
    private final ConfigHandler configHandler = ConfigHandler.getInstance();
    //Normally I would make all those edit classes generic, so I have to do it once, but unfortunately I don't have the time to do it now
    public EditObjectivesListener() {
    }

    @EventHandler
    public void onInventoryClick2(InventoryClickEvent e) {
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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        UUID playerId = player.getUniqueId();
        if (!EditObjectivesUtils.editObjectiveInventory.containsKey(playerId)) return;
        Inventory inv = EditObjectivesUtils.editObjectiveInventory.get(playerId);
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

            editObjectiveAttributeInventory(inv, player, playerId, displayName, clickedItem);

        } catch (Exception ignored) {
            // Exception ignored, because it is not necessary to handle it (Only happens if player spam accepts EditQuestsUtils.quests)
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
            EditObjectivesUtils.editObjectivesInventory(objective, player).thenAccept(inv -> {
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
                    handleQuestItemDelete(player, displayName, inv);
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
            databaseHandler.updateAsync(quest).thenAccept(v -> {
                EditObjectivesUtils.refreshObjectives(quest).join();
            });
        });
    }

    private void handleQuestItemEdit(Player player, TextComponent displayName) {
        var objectiveFuture = databaseHandler.getObjectiveByNameAsync(displayName.content());

        CompletableFuture.allOf(objectiveFuture).thenAccept(x -> {
            var objective = objectiveFuture.join();
            EditObjectivesUtils.editingObjective.put(player.getUniqueId(), objective);
            EditObjectivesUtils.editObjectivesInventory(objective, player).thenAccept(inv -> {
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
        var objectiveFuture = databaseHandler.getObjectiveByNameAsync(displayName.content());
        CompletableFuture<String> deleteSoundFuture = configHandler.getStringAsync("quest-manager.lore-entries.delete.sound");
        CompletableFuture<Integer> deleteVolumeFuture = configHandler.getIntAsync("quest-manager.lore-entries.delete.volume");
        CompletableFuture<Integer> deletePitchFuture = configHandler.getIntAsync("quest-manager.lore-entries.delete.pitch");
        CompletableFuture.allOf(
                objectiveFuture, deleteSoundFuture, deleteVolumeFuture, deletePitchFuture
        ).thenAccept(x -> {
            var objective = objectiveFuture.join();
            var deleteSound = deleteSoundFuture.join();
            var deleteVolume = deleteVolumeFuture.join();
            var deletePitch = deletePitchFuture.join();
            if (objective == null) return;
            databaseHandler.deleteAsync(objective);
            EditObjectivesUtils.objectives.remove(objective);
            Bukkit.getScheduler().runTask(questSystem, () -> {
                player.playSound(player.getLocation(), Sound.valueOf(deleteSound), deleteVolume, deletePitch);
                EditObjectivesUtils.refreshInventory(EditObjectivesUtils.objectives, EditQuestsUtils.editingQuest.get(player.getUniqueId()), inv, player);
            });
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }


    private void editObjectiveAttributeInventory(Inventory inv, Player player, UUID playerId, TextComponent displayName, ItemStack clickedItem) {
        CompletableFuture<String> nameMaterialFuture = configHandler.getStringAsync("inventory.edit-objective.name.material");
        CompletableFuture<String> amountMaterialFuture = configHandler.getStringAsync("inventory.edit-objective.amount.material");
        CompletableFuture<String> objectiveTypeIdMaterialFuture = configHandler.getStringAsync("inventory.edit-objective.objectiveTypeId.material");
        CompletableFuture<String> valueMaterialFuture = configHandler.getStringAsync("inventory.edit-objective.value.material");
        CompletableFuture<String> saveMaterialFuture = configHandler.getStringAsync("inventory.edit-objective.save.material");
        CompletableFuture<String> cancelMaterialFuture = configHandler.getStringAsync("inventory.edit-objective.cancel.material");

        CompletableFuture.allOf(
                nameMaterialFuture,
                amountMaterialFuture,
                objectiveTypeIdMaterialFuture,
                valueMaterialFuture,
                saveMaterialFuture,
                cancelMaterialFuture
        ).thenAcceptAsync(v -> {
            try {
                Objective objective = EditObjectivesUtils.editingObjective.get(playerId);

                String nameMaterialName = nameMaterialFuture.join();
                String amountMaterialName = amountMaterialFuture.join();
                String objectiveTypeIdMaterialName = objectiveTypeIdMaterialFuture.join();
                String valueMaterialName = valueMaterialFuture.join();

                String saveMaterialName = saveMaterialFuture.join();
                String cancelMaterialName = cancelMaterialFuture.join();
                if (clickedItem.getType().equals(Material.getMaterial(nameMaterialName))) {
                    editObjectiveName(player, inv, objective);
                } else if (clickedItem.getType().equals(Material.getMaterial(amountMaterialName))) {
                    editObjectiveAmount(player, inv, objective);
                } else if (clickedItem.getType().equals(Material.getMaterial(objectiveTypeIdMaterialName))) {
                    editObjectiveTypeId(player, inv, objective);
                } else if (clickedItem.getType().equals(Material.getMaterial(valueMaterialName))) {
                    editObjectiveValue(player, inv, objective);
                } else if (clickedItem.getType().equals(Material.getMaterial(saveMaterialName))) {
                    saveQuest(player, inv, EditQuestsUtils.editingQuest.get(player.getUniqueId()), objective);
                } else if (clickedItem.getType().equals(Material.getMaterial(cancelMaterialName))) {
                    cancelEdit(player, inv, EditQuestsUtils.editingQuest.get(player.getUniqueId()));
                }
            } catch (CompletionException ex) {
                ex.printStackTrace();
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    private void editObjectiveValue(Player player, Inventory inv, Objective objective) {
        CompletableFuture<String> valueChangeMsgFuture = configHandler.getStringAsync("inventory.edit-objective.value.change-message");
        valueChangeMsgFuture.thenAcceptAsync(valueChangeMsg -> {
            Bukkit.getScheduler().runTask(questSystem, () -> {
                new AnvilGUI.Builder()
                        .onClick((slot, stateSnapshot) -> {
                            if (slot != AnvilGUI.Slot.OUTPUT) {
                                return Collections.emptyList();
                            }
                            return Arrays.asList(
                                    AnvilGUI.ResponseAction.close(),
                                    AnvilGUI.ResponseAction.run(() -> objective.setValue(stateSnapshot.getText()))
                            );
                        })
                        .onClose(player1 -> {
                            EditObjectivesUtils.editObjectivesInventory(objective, player).thenAccept(v -> {
                                Bukkit.getScheduler().runTask(questSystem, () -> {
                                    player.openInventory(EditObjectivesUtils.editObjectiveInventory.get(player.getUniqueId()));
                                });
                            });
                        })
                        .text(objective.getValue())
                        .title(valueChangeMsg)
                        .plugin(questSystem)
                        .open(player);
            });
        });
    }

    private void editObjectiveTypeId(Player player, Inventory inv, Objective objective) {
        CompletableFuture<String> objectiveTypeIdChangeMsgFuture = configHandler.getStringAsync("inventory.edit-objective.type.change-message");
        objectiveTypeIdChangeMsgFuture.thenAcceptAsync(objectiveTypeIdChangeMsg -> {
            Bukkit.getScheduler().runTask(questSystem, () -> {
                new AnvilGUI.Builder()
                        .onClick((slot, stateSnapshot) -> {
                            if (slot != AnvilGUI.Slot.OUTPUT) {
                                return Collections.emptyList();
                            }
                            return Arrays.asList(
                                    AnvilGUI.ResponseAction.close(),
                                    AnvilGUI.ResponseAction.run(() -> objective.setType(stateSnapshot.getText()))
                            );
                        })
                        .onClose(player1 -> {
                            EditObjectivesUtils.editObjectivesInventory(objective, player).thenAccept(v -> {
                                Bukkit.getScheduler().runTask(questSystem, () -> {
                                    player.openInventory(EditObjectivesUtils.editObjectiveInventory.get(player.getUniqueId()));
                                });
                            });
                        })
                        .text(objective.getType())
                        .title(objectiveTypeIdChangeMsg)
                        .plugin(questSystem)
                        .open(player);
            });
        });
    }

    private void editObjectiveAmount(Player player, Inventory inv, Objective objective) {
        CompletableFuture<String> amountChangeMsgFuture = configHandler.getStringAsync("inventory.edit-objective.amount.change-message");
        amountChangeMsgFuture.thenAcceptAsync(amountChangeMsg -> {
            Bukkit.getScheduler().runTask(questSystem, () -> {
                new AnvilGUI.Builder()
                        .onClick((slot, stateSnapshot) -> {
                            if (slot != AnvilGUI.Slot.OUTPUT) {
                                return Collections.emptyList();
                            }
                            return Arrays.asList(
                                    AnvilGUI.ResponseAction.close(),
                                    AnvilGUI.ResponseAction.run(() -> objective.setCount(Integer.parseInt(stateSnapshot.getText())))
                            );
                        })
                        .onClose(player1 -> {
                            EditObjectivesUtils.editObjectivesInventory(objective, player).thenAccept(v -> {
                                Bukkit.getScheduler().runTask(questSystem, () -> {
                                    player.openInventory(EditObjectivesUtils.editObjectiveInventory.get(player.getUniqueId()));
                                });
                            });
                        })
                        .text(objective.getCount() + "")
                        .title(amountChangeMsg)
                        .plugin(questSystem)
                        .open(player);
            });
        });
    }

    private void editObjectiveName(Player player, Inventory inv, Objective objective) {
        CompletableFuture<String> nameChangeMsgFuture = configHandler.getStringAsync("inventory.edit-objective.name.change-message");
        nameChangeMsgFuture.thenAcceptAsync(nameChangeMsg -> {
            Bukkit.getScheduler().runTask(questSystem, () -> {
                new AnvilGUI.Builder()
                        .onClick((slot, stateSnapshot) -> {
                            if (slot != AnvilGUI.Slot.OUTPUT) {
                                return Collections.emptyList();
                            }
                            return Arrays.asList(
                                    AnvilGUI.ResponseAction.close(),
                                    AnvilGUI.ResponseAction.run(() -> objective.setObjective(stateSnapshot.getText()))
                            );
                        })
                        .onClose(player1 -> {
                            EditObjectivesUtils.editObjectivesInventory(objective, player).thenAccept(v -> {
                                Bukkit.getScheduler().runTask(questSystem, () -> {
                                    player.openInventory(EditObjectivesUtils.editObjectiveInventory.get(player.getUniqueId()));
                                });
                            });
                        })
                        .text(objective.getObjective())
                        .title(nameChangeMsg)
                        .plugin(questSystem)
                        .open(player);
            });
        });
    }

    private void cancelEdit(Player player, Inventory inv, Quest quest) {
        EditObjectivesUtils.editObjectiveInventory.remove(player.getUniqueId());
        EditObjectivesUtils.editingObjective.remove(player.getUniqueId());
        EditObjectivesUtils.refreshObjectives(quest).thenAccept(v -> {
            Bukkit.getScheduler().runTask(questSystem, () -> {
                player.closeInventory();
                player.openInventory(EditObjectivesUtils.allObjectivesInventory.get(player.getUniqueId()));
            });
        });
    }

    private void saveQuest(Player player, Inventory inv, Quest quest, Objective objective) {
        var createRewardNameDefaultValue = configHandler.getStringAsync("inventory.create-objective.default.name").join();
        var createRewardErrorDefaultValue = configHandler.getStringAsync("inventory.create-objective.error-message").join();
        if (objective.getObjective().equals(createRewardNameDefaultValue)) {
            player.sendMessage(createRewardErrorDefaultValue);
            return;
        }
        EditObjectivesUtils.editObjectiveInventory.remove(player.getUniqueId());
        EditObjectivesUtils.editingObjective.remove(player.getUniqueId());

        if(objective.getId() == 0) {
            databaseHandler.saveAsync(objective).thenAccept(v -> {
                quest.setObjectiveId(objective.getId());
                quest.setObjective(objective);
                databaseHandler.updateAsync(quest).thenAccept(v2 -> {
                    EditObjectivesUtils.refreshObjectives(quest).thenAccept(v3 -> {
                        Bukkit.getScheduler().runTask(questSystem, () -> {
                            player.closeInventory();
                            player.openInventory(EditObjectivesUtils.allObjectivesInventory.get(player.getUniqueId()));
                        });
                    });
                });
            }).exceptionally(ex -> {
                ex.printStackTrace();
                return null;
            });
        }else{
            databaseHandler.updateAsync(objective).thenAccept(v -> {
                EditObjectivesUtils.refreshObjectives(quest).thenAccept(v2 -> {
                    Bukkit.getScheduler().runTask(questSystem, () -> {
                        player.closeInventory();
                        player.openInventory(EditObjectivesUtils.allObjectivesInventory.get(player.getUniqueId()));
                    });
                });
            }).exceptionally(ex -> {
                ex.printStackTrace();
                return null;
            });
        }

    }
}
