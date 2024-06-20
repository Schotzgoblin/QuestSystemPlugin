package com.schotzgoblin.utils.edit;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.database.Quest;
import com.schotzgoblin.database.QuestReward;
import com.schotzgoblin.database.Reward;
import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.listener.QuestManager;
import com.schotzgoblin.main.QuestSystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.schotzgoblin.utils.edit.EditUtils.*;

public class EditRewardsUtils {
    private static final Logger logger = LoggerFactory.getLogger(EditRewardsUtils.class);
    public static Map<UUID, Inventory> allRewardsInventory = Collections.synchronizedMap(new HashMap<>());
    public static Map<UUID, Inventory> editRewardInventory = Collections.synchronizedMap(new HashMap<>());
    public static Map<UUID, Reward> editingReward = Collections.synchronizedMap(new HashMap<>());
    public static List<Reward> rewards = Collections.synchronizedList(new ArrayList<>());
    private static final QuestSystem questSystem = QuestSystem.getInstance();
    private static final DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
    private static final ConfigHandler configHandler = ConfigHandler.getInstance();
    private static final QuestManager questManager = QuestManager.getInstance();

    public static void refreshInventory(List<Reward> rewards, Quest quest, Inventory inventory, Player player) {
        initTopInventoryRow(inventory,player,"reward",rewards.size()).join();

        addAllRewardsToInventory(rewards,quest, inventory, player);

        questManager.finaliseInventory(inventory,54);
    }

    public static CompletableFuture<Inventory> editRewardsInventory(Reward reward, Player player) {
        CompletableFuture<String> editTitleFuture = configHandler.getStringAsync("quest-manager.reward.edit.title");
        CompletableFuture<String> editColourFuture = configHandler.getStringAsync("quest-manager.reward.edit.colour");

        CompletableFuture<String> nameTitleFuture = configHandler.getStringAsync("inventory.edit-reward.name.title");
        CompletableFuture<String> nameColourFuture = configHandler.getStringAsync("inventory.edit-reward.name.colour");
        CompletableFuture<String> nameMaterialFuture = configHandler.getStringAsync("inventory.edit-reward.name.material");

        CompletableFuture<String> amountTitleFuture = configHandler.getStringAsync("inventory.edit-reward.amount.title");
        CompletableFuture<String> amountColourFuture = configHandler.getStringAsync("inventory.edit-reward.amount.colour");
        CompletableFuture<String> amountMaterialFuture = configHandler.getStringAsync("inventory.edit-reward.amount.material");

        CompletableFuture<String> rewardTypeIdTitleFuture = configHandler.getStringAsync("inventory.edit-reward.reward-type-id.title");
        CompletableFuture<String> rewardTypeIdColourFuture = configHandler.getStringAsync("inventory.edit-reward.reward-type-id.colour");
        CompletableFuture<String> rewardTypeIdMaterialFuture = configHandler.getStringAsync("inventory.edit-reward.reward-type-id.material");

        CompletableFuture<String> valueTitleFuture = configHandler.getStringAsync("inventory.edit-reward.value.title");
        CompletableFuture<String> valueColourFuture = configHandler.getStringAsync("inventory.edit-reward.value.colour");
        CompletableFuture<String> valueMaterialFuture = configHandler.getStringAsync("inventory.edit-reward.value.material");

        CompletableFuture<String> saveTitleFuture = configHandler.getStringAsync("inventory.edit-reward.save.title");
        CompletableFuture<String> saveColourFuture = configHandler.getStringAsync("inventory.edit-reward.save.colour");
        CompletableFuture<String> saveMaterialFuture = configHandler.getStringAsync("inventory.edit-reward.save.material");

        CompletableFuture<String> cancelTitleFuture = configHandler.getStringAsync("inventory.edit-reward.cancel.title");
        CompletableFuture<String> cancelColourFuture = configHandler.getStringAsync("inventory.edit-reward.cancel.colour");
        CompletableFuture<String> cancelMaterialFuture = configHandler.getStringAsync("inventory.edit-reward.cancel.material");

        return CompletableFuture.allOf(
                editTitleFuture, editColourFuture,
                nameTitleFuture, nameColourFuture, nameMaterialFuture,
                amountTitleFuture, amountColourFuture, amountMaterialFuture,
                rewardTypeIdTitleFuture, rewardTypeIdColourFuture, rewardTypeIdMaterialFuture,
                valueTitleFuture, valueColourFuture, valueMaterialFuture,
                saveTitleFuture, saveColourFuture, saveMaterialFuture,
                cancelTitleFuture, cancelColourFuture, cancelMaterialFuture
        ).thenCompose(v -> {
            var inv = createInventory(editTitleFuture.join(), editColourFuture.join(), 27);
            try {
                String nameTitle = nameTitleFuture.join();
                String nameColour = nameColourFuture.join();
                String nameMaterialName = nameMaterialFuture.join();

                String amountTitle = amountTitleFuture.join();
                String amountColour = amountColourFuture.join();
                String amountMaterialName = amountMaterialFuture.join();

                String rewardTypeIdTitle = rewardTypeIdTitleFuture.join();
                String rewardTypeIdColour = rewardTypeIdColourFuture.join();
                String rewardTypeIdMaterialName = rewardTypeIdMaterialFuture.join();

                String valueTitle = valueTitleFuture.join();
                String valueColour = valueColourFuture.join();
                String valueMaterialName = valueMaterialFuture.join();

                String saveTitle = saveTitleFuture.join();
                String saveColour = saveColourFuture.join();
                String saveMaterialName = saveMaterialFuture.join();

                String cancelTitle = cancelTitleFuture.join();
                String cancelColour = cancelColourFuture.join();
                String cancelMaterialName = cancelMaterialFuture.join();

                ItemStack nameItem = createItem(nameMaterialName, nameTitle, nameColour);
                setItemLore(nameItem, List.of(Component.text(reward.getName())));

                ItemStack amountItem = createItem(amountMaterialName, amountTitle, amountColour);
                setItemLore(amountItem, List.of(Component.text(reward.getAmount()+"")));

                ItemStack rewardTypeIdItem = createItem(rewardTypeIdMaterialName, rewardTypeIdTitle, rewardTypeIdColour);
                setItemLore(rewardTypeIdItem, List.of(Component.text(reward.getRewardTypeId())));

                ItemStack valueItem = createItem(valueMaterialName, valueTitle, valueColour);
                setItemLore(valueItem, List.of(Component.text(reward.getValue())));

                ItemStack saveItem = createItem(saveMaterialName, saveTitle, saveColour);
                ItemStack cancelItem = createItem(cancelMaterialName, cancelTitle, cancelColour);

                inv.setItem(0, nameItem);
                inv.setItem(8, amountItem);
                inv.setItem(11, rewardTypeIdItem);
                inv.setItem(15, valueItem);
                inv.setItem(21, saveItem);
                inv.setItem(23, cancelItem);

                questManager.finaliseInventory(inv,27);
            } catch (CompletionException ex) {
                logger.error(ex.getMessage(),ex);
            }
            EditRewardsUtils.editRewardInventory.put(player.getUniqueId(), inv);
            return CompletableFuture.completedFuture(inv);
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(),ex);
            return null;
        });
    }

    public static CompletableFuture<Void> initInventory(Player player, Quest quest) {
        var rewardsFuture = databaseHandler.getAllRewardsAsync();
        var title = configHandler.getStringAsync("reward-inv.title");
        var colour = configHandler.getStringAsync("reward-inv.colour");
        return CompletableFuture.allOf(rewardsFuture, title, colour).thenCompose(x -> {
            List<Reward> rewards = rewardsFuture.join();
            EditRewardsUtils.rewards = rewards;
            String inventoryTitle = title.join();
            String inventoryColour = colour.join();
            EditUtils.playerPage.put(player.getUniqueId(), 1);
            var inventory = createInventory(inventoryTitle, inventoryColour, 9 * 6);
            refreshInventory(rewards,quest, inventory, player);
            return CompletableFuture.completedFuture(null);
        });
    }

    private static void addAllRewardsToInventory(List<Reward> rewards,Quest quest, Inventory inventory, Player player) {
        int startIndex = (EditUtils.playerPage.get(player.getUniqueId())-1) * EditUtils.pageSize;
        int endIndex = startIndex + EditUtils.pageSize;


        for (int i = startIndex; i < endIndex; i++) {
            if(i >= rewards.size()) {
                var itemStack = new ItemStack(Material.AIR);
                inventory.setItem((i - startIndex) + 18, itemStack);
                continue;
            }
            Reward reward = rewards.get(i);
            var itemStack = createRewardItemStackAsync(reward,quest).join();
            inventory.setItem((i - startIndex) + 18, itemStack);
        }

        EditRewardsUtils.allRewardsInventory.put(player.getUniqueId(), inventory);
    }

    public static CompletableFuture<ItemStack> createRewardItemStackAsync(Reward reward, Quest quest) {
        List<Component> lore = new ArrayList<>();

        CompletableFuture<QuestReward> questRewardFuture = databaseHandler.getQuestRewardAsync(quest,reward);
        CompletableFuture<String> mainColourFuture = configHandler.getStringAsync("quest-manager.reward.colour");
        CompletableFuture<String> editMessageFuture = configHandler.getStringAsync("quest-manager.reward.lore.click-to-edit.message");
        CompletableFuture<String> editColourFuture = configHandler.getStringAsync("quest-manager.reward.lore.click-to-edit.colour");
        CompletableFuture<String> deleteMessageFuture = configHandler.getStringAsync("quest-manager.reward.lore.click-to-delete.message");
        CompletableFuture<String> deleteColourFuture = configHandler.getStringAsync("quest-manager.reward.lore.click-to-delete.colour");
        CompletableFuture<String> assignedMsgFuture = configHandler.getStringAsync("quest-manager.reward.lore.already-assigned.message");
        CompletableFuture<String> assignedColourFuture = configHandler.getStringAsync("quest-manager.reward.lore.already-assigned.colour");
        CompletableFuture<String> notAssignedMsgFuture = configHandler.getStringAsync("quest-manager.reward.lore.click-to-assign.message");
        CompletableFuture<String> notAssignedColourFuture = configHandler.getStringAsync("quest-manager.reward.lore.click-to-assign.colour");
        CompletableFuture<String> rewardNameMsgFuture = configHandler.getStringAsync("quest-manager.reward.lore.name.message");
        CompletableFuture<String> rewardNameColourFuture = configHandler.getStringAsync("quest-manager.reward.lore.name.colour");
        CompletableFuture<String> rewardAmountMsgFuture = configHandler.getStringAsync("quest-manager.reward.lore.amount.message");
        CompletableFuture<String> rewardAmountColourFuture = configHandler.getStringAsync("quest-manager.reward.lore.amount.colour");
        CompletableFuture<String> rewardTypeIdMsgFuture = configHandler.getStringAsync("quest-manager.reward.lore.rewardTypeId.message");
        CompletableFuture<String> rewardTypeIdColourFuture = configHandler.getStringAsync("quest-manager.reward.lore.rewardTypeId.colour");
        CompletableFuture<String> rewardValueMsgFuture = configHandler.getStringAsync("quest-manager.reward.lore.value.message");
        CompletableFuture<String> rewardValueColourFuture = configHandler.getStringAsync("quest-manager.reward.lore.value.colour");

        CompletableFuture<String> emptyLoreFuture = configHandler.getStringAsync("quest-manager.lore-entries.empty");


        return CompletableFuture.allOf(
                questRewardFuture, mainColourFuture,
                editMessageFuture, editColourFuture,
                deleteMessageFuture, deleteColourFuture,
                assignedMsgFuture, assignedColourFuture,
                notAssignedMsgFuture, notAssignedColourFuture,
                rewardNameMsgFuture, rewardNameColourFuture,
                rewardAmountMsgFuture, rewardAmountColourFuture,
                rewardTypeIdMsgFuture, rewardTypeIdColourFuture,
                rewardValueMsgFuture, rewardValueColourFuture,
                emptyLoreFuture
                ).thenCompose(v -> {
            try {
                ItemStack itemStack = new ItemStack(Material.EMERALD);
                var questReward = questRewardFuture.get();
                var mainRewardColor = TextColor.fromHexString(mainColourFuture.get());

                var rewardsLabel = rewardNameMsgFuture.get();
                var rewardsLabelColour = TextColor.fromHexString(rewardNameColourFuture.get());

                var rewardsAmount = rewardAmountMsgFuture.get();
                var rewardsAmountColour = TextColor.fromHexString(rewardAmountColourFuture.get());

                var rewardTypeId = rewardTypeIdMsgFuture.get();
                var rewardTypeIdColour = TextColor.fromHexString(rewardTypeIdColourFuture.get());

                var rewardValue = rewardValueMsgFuture.get();
                var rewardValueColour = TextColor.fromHexString(rewardValueColourFuture.get());

                var emptyLore = emptyLoreFuture.get();

                var editMessage = editMessageFuture.get();
                var editColour = TextColor.fromCSSHexString(editColourFuture.get());
                var deleteMessage = deleteMessageFuture.get();
                var deleteColour = TextColor.fromCSSHexString(deleteColourFuture.get());
                var assignedMsg = assignedMsgFuture.get();
                var assignedColour = TextColor.fromCSSHexString(assignedColourFuture.get());
                var notAssignedMsg = notAssignedMsgFuture.get();
                var notAssignedColour = TextColor.fromCSSHexString(notAssignedColourFuture.get());

                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.displayName(Component.text(reward.getName(),mainRewardColor));
                if(questReward!=null){
                    lore.add(Component.text(assignedMsg, assignedColour));
                }else{
                    lore.add(Component.text(notAssignedMsg, notAssignedColour));
                }
                lore.add(Component.text(emptyLore));
                lore.add(Component.text(editMessage, editColour));
                lore.add(Component.text(deleteMessage, deleteColour));
                lore.add(Component.text(emptyLore));
                lore.add(Component.text(rewardsLabel, rewardsLabelColour));
                lore.add(Component.text(reward.getName()));
                lore.add(Component.text(emptyLore));
                lore.add(Component.text(rewardsAmount, rewardsAmountColour));
                lore.add(Component.text(reward.getAmount()+""));
                lore.add(Component.text(emptyLore));
                lore.add(Component.text(rewardTypeId, rewardTypeIdColour));
                lore.add(Component.text(reward.getRewardTypeId()));
                lore.add(Component.text(emptyLore));
                lore.add(Component.text(rewardValue, rewardValueColour));
                lore.add(Component.text(reward.getValue()));
                itemMeta.lore(lore);
                itemStack.setItemMeta(itemMeta);
                return CompletableFuture.completedFuture(itemStack);
            } catch (Exception e) {
                logger.error(e.getMessage(),e);
            }
            return CompletableFuture.completedFuture(new ItemStack(Material.AIR));
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(),ex);
            return null;
        });
    }


    public static CompletableFuture<Void> refreshRewards(Quest quest) {
        var future = databaseHandler.getAllRewardsAsync();
        return future.thenAccept(rewards -> {
            EditRewardsUtils.rewards = rewards;
            Bukkit.getScheduler().runTask(questSystem, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Inventory inventory = EditRewardsUtils.allRewardsInventory.get(player.getUniqueId());
                    refreshInventory(rewards, quest, inventory, player);
                }
            });
        });
    }
}
