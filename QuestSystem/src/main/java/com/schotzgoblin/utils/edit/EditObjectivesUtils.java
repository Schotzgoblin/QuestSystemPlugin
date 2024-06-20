package com.schotzgoblin.utils.edit;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.database.Quest;
import com.schotzgoblin.database.Objective;
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

public class EditObjectivesUtils {
    private static final Logger logger = LoggerFactory.getLogger(EditObjectivesUtils.class);
    public static Map<UUID, Inventory> allObjectivesInventory = Collections.synchronizedMap(new HashMap<>());
    public static Map<UUID, Inventory> editObjectiveInventory = Collections.synchronizedMap(new HashMap<>());
    public static Map<UUID, Objective> editingObjective = Collections.synchronizedMap(new HashMap<>());
    public static List<Objective> objectives = Collections.synchronizedList(new ArrayList<>());
    private static final QuestSystem questSystem = QuestSystem.getInstance();
    private static final DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
    private static final ConfigHandler configHandler = ConfigHandler.getInstance();
    private static final QuestManager questManager = QuestManager.getInstance();

    public static void refreshInventory(List<Objective> objectives, Quest quest, Inventory inventory, Player player) {
        initTopInventoryRow(inventory,player,"objective",objectives.size()).join();

        addAllObjectivesToInventory(objectives,quest, inventory, player);

        questManager.finaliseInventory(inventory,54);
    }

    public static CompletableFuture<Inventory> editObjectivesInventory(Objective objective, Player player) {
        CompletableFuture<String> editTitleFuture = configHandler.getStringAsync("quest-manager.objective.edit.title");
        CompletableFuture<String> editColourFuture = configHandler.getStringAsync("quest-manager.objective.edit.colour");

        CompletableFuture<String> nameTitleFuture = configHandler.getStringAsync("inventory.edit-objective.name.title");
        CompletableFuture<String> nameColourFuture = configHandler.getStringAsync("inventory.edit-objective.name.colour");
        CompletableFuture<String> nameMaterialFuture = configHandler.getStringAsync("inventory.edit-objective.name.material");

        CompletableFuture<String> amountTitleFuture = configHandler.getStringAsync("inventory.edit-objective.count.title");
        CompletableFuture<String> amountColourFuture = configHandler.getStringAsync("inventory.edit-objective.count.colour");
        CompletableFuture<String> amountMaterialFuture = configHandler.getStringAsync("inventory.edit-objective.count.material");

        CompletableFuture<String> objectiveTypeIdTitleFuture = configHandler.getStringAsync("inventory.edit-objective.type.title");
        CompletableFuture<String> objectiveTypeIdColourFuture = configHandler.getStringAsync("inventory.edit-objective.type.colour");
        CompletableFuture<String> objectiveTypeIdMaterialFuture = configHandler.getStringAsync("inventory.edit-objective.type.material");

        CompletableFuture<String> valueTitleFuture = configHandler.getStringAsync("inventory.edit-objective.value.title");
        CompletableFuture<String> valueColourFuture = configHandler.getStringAsync("inventory.edit-objective.value.colour");
        CompletableFuture<String> valueMaterialFuture = configHandler.getStringAsync("inventory.edit-objective.value.material");

        CompletableFuture<String> saveTitleFuture = configHandler.getStringAsync("inventory.edit-objective.save.title");
        CompletableFuture<String> saveColourFuture = configHandler.getStringAsync("inventory.edit-objective.save.colour");
        CompletableFuture<String> saveMaterialFuture = configHandler.getStringAsync("inventory.edit-objective.save.material");

        CompletableFuture<String> cancelTitleFuture = configHandler.getStringAsync("inventory.edit-objective.cancel.title");
        CompletableFuture<String> cancelColourFuture = configHandler.getStringAsync("inventory.edit-objective.cancel.colour");
        CompletableFuture<String> cancelMaterialFuture = configHandler.getStringAsync("inventory.edit-objective.cancel.material");

        return CompletableFuture.allOf(
                editTitleFuture, editColourFuture,
                nameTitleFuture, nameColourFuture, nameMaterialFuture,
                amountTitleFuture, amountColourFuture, amountMaterialFuture,
                objectiveTypeIdTitleFuture, objectiveTypeIdColourFuture, objectiveTypeIdMaterialFuture,
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

                String valueTitle = valueTitleFuture.join();
                String valueColour = valueColourFuture.join();
                String valueMaterialName = valueMaterialFuture.join();

                String objectiveTypeTitle = objectiveTypeIdTitleFuture.join();
                String objectiveTypeColour = objectiveTypeIdColourFuture.join();
                String objectiveTypeMaterialName = objectiveTypeIdMaterialFuture.join();

                String saveTitle = saveTitleFuture.join();
                String saveColour = saveColourFuture.join();
                String saveMaterialName = saveMaterialFuture.join();

                String cancelTitle = cancelTitleFuture.join();
                String cancelColour = cancelColourFuture.join();
                String cancelMaterialName = cancelMaterialFuture.join();

                ItemStack nameItem = createItem(nameMaterialName, nameTitle, nameColour);
                setItemLore(nameItem, List.of(Component.text(objective.getObjective())));

                ItemStack amountItem = createItem(amountMaterialName, amountTitle, amountColour);
                setItemLore(amountItem, List.of(Component.text(objective.getCount()+"")));

                ItemStack valueItem = createItem(valueMaterialName, valueTitle, valueColour);
                setItemLore(valueItem, List.of(Component.text(objective.getValue())));

                ItemStack objectiveTypeItem = createItem(objectiveTypeMaterialName, objectiveTypeTitle, objectiveTypeColour);
                setItemLore(objectiveTypeItem, List.of(Component.text(objective.getType())));

                ItemStack saveItem = createItem(saveMaterialName, saveTitle, saveColour);
                ItemStack cancelItem = createItem(cancelMaterialName, cancelTitle, cancelColour);

                inv.setItem(0, nameItem);
                inv.setItem(8, amountItem);
                inv.setItem(11, objectiveTypeItem);
                inv.setItem(15, valueItem);
                inv.setItem(21, saveItem);
                inv.setItem(23, cancelItem);

                questManager.finaliseInventory(inv,27);
            } catch (CompletionException ex) {
                logger.error(ex.getMessage(),ex);
            }
            EditObjectivesUtils.editObjectiveInventory.put(player.getUniqueId(), inv);
            return CompletableFuture.completedFuture(inv);
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(),ex);
            return null;
        });
    }

    public static CompletableFuture<Void> initInventory(Player player, Quest quest) {
        var objectivesFuture = databaseHandler.getAllObjectivesAsync();
        var title = configHandler.getStringAsync("objective-inv.title");
        var colour = configHandler.getStringAsync("objective-inv.colour");
        return CompletableFuture.allOf(objectivesFuture, title, colour).thenCompose(x -> {
            List<Objective> objectives = objectivesFuture.join();
            EditObjectivesUtils.objectives = objectives;
            String inventoryTitle = title.join();
            String inventoryColour = colour.join();
            EditUtils.playerPage.put(player.getUniqueId(), 1);
            var inventory = createInventory(inventoryTitle, inventoryColour, 9 * 6);
            refreshInventory(objectives,quest, inventory, player);
            return CompletableFuture.completedFuture(null);
        });
    }

    private static void addAllObjectivesToInventory(List<Objective> objectives,Quest quest, Inventory inventory, Player player) {
        int startIndex = (EditUtils.playerPage.get(player.getUniqueId())-1) * EditUtils.pageSize;
        int endIndex = startIndex + EditUtils.pageSize;


        for (int i = startIndex; i < endIndex; i++) {
            if(i >= objectives.size()) {
                var itemStack = new ItemStack(Material.AIR);
                inventory.setItem((i - startIndex) + 18, itemStack);
                continue;
            }
            Objective objective = objectives.get(i);
            var itemStack = createObjectiveItemStackAsync(objective,quest).join();
            inventory.setItem((i - startIndex) + 18, itemStack);
        }

        EditObjectivesUtils.allObjectivesInventory.put(player.getUniqueId(), inventory);
    }

    public static CompletableFuture<ItemStack> createObjectiveItemStackAsync(Objective objective, Quest quest) {
        List<Component> lore = new ArrayList<>();

        CompletableFuture<String> mainColourFuture = configHandler.getStringAsync("quest-manager.objective.colour");
        CompletableFuture<String> editMessageFuture = configHandler.getStringAsync("quest-manager.objective.lore.click-to-edit.message");
        CompletableFuture<String> editColourFuture = configHandler.getStringAsync("quest-manager.objective.lore.click-to-edit.colour");
        CompletableFuture<String> deleteMessageFuture = configHandler.getStringAsync("quest-manager.objective.lore.click-to-delete.message");
        CompletableFuture<String> deleteColourFuture = configHandler.getStringAsync("quest-manager.objective.lore.click-to-delete.colour");
        CompletableFuture<String> assignedMsgFuture = configHandler.getStringAsync("quest-manager.objective.lore.already-assigned.message");
        CompletableFuture<String> assignedColourFuture = configHandler.getStringAsync("quest-manager.objective.lore.already-assigned.colour");
        CompletableFuture<String> notAssignedMsgFuture = configHandler.getStringAsync("quest-manager.objective.lore.click-to-assign.message");
        CompletableFuture<String> notAssignedColourFuture = configHandler.getStringAsync("quest-manager.objective.lore.click-to-assign.colour");
        CompletableFuture<String> objectiveNameMsgFuture = configHandler.getStringAsync("quest-manager.objective.lore.name.message");
        CompletableFuture<String> objectiveNameColourFuture = configHandler.getStringAsync("quest-manager.objective.lore.name.colour");
        CompletableFuture<String> objectiveCountMsgFuture = configHandler.getStringAsync("quest-manager.objective.lore.count.message");
        CompletableFuture<String> objectiveCountColourFuture = configHandler.getStringAsync("quest-manager.objective.lore.count.colour");
        CompletableFuture<String> typeMsgFuture = configHandler.getStringAsync("quest-manager.objective.lore.type.message");
        CompletableFuture<String> typeColourFuture = configHandler.getStringAsync("quest-manager.objective.lore.type.colour");
        CompletableFuture<String> objectiveValueMsgFuture = configHandler.getStringAsync("quest-manager.objective.lore.value.message");
        CompletableFuture<String> objectiveValueColourFuture = configHandler.getStringAsync("quest-manager.objective.lore.value.colour");

        CompletableFuture<String> emptyLoreFuture = configHandler.getStringAsync("quest-manager.lore-entries.empty");


        return CompletableFuture.allOf(
                mainColourFuture,
                editMessageFuture, editColourFuture,
                deleteMessageFuture, deleteColourFuture,
                assignedMsgFuture, assignedColourFuture,
                notAssignedMsgFuture, notAssignedColourFuture,
                objectiveNameMsgFuture, objectiveNameColourFuture,
                objectiveCountMsgFuture, objectiveCountColourFuture,
                typeMsgFuture, typeColourFuture,
                objectiveValueMsgFuture, objectiveValueColourFuture,
                emptyLoreFuture
                ).thenCompose(v -> {
            try {
                ItemStack itemStack = new ItemStack(Material.EMERALD);
                var mainObjectiveColor = TextColor.fromHexString(mainColourFuture.get());

                var objectivesLabel = objectiveNameMsgFuture.get();
                var objectivesLabelColour = TextColor.fromHexString(objectiveNameColourFuture.get());

                var objectivesCount = objectiveCountMsgFuture.get();
                var objectivesCountColour = TextColor.fromHexString(objectiveCountColourFuture.get());

                var objectiveType = typeMsgFuture.get();
                var objectiveTypeColour = TextColor.fromHexString(typeColourFuture.get());

                var objectiveValue = objectiveValueMsgFuture.get();
                var objectiveValueColour = TextColor.fromHexString(objectiveValueColourFuture.get());

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
                itemMeta.displayName(Component.text(objective.getObjective(),mainObjectiveColor));
                if(quest.getObjectiveId()==objective.getId()){
                    lore.add(Component.text(assignedMsg, assignedColour));
                }else{
                    lore.add(Component.text(notAssignedMsg, notAssignedColour));
                }
                lore.add(Component.text(emptyLore));
                lore.add(Component.text(editMessage, editColour));
                lore.add(Component.text(deleteMessage, deleteColour));
                lore.add(Component.text(emptyLore));
                lore.add(Component.text(objectivesLabel, objectivesLabelColour));
                lore.add(Component.text(objective.getObjective()));
                lore.add(Component.text(emptyLore));
                lore.add(Component.text(objectivesCount, objectivesCountColour));
                lore.add(Component.text(objective.getCount()+""));
                lore.add(Component.text(emptyLore));
                lore.add(Component.text(objectiveType, objectiveTypeColour));
                lore.add(Component.text(objective.getType()));
                lore.add(Component.text(emptyLore));
                lore.add(Component.text(objectiveValue, objectiveValueColour));
                lore.add(Component.text(objective.getValue()));
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


    public static CompletableFuture<Void> refreshObjectives(Quest quest) {
        var future = databaseHandler.getAllObjectivesAsync();
        return future.thenAccept(objectives -> {
            EditObjectivesUtils.objectives = objectives;
            Bukkit.getScheduler().runTask(questSystem, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Inventory inventory = EditObjectivesUtils.allObjectivesInventory.get(player.getUniqueId());
                    refreshInventory(objectives, quest, inventory, player);
                }
            });
        });
    }
}
