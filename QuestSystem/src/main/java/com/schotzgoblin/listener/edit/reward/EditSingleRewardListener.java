package com.schotzgoblin.listener.edit.reward;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.database.Quest;
import com.schotzgoblin.database.Reward;
import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.QuestSystem;
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

public class EditSingleRewardListener implements Listener {
    private static final Logger logger = LoggerFactory.getLogger(EditSingleRewardListener.class);
    private final QuestSystem questSystem = QuestSystem.getInstance();
    private final DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
    private final ConfigHandler configHandler = ConfigHandler.getInstance();
    private final Map<String, BiConsumer<Player, Reward>> actionMap = new HashMap<>();
    //Normally I would make all those edit classes generic, so I have to do it once, but unfortunately I don't have the time to do it now
    {
        actionMap.put("inventory.edit-reward.name.material", this::editRewardName);
        actionMap.put("inventory.edit-reward.amount.material", this::editRewardAmount);
        actionMap.put("inventory.edit-reward.reward-type-id.material", this::editRewardTypeId);
        actionMap.put("inventory.edit-reward.value.material", this::editRewardValue);
        actionMap.put("inventory.edit-reward.save.material", (player, reward) -> saveReward(player, EditQuestsUtils.editingQuest.get(player.getUniqueId()), reward));
        actionMap.put("inventory.edit-reward.cancel.material", (player, reward) -> cancelEdit(player, EditQuestsUtils.editingQuest.get(player.getUniqueId())));

    }

    public EditSingleRewardListener() {
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
            if (itemMeta == null || !(itemMeta.displayName() instanceof TextComponent)) {
                return;
            }

            editRewardAttributeInventory(player, playerId, clickedItem);

        } catch (Exception ignored) {
            // Exception ignored, because it is not necessary to handle it (Only happens if player spam accepts EditQuestsUtils.quests)
        }
    }

    private void editRewardAttributeInventory(Player player, UUID playerId, ItemStack clickedItem) {
        Map<String, CompletableFuture<String>> futures = new HashMap<>();
        actionMap.keySet().forEach(key -> futures.put(key, configHandler.getStringAsync(key)));

        EditUtils.handleClickedItem(player, clickedItem, futures, actionMap,
                () -> EditRewardsUtils.editingReward.get(playerId));
    }

    private void editRewardProperty(Player player, Reward reward, String configKey, String initialValue, BiConsumer<Reward, String> onValueChange) {
        CompletableFuture<String> changeMsgFuture = configHandler.getStringAsync(configKey);
        changeMsgFuture.thenAcceptAsync(changeMsg -> {
            Bukkit.getScheduler().runTask(questSystem, () -> {
                new AnvilGUI.Builder().onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }
                    return Arrays.asList(AnvilGUI.ResponseAction.close(), AnvilGUI.ResponseAction.run(() -> {
                        onValueChange.accept(reward, stateSnapshot.getText());
                    }));
                }).onClose(player1 -> {
                    EditRewardsUtils.editRewardsInventory(reward, player).thenAccept(v -> {
                        Bukkit.getScheduler().runTask(questSystem, () -> {
                            player.openInventory(EditRewardsUtils.editRewardInventory.get(player.getUniqueId()));
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

    private void editRewardValue(Player player, Reward reward) {
        editRewardProperty(player, reward, "inventory.edit-reward.value.change-message",
                reward.getValue(), Reward::setValue);
    }

    private void editRewardTypeId(Player player, Reward reward) {
        editRewardProperty(player, reward, "inventory.edit-reward.reward-type-id.change-message",
                String.valueOf(reward.getRewardTypeId()), (r, text) -> r.setRewardTypeId(Integer.parseInt(text)));
    }

    private void editRewardAmount(Player player, Reward reward) {
        editRewardProperty(player, reward, "inventory.edit-reward.amount.change-message",
                String.valueOf(reward.getAmount()), (r, text) -> r.setAmount(Integer.parseInt(text)));
    }

    private void editRewardName(Player player, Reward reward) {
        editRewardProperty(player, reward, "inventory.edit-reward.name.change-message",
                reward.getName(), Reward::setName);
    }

    private void cancelEdit(Player player, Quest quest) {
        EditRewardsUtils.editRewardInventory.remove(player.getUniqueId());
        EditRewardsUtils.editingReward.remove(player.getUniqueId());
        EditUtils.navigateBack(player, quest, false);
    }

    private void saveReward(Player player, Quest quest, Reward reward) {
        var createRewardNameDefaultValue = configHandler.getStringAsync("inventory.create-reward.default.name").join();
        var createRewardErrorDefaultValue = configHandler.getStringAsync("inventory.create-reward.error-message").join();
        if (reward.getName().equals(createRewardNameDefaultValue)) {
            player.sendMessage(createRewardErrorDefaultValue);
            return;
        }
        EditRewardsUtils.editRewardInventory.remove(player.getUniqueId());
        EditRewardsUtils.editingReward.remove(player.getUniqueId());
        if(reward.getId() == 0) {
            databaseHandler.saveAsync(reward).thenAccept(v -> {
                EditUtils.updateAndReloadQuest(player, quest, databaseHandler.updateAsync(quest),false);
            }).exceptionally(ex -> {
                logger.error(ex.getMessage(), ex);
                return null;
            });
        }else{
            EditUtils.updateAndReloadQuest(player, quest, databaseHandler.updateAsync(reward),false);
        }
    }

}
