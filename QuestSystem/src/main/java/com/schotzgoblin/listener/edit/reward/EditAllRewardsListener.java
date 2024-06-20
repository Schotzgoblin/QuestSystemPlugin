package com.schotzgoblin.listener.edit.reward;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.database.Reward;
import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.QuestSystem;
import com.schotzgoblin.utils.edit.EditQuestsUtils;
import com.schotzgoblin.utils.edit.EditRewardsUtils;
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

public class EditAllRewardsListener implements Listener {
    private static final Logger logger = LoggerFactory.getLogger(EditAllRewardsListener.class);
    private final QuestSystem questSystem = QuestSystem.getInstance();
    private final DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
    private final ConfigHandler configHandler = ConfigHandler.getInstance();
    //Normally I would make all those edit classes generic, so I have to do it once, but unfortunately I don't have the time to do it now

    public EditAllRewardsListener() {
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
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
            var createRewardMaterial = configHandler.getMaterialAsync("inventory.create-reward.material").join();
            if (clickedItem.getType().equals(Material.PLAYER_HEAD)) {
                EditUtils.handlePageSwitch(player, inv, displayName.content());
            } else if (clickedItem.getType().equals(Material.EMERALD)) {
                allRewardsInventory(e, player, playerId);
            } else if (clickedItem.getType().equals(Material.BARRIER)) {
                Utils.navigateToQuestsInventory(player);
            }else if(clickedItem.getType().equals(createRewardMaterial)){
                createReward(player);
            }
        } catch (Exception ignored) {
            // Exception ignored, because it is not necessary to handle it (Only happens if player spam accepts EditQuestsUtils.quests)
        }
    }

    private void createReward(Player player) {
        var createRewardNameDefaultValueFuture = configHandler.getStringAsync("inventory.create-reward.default.name");
        var createRewardAmountDefaultValueFuture = configHandler.getIntAsync("inventory.create-reward.default.amount");
        var createRewardValueDefaultValueFuture = configHandler.getStringAsync("inventory.create-reward.default.value");
        var createRewardRewardTypeIdDefaultValueFuture = configHandler.getIntAsync("inventory.create-reward.default.rewardTypeId");

        CompletableFuture.allOf(
                createRewardNameDefaultValueFuture,
                createRewardAmountDefaultValueFuture,
                createRewardValueDefaultValueFuture,
                createRewardRewardTypeIdDefaultValueFuture
        ).thenAcceptAsync(v -> {
            var createRewardNameDefaultValue = createRewardNameDefaultValueFuture.join();
            var createRewardAmountDefaultValue = createRewardAmountDefaultValueFuture.join();
            var createRewardValueDefaultValue = createRewardValueDefaultValueFuture.join();
            var createRewardRewardTypeIdDefaultValue = createRewardRewardTypeIdDefaultValueFuture.join();
            var reward = new Reward(createRewardNameDefaultValue, createRewardAmountDefaultValue,
                    createRewardValueDefaultValue, createRewardRewardTypeIdDefaultValue);
            createOrEditReward(player, reward);
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(), ex);
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
                    EditUtils.handleQuestItemDelete(player, displayName, inv, "reward");
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
            }).exceptionally(ex -> {
                logger.error(ex.getMessage(), ex);
                return null;
            });
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(), ex);
            return null;
        });
    }

    private void handleQuestItemEdit(Player player, TextComponent displayName) {
        var rewardFuture = databaseHandler.getRewardByNameAsync(displayName.content());

        CompletableFuture.allOf(rewardFuture).thenAccept(x -> {
            var reward = rewardFuture.join();
            createOrEditReward(player, reward);
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(), ex);
            return null;
        });

    }

    private void createOrEditReward(Player player, Reward reward) {
        EditRewardsUtils.editingReward.put(player.getUniqueId(), reward);
        EditRewardsUtils.editRewardsInventory(reward, player).thenAccept(inv -> {
            Bukkit.getScheduler().runTask(questSystem, () -> {
                player.closeInventory();
                player.openInventory(inv);
            });
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(), ex);
            return null;
        });
    }
}
