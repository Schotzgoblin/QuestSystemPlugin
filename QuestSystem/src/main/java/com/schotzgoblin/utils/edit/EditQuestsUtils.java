package com.schotzgoblin.utils.edit;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.database.Quest;
import com.schotzgoblin.database.Reward;
import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.QuestManager;
import com.schotzgoblin.main.QuestSystem;
import com.schotzgoblin.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.schotzgoblin.utils.edit.EditUtils.*;

public class EditQuestsUtils {
    public static Map<UUID, Inventory> allQuestsInventory = Collections.synchronizedMap(new HashMap<>());
    public static Map<UUID, Inventory> editQuestInventory = Collections.synchronizedMap(new HashMap<>());
    public static Map<UUID, Quest> editingQuest = Collections.synchronizedMap(new HashMap<>());
    public static List<Quest> quests = Collections.synchronizedList(new ArrayList<>());
    private static final QuestSystem questSystem = QuestSystem.getInstance();
    private static final DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
    private static final ConfigHandler configHandler = ConfigHandler.getInstance();
    private static final QuestManager questManager = QuestManager.getInstance();

    public static void refreshInventory(List<Quest> quests, Inventory inventory, Player player) {
        initTopInventoryRow(inventory,player,"quest", quests.size()).join();

        addAllQuestsToInventory(quests, inventory, player);

        questManager.finaliseInventory(inventory,54);
    }

    public static CompletableFuture<Inventory> refreshEditInventory(Quest quest, Player player) {
        return editQuestInventory(quest, player);
    }

    public static CompletableFuture<Inventory> editQuestInventory(Quest quest, Player player) {
        var rewardsFuture = databaseHandler.getQuestRewardsAsync(quest);
        CompletableFuture<String> editTitleFuture = configHandler.getStringAsync("quest-manager.quest.edit.title");
        CompletableFuture<String> editColourFuture = configHandler.getStringAsync("quest-manager.quest.edit.colour");
        CompletableFuture<String> loreTimeLeftFuture = configHandler.getStringAsync("quest-manager.lore-entries.time-left-label");

        CompletableFuture<String> noObjectiveTitleFuture = configHandler.getStringAsync("inventory.edit-quest.no-objective.title");
        CompletableFuture<String> noObjectiveColourFuture = configHandler.getStringAsync("inventory.edit-quest.no-objective.colour");

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
                noObjectiveColourFuture, noObjectiveTitleFuture,
                rewardsFuture,loreTimeLeftFuture, editTitleFuture, editColourFuture,
                nameTitleFuture, nameColourFuture, nameMaterialFuture,
                descriptionTitleFuture, descriptionColourFuture, descriptionMaterialFuture,
                objectiveTitleFuture, objectiveColourFuture, objectiveMaterialFuture,
                timeLimitTitleFuture, timeLimitColourFuture, timeLimitMaterialFuture,
                rewardsTitleFuture, rewardsColourFuture, rewardsMaterialFuture,
                saveTitleFuture, saveColourFuture, saveMaterialFuture,
                cancelTitleFuture, cancelColourFuture, cancelMaterialFuture
        ).thenCompose(v -> {
            String editTitle = editTitleFuture.join();
            String editColour = editColourFuture.join();
            var inv = createInventory(editTitle, editColour, 27);
            try {
                var rewards = rewardsFuture.join();
                String loreTimeLeft = loreTimeLeftFuture.join();
                String nameTitle = nameTitleFuture.join();
                String nameColour = nameColourFuture.join();
                String nameMaterialName = nameMaterialFuture.join();

                String noObjectiveTitle = noObjectiveTitleFuture.join();
                var noObjectiveColour = TextColor.fromHexString(noObjectiveColourFuture.join());

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
                setItemLore(nameItem, List.of(Component.text(quest.getName())));

                ItemStack descriptionItem = createItem(descriptionMaterialName, descriptionTitle, descriptionColour);
                setItemLore(descriptionItem, List.of(Component.text(quest.getDescription())));

                ItemStack objectiveItem = createItem(objectiveMaterialName, objectiveTitle, objectiveColour);
                var objectiveText = Component.text(noObjectiveTitle, noObjectiveColour);
                if(quest.getObjective() != null) {
                    objectiveText = Component.text(quest.getObjective().getObjective());
                }
                setItemLore(objectiveItem,List.of(objectiveText));

                ItemStack timeLimitItem = createItem(timeLimitMaterialName, timeLimitTitle, timeLimitColour);
                setItemLore(timeLimitItem, List.of(Component.text(loreTimeLeft+ Utils.getTimeStringFromSecs(quest.getTimeLimit()))));

                ItemStack rewardsItem = createItem(rewardsMaterialName, rewardsTitle, rewardsColour);
                setItemLore(rewardsItem, rewards.stream().map(x->Component.text(x.getName())).toList());

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
            EditQuestsUtils.editQuestInventory.put(player.getUniqueId(), inv);
            return CompletableFuture.completedFuture(inv);
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    public static CompletableFuture<Void> initInventory(Player player) {
        var questsFuture = databaseHandler.getAllQuestsAsync();
        var title = configHandler.getStringAsync("quest-inv.title");
        var colour = configHandler.getStringAsync("quest-inv.colour");
        return CompletableFuture.allOf(questsFuture, title, colour).thenCompose(x -> {
            List<Quest> quests = questsFuture.join();
            EditQuestsUtils.quests = quests;
            String inventoryTitle = title.join();
            String inventoryColour = colour.join();
            EditUtils.playerPage.put(player.getUniqueId(), 1);
            var inventory = createInventory(inventoryTitle, inventoryColour, 9 * 6);
            refreshInventory(quests, inventory, player);
            return CompletableFuture.completedFuture(null);
        });
    }

    private static void addAllQuestsToInventory(List<Quest> quests, Inventory inventory, Player player) {
        int startIndex = (EditUtils.playerPage.get(player.getUniqueId())-1) * EditUtils.pageSize;
        int endIndex = startIndex + EditUtils.pageSize;


        for (int i = startIndex; i < endIndex; i++) {
            if(i >= quests.size()) {
                var itemStack = new ItemStack(Material.AIR);
                inventory.setItem((i - startIndex) + 18, itemStack);
                continue;
            }
            Quest quest = quests.get(i);
            var itemStack = createQuestItemStackAsync(quest).join();
            inventory.setItem((i - startIndex) + 18, itemStack);
        }

        EditQuestsUtils.allQuestsInventory.put(player.getUniqueId(), inventory);
    }

    public static CompletableFuture<ItemStack> createQuestItemStackAsync(Quest quest) {
        List<Component> lore = new ArrayList<>();

        CompletableFuture<List<Reward>> rewardsFuture = databaseHandler.getQuestRewardsAsync(quest);
        CompletableFuture<String> notStartedMsgFuture = configHandler.getStringAsync("quest-manager.quest.click-not-started");
        CompletableFuture<String> inProgressMsgFuture = configHandler.getStringAsync("quest-manager.quest.click-in-progress");
        CompletableFuture<String> completedMsgFuture = configHandler.getStringAsync("quest-manager.quest.click-completed");
        CompletableFuture<String> canceledMsgFuture = configHandler.getStringAsync("quest-manager.quest.click-canceled");
        CompletableFuture<String> mainQuestColourHexString = configHandler.getStringAsync("quest-manager.quest.main-colour");
        CompletableFuture<String> emptyLoreFuture = configHandler.getStringAsync("quest-manager.lore-entries.empty");
        CompletableFuture<String> descriptionLabelFuture = configHandler.getStringAsync("quest-manager.lore-entries.description-label");
        CompletableFuture<String> objectiveLabelFuture = configHandler.getStringAsync("quest-manager.lore-entries.objective-label");
        CompletableFuture<String> timeLimitLabelFuture = configHandler.getStringAsync("quest-manager.lore-entries.time-limit-label");
        CompletableFuture<String> rewardsLabelFuture = configHandler.getStringAsync("quest-manager.lore-entries.rewards-label");
        CompletableFuture<String> rewardsLabelColourFuture = configHandler.getStringAsync("quest-manager.lore-entries.rewards-colour");
        CompletableFuture<String> editMessageFuture = configHandler.getStringAsync("quest-manager.lore-entries.edit.title");
        CompletableFuture<String> editColourFuture = configHandler.getStringAsync("quest-manager.lore-entries.edit.colour");
        CompletableFuture<String> deleteMessageFuture = configHandler.getStringAsync("quest-manager.lore-entries.delete.title");
        CompletableFuture<String> deleteColourFuture = configHandler.getStringAsync("quest-manager.lore-entries.delete.colour");

        return CompletableFuture.allOf(
                rewardsFuture, notStartedMsgFuture, inProgressMsgFuture, rewardsLabelFuture, rewardsLabelColourFuture,
                completedMsgFuture, canceledMsgFuture, mainQuestColourHexString, emptyLoreFuture,
                editMessageFuture, editColourFuture, deleteMessageFuture, deleteColourFuture,
                descriptionLabelFuture, objectiveLabelFuture, timeLimitLabelFuture).thenCompose(v -> {
            try {
                ItemStack itemStack = new ItemStack(Material.PAPER);
                var rewards = rewardsFuture.get();
                String rewardsLabel = rewardsLabelFuture.get();
                String rewardsLabelColour = rewardsLabelColourFuture.get();
                String mainQuestColorHexString = mainQuestColourHexString.get();

                var editMessage = editMessageFuture.get();
                var editColour = TextColor.fromCSSHexString(editColourFuture.get());
                var deleteMessage = deleteMessageFuture.get();
                var deleteColour = TextColor.fromCSSHexString(deleteColourFuture.get());

                String emptyLore = emptyLoreFuture.get();
                String descriptionLabel = descriptionLabelFuture.get();
                String objectiveLabel = objectiveLabelFuture.get();
                String timeLimitLabel = timeLimitLabelFuture.get();


                var mainQuestColor = TextColor.fromCSSHexString(mainQuestColorHexString);
                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.displayName(Component.text(quest.getName(), mainQuestColor));
                lore.add(Component.text(editMessage, editColour));
                lore.add(Component.text(deleteMessage, deleteColour));
                lore.add(Component.text(emptyLore));
                lore.add(Component.text(descriptionLabel, mainQuestColor));
                lore.add(Component.text(quest.getDescription()));
                lore.add(Component.text(emptyLore));
                lore.add(Component.text(objectiveLabel, mainQuestColor));
                lore.add(Component.text(quest.getObjective().getObjective()));
                lore.add(Component.text(emptyLore));
                lore.add(Component.text(timeLimitLabel, mainQuestColor));
                lore.add(Component.text(Utils.getTimeStringFromSecs(quest.getTimeLimit())));
                lore.add(Component.text(emptyLore));
                lore.add(Component.text(rewardsLabel, mainQuestColor));
                for (Reward reward : rewards) {
                    lore.add(Component.text(reward.getName(), TextColor.fromHexString(rewardsLabelColour)));
                }
                itemMeta.lore(lore);
                itemStack.setItemMeta(itemMeta);
                return CompletableFuture.completedFuture(itemStack);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return CompletableFuture.completedFuture(new ItemStack(Material.AIR));
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }


    public static CompletableFuture<Void> refreshQuests() {
        var future = databaseHandler.getAllQuestsAsync();
        return future.thenAccept(quests -> {
            EditQuestsUtils.quests = quests;
            for (Player player : Bukkit.getOnlinePlayers()) {
                Inventory inventory = EditQuestsUtils.allQuestsInventory.get(player.getUniqueId());
                refreshInventory(quests, inventory, player);
            }
        });
    }
}
