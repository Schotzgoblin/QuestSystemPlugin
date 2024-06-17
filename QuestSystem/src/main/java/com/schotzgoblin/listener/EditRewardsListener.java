package com.schotzgoblin.listener;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.database.Quest;
import com.schotzgoblin.database.Reward;
import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.QuestSystem;
import com.schotzgoblin.utils.EditQuestsUtils;
import com.schotzgoblin.utils.EditRewardsUtils;
import com.schotzgoblin.utils.EditUtils;
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

public class EditRewardsListener implements Listener {
    private final QuestSystem questSystem = QuestSystem.getInstance();
    private final DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
    private final ConfigHandler configHandler = ConfigHandler.getInstance();

    public EditRewardsListener() {
    }

    @EventHandler
    public void onInventoryClick2(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        UUID playerId = player.getUniqueId();
        if (!EditRewardsUtils.allRewardsInventory.containsKey(playerId)) return;
        Inventory inv = EditRewardsUtils.allRewardsInventory.get(playerId);
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
            } else if (clickedItem.getType().equals(Material.EMERALD)) {
                allRewardsInventory(e, player, playerId);
            }
        } catch (Exception ignored) {

        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        UUID playerId = player.getUniqueId();
        if (!EditRewardsUtils.editRewardInventory.containsKey(playerId)) return;
        Inventory inv = EditRewardsUtils.editRewardInventory.get(playerId);
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

            editRewardAttributeInventory(inv, player, playerId, displayName, clickedItem);

        } catch (Exception ignored) {
            // Exception ignored, because it is not necessary to handle it (Only happens if player spam accepts EditQuestsUtils.quests)
        }
    }

    public void handlePageSwitch(Player player, Inventory inv, String content) {
        CompletableFuture<String> nextPageNameFuture = configHandler.getStringAsync("inventory.next-page.display-name");
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


            if (content.equals(nextPageName.replace("%page%", EditUtils.getNextPage(player, EditRewardsUtils.rewards.size())))) {
                int currentPage = EditUtils.playerPage.get(player.getUniqueId());
                int totalPages = EditUtils.getMaxPage(EditRewardsUtils.rewards.size());
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
                EditRewardsUtils.refreshInventory(EditRewardsUtils.rewards, EditQuestsUtils.editingQuest.get(player.getUniqueId()), inv, player);
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
                EditRewardsUtils.refreshInventory(EditRewardsUtils.rewards, EditQuestsUtils.editingQuest.get(player.getUniqueId()), inv, player);
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });

    }

    private void allRewardsInventory(InventoryClickEvent e, Player player, UUID playerId) {
        Inventory inv = EditRewardsUtils.allRewardsInventory.get(playerId);
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
                    handleRewardChange(player, displayName);
                }
            }

        } catch (Exception ignored) {
            // Exception ignored, because it is not necessary to handle it (Only happens if player spam accepts EditQuestsUtils.quests)
        }
    }

    private void handleRewardChange(Player player, TextComponent displayName) {
        var rewardFuture = databaseHandler.getRewardByNameAsync(displayName.content());

        CompletableFuture.allOf(rewardFuture).thenAccept(x -> {
            var reward = rewardFuture.join();
            var quest = EditQuestsUtils.editingQuest.get(player.getUniqueId());
            var questRewardFuture = databaseHandler.getQuestRewardAsync(quest, reward);
            questRewardFuture.thenAccept(questReward -> {
                if (questReward == null) {
                    databaseHandler.addQuestReward(quest, reward).join();
                } else {
                    databaseHandler.deleteAsync(questReward).join();
                }
                EditRewardsUtils.refreshRewards(quest).join();
            });

        });
    }

    private void handleQuestItemEdit(Player player, TextComponent displayName) {
        var rewardFuture = databaseHandler.getRewardByNameAsync(displayName.content());

        CompletableFuture.allOf(rewardFuture).thenAccept(x -> {
            var reward = rewardFuture.join();
            EditRewardsUtils.editingReward.put(player.getUniqueId(), reward);
            EditRewardsUtils.editRewardsInventory(reward, player).thenAccept(inv -> {
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
        var rewardFuture = databaseHandler.getRewardByNameAsync(displayName.content());
        CompletableFuture<String> deleteSoundFuture = configHandler.getStringAsync("quest-manager.lore-entries.delete.sound");
        CompletableFuture<Integer> deleteVolumeFuture = configHandler.getIntAsync("quest-manager.lore-entries.delete.volume");
        CompletableFuture<Integer> deletePitchFuture = configHandler.getIntAsync("quest-manager.lore-entries.delete.pitch");
        CompletableFuture.allOf(
                rewardFuture, deleteSoundFuture, deleteVolumeFuture, deletePitchFuture
        ).thenAccept(x -> {
            var reward = rewardFuture.join();
            var deleteSound = deleteSoundFuture.join();
            var deleteVolume = deleteVolumeFuture.join();
            var deletePitch = deletePitchFuture.join();
            if (reward == null) return;
            databaseHandler.deleteAsync(reward);
            EditRewardsUtils.rewards.remove(reward);
            Bukkit.getScheduler().runTask(questSystem, () -> {
                player.playSound(player.getLocation(), Sound.valueOf(deleteSound), deleteVolume, deletePitch);
                player.sendMessage("Quest deleted");
                EditRewardsUtils.refreshInventory(EditRewardsUtils.rewards, EditQuestsUtils.editingQuest.get(player.getUniqueId()), inv, player);
            });
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }


    private void editRewardAttributeInventory(Inventory inv, Player player, UUID playerId, TextComponent displayName, ItemStack clickedItem) {
        CompletableFuture<String> nameMaterialFuture = configHandler.getStringAsync("inventory.edit-reward.name.material");
        CompletableFuture<String> amountMaterialFuture = configHandler.getStringAsync("inventory.edit-reward.amount.material");
        CompletableFuture<String> rewardTypeIdMaterialFuture = configHandler.getStringAsync("inventory.edit-reward.rewardTypeId.material");
        CompletableFuture<String> valueMaterialFuture = configHandler.getStringAsync("inventory.edit-reward.value.material");
        CompletableFuture<String> saveMaterialFuture = configHandler.getStringAsync("inventory.edit-reward.save.material");
        CompletableFuture<String> cancelMaterialFuture = configHandler.getStringAsync("inventory.edit-reward.cancel.material");

        CompletableFuture.allOf(
                nameMaterialFuture,
                amountMaterialFuture,
                rewardTypeIdMaterialFuture,
                valueMaterialFuture,
                saveMaterialFuture,
                cancelMaterialFuture
        ).thenAcceptAsync(v -> {
            try {
                Reward reward = EditRewardsUtils.editingReward.get(playerId);

                String nameMaterialName = nameMaterialFuture.join();
                String amountMaterialName = amountMaterialFuture.join();
                String rewardTypeIdMaterialName = rewardTypeIdMaterialFuture.join();
                String valueMaterialName = valueMaterialFuture.join();

                String saveMaterialName = saveMaterialFuture.join();
                String cancelMaterialName = cancelMaterialFuture.join();
                if (clickedItem.getType().equals(Material.getMaterial(nameMaterialName))) {
                    editRewardName(player, inv, reward);
                } else if (clickedItem.getType().equals(Material.getMaterial(amountMaterialName))) {
                    editRewardAmount(player, inv, reward);
                } else if (clickedItem.getType().equals(Material.getMaterial(rewardTypeIdMaterialName))) {
                    editRewardTypeId(player, inv, reward);
                } else if (clickedItem.getType().equals(Material.getMaterial(valueMaterialName))) {
                    editRewardValue(player, inv, reward);
                } else if (clickedItem.getType().equals(Material.getMaterial(saveMaterialName))) {
                    saveQuest(player, inv, EditQuestsUtils.editingQuest.get(player.getUniqueId()), reward);
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

    private void editRewardValue(Player player, Inventory inv, Reward reward) {
        CompletableFuture<String> valueChangeMsgFuture = configHandler.getStringAsync("inventory.edit-reward.value.change-message");
        valueChangeMsgFuture.thenAcceptAsync(valueChangeMsg -> {
            Bukkit.getScheduler().runTask(questSystem, () -> {
                new AnvilGUI.Builder()
                        .onClick((slot, stateSnapshot) -> {
                            if (slot != AnvilGUI.Slot.OUTPUT) {
                                return Collections.emptyList();
                            }
                            return Arrays.asList(
                                    AnvilGUI.ResponseAction.close(),
                                    AnvilGUI.ResponseAction.run(() -> reward.setValue(stateSnapshot.getText()))
                            );
                        })
                        .onClose(player1 -> {
                            EditRewardsUtils.editRewardsInventory(reward, player).thenAccept(v -> {
                                Bukkit.getScheduler().runTask(questSystem, () -> {
                                    player.openInventory(EditRewardsUtils.editRewardInventory.get(player.getUniqueId()));
                                });
                            });
                        })
                        .text(reward.getValue())
                        .title(valueChangeMsg)
                        .plugin(questSystem)
                        .open(player);
            });
        });
    }

    private void editRewardTypeId(Player player, Inventory inv, Reward reward) {
        CompletableFuture<String> rewardTypeIdChangeMsgFuture = configHandler.getStringAsync("inventory.edit-reward.reward-type-id.change-message");
        rewardTypeIdChangeMsgFuture.thenAcceptAsync(rewardTypeIdChangeMsg -> {
            Bukkit.getScheduler().runTask(questSystem, () -> {
                new AnvilGUI.Builder()
                        .onClick((slot, stateSnapshot) -> {
                            if (slot != AnvilGUI.Slot.OUTPUT) {
                                return Collections.emptyList();
                            }
                            return Arrays.asList(
                                    AnvilGUI.ResponseAction.close(),
                                    AnvilGUI.ResponseAction.run(() -> reward.setRewardTypeId(Integer.parseInt(stateSnapshot.getText())))
                            );
                        })
                        .onClose(player1 -> {
                            EditRewardsUtils.editRewardsInventory(reward, player).thenAccept(v -> {
                                Bukkit.getScheduler().runTask(questSystem, () -> {
                                    player.openInventory(EditRewardsUtils.editRewardInventory.get(player.getUniqueId()));
                                });
                            });
                        })
                        .text(reward.getRewardTypeId() + "")
                        .title(rewardTypeIdChangeMsg)
                        .plugin(questSystem)
                        .open(player);
            });
        });
    }

    private void editRewardAmount(Player player, Inventory inv, Reward reward) {
        CompletableFuture<String> amountChangeMsgFuture = configHandler.getStringAsync("inventory.edit-reward.amount.change-message");
        amountChangeMsgFuture.thenAcceptAsync(amountChangeMsg -> {
            Bukkit.getScheduler().runTask(questSystem, () -> {
                new AnvilGUI.Builder()
                        .onClick((slot, stateSnapshot) -> {
                            if (slot != AnvilGUI.Slot.OUTPUT) {
                                return Collections.emptyList();
                            }
                            return Arrays.asList(
                                    AnvilGUI.ResponseAction.close(),
                                    AnvilGUI.ResponseAction.run(() -> reward.setAmount(Integer.parseInt(stateSnapshot.getText())))
                            );
                        })
                        .onClose(player1 -> {
                            EditRewardsUtils.editRewardsInventory(reward, player).thenAccept(v -> {
                                Bukkit.getScheduler().runTask(questSystem, () -> {
                                    player.openInventory(EditRewardsUtils.editRewardInventory.get(player.getUniqueId()));
                                });
                            });
                        })
                        .text(reward.getAmount() + "")
                        .title(amountChangeMsg)
                        .plugin(questSystem)
                        .open(player);
            });
        });
    }

    private void editRewardName(Player player, Inventory inv, Reward reward) {
        CompletableFuture<String> nameChangeMsgFuture = configHandler.getStringAsync("inventory.edit-reward.name.change-message");
        nameChangeMsgFuture.thenAcceptAsync(nameChangeMsg -> {
            Bukkit.getScheduler().runTask(questSystem, () -> {
                new AnvilGUI.Builder()
                        .onClick((slot, stateSnapshot) -> {
                            if (slot != AnvilGUI.Slot.OUTPUT) {
                                return Collections.emptyList();
                            }
                            return Arrays.asList(
                                    AnvilGUI.ResponseAction.close(),
                                    AnvilGUI.ResponseAction.run(() -> reward.setName(stateSnapshot.getText()))
                            );
                        })
                        .onClose(player1 -> {
                            EditRewardsUtils.editRewardsInventory(reward, player).thenAccept(v -> {
                                Bukkit.getScheduler().runTask(questSystem, () -> {
                                    player.openInventory(EditRewardsUtils.editRewardInventory.get(player.getUniqueId()));
                                });
                            });
                        })
                        .text(reward.getName())
                        .title(nameChangeMsg)
                        .plugin(questSystem)
                        .open(player);
            });
        });
    }

    private void cancelEdit(Player player, Inventory inv, Quest quest) {
        EditRewardsUtils.editRewardInventory.remove(player.getUniqueId());
        EditRewardsUtils.editingReward.remove(player.getUniqueId());
        EditRewardsUtils.refreshRewards(quest).thenAccept(v -> {
            Bukkit.getScheduler().runTask(questSystem, () -> {
                player.closeInventory();
                player.openInventory(EditRewardsUtils.allRewardsInventory.get(player.getUniqueId()));
            });
        });
    }

    private void saveQuest(Player player, Inventory inv, Quest quest, Reward reward) {
        EditRewardsUtils.editRewardInventory.remove(player.getUniqueId());
        EditRewardsUtils.editingReward.remove(player.getUniqueId());
        databaseHandler.updateAsync(reward).thenAccept(v -> {
            EditRewardsUtils.refreshRewards(quest).thenAccept(v2 -> {
                Bukkit.getScheduler().runTask(questSystem, () -> {
                    player.closeInventory();
                    player.openInventory(EditRewardsUtils.allRewardsInventory.get(player.getUniqueId()));
                });
            });
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }
}
