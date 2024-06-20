package com.schotzgoblin.listener.edit.objective;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.database.Objective;
import com.schotzgoblin.database.Quest;
import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.QuestSystem;
import com.schotzgoblin.utils.edit.EditObjectivesUtils;
import com.schotzgoblin.utils.edit.EditQuestsUtils;
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
import java.util.function.Consumer;

public class EditSingleObjectiveListener implements Listener {
    private static final Logger logger = LoggerFactory.getLogger(EditSingleObjectiveListener.class);
    private final QuestSystem questSystem = QuestSystem.getInstance();
    private final DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
    private final ConfigHandler configHandler = ConfigHandler.getInstance();
    private final Map<String, BiConsumer<Player, Objective>> actionMap = new HashMap<>();

    {
        actionMap.put("inventory.edit-objective.name.material", this::editObjectiveName);
        actionMap.put("inventory.edit-objective.count.material", this::editObjectiveAmount);
        actionMap.put("inventory.edit-objective.type.material", this::editObjectiveTypeId);
        actionMap.put("inventory.edit-objective.value.material", this::editObjectiveValue);
        actionMap.put("inventory.edit-objective.save.material", (player, objective) -> saveQuest(player, EditQuestsUtils.editingQuest.get(player.getUniqueId()), objective));
        actionMap.put("inventory.edit-objective.cancel.material", (player, objective) -> cancelEdit(player, EditQuestsUtils.editingQuest.get(player.getUniqueId())));
    }
    //Normally I would make all those edit classes generic, so I have to do it once, but unfortunately I don't have the time to do it now

    public EditSingleObjectiveListener() {
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
            if (itemMeta == null || !(itemMeta.displayName() instanceof TextComponent)) {
                return;
            }

            editObjectiveAttributeInventory(player, playerId, clickedItem);

        } catch (Exception ignored) {
            // Exception ignored, because it is not necessary to handle it (Only happens if player spam accepts EditQuestsUtils.quests)
        }
    }

    private void editObjectiveAttributeInventory(Player player, UUID playerId, ItemStack clickedItem) {
        Map<String, CompletableFuture<String>> futures = new HashMap<>();
        actionMap.keySet().forEach(key -> futures.put(key, configHandler.getStringAsync(key)));

        EditUtils.handleClickedItem(player, clickedItem, futures, actionMap,
                () -> EditObjectivesUtils.editingObjective.get(playerId));
    }

    private void editObjectiveValue(Player player, Objective objective) {
        editObjectiveWithAnvil(player, objective, "inventory.edit-objective.value.change-message", objective.getValue(), objective::setValue);
    }

    private void editObjectiveTypeId(Player player, Objective objective) {
        editObjectiveWithAnvil(player, objective, "inventory.edit-objective.type.change-message", objective.getType(), objective::setType);
    }

    private void editObjectiveAmount(Player player, Objective objective) {
        editObjectiveWithAnvil(player, objective, "inventory.edit-objective.count.change-message", String.valueOf(objective.getCount()), text -> objective.setCount(Integer.parseInt(text)));
    }

    private void editObjectiveName(Player player, Objective objective) {
        editObjectiveWithAnvil(player, objective, "inventory.edit-objective.name.change-message", objective.getObjective(), objective::setObjective);
    }

    private void editObjectiveWithAnvil(Player player, Objective objective, String configKey, String initialText, Consumer<String> onChange) {
        CompletableFuture<String> changeMsgFuture = configHandler.getStringAsync(configKey);
        changeMsgFuture.thenAcceptAsync(changeMsg -> Bukkit.getScheduler().runTask(questSystem, () -> new AnvilGUI.Builder()
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }
                    return Arrays.asList(
                            AnvilGUI.ResponseAction.close(),
                            AnvilGUI.ResponseAction.run(() -> onChange.accept(stateSnapshot.getText()))
                    );
                })
                .onClose(player1 -> EditObjectivesUtils.editObjectivesInventory(objective, player).thenAccept(v -> Bukkit.getScheduler().runTask(questSystem, () -> player.openInventory(EditObjectivesUtils.editObjectiveInventory.get(player.getUniqueId())))))
                .text(initialText)
                .title(changeMsg)
                .plugin(questSystem)
                .open(player))).exceptionally(ex -> {
            logger.error(ex.getMessage(), ex);
            return null;
        });
    }

    private void cancelEdit(Player player, Quest quest) {
        EditObjectivesUtils.editObjectiveInventory.remove(player.getUniqueId());
        EditObjectivesUtils.editingObjective.remove(player.getUniqueId());
        EditUtils.navigateBack(player, quest, true);
    }

    private void saveQuest(Player player, Quest quest, Objective objective) {
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
                EditUtils.updateAndReloadQuest(player, quest, databaseHandler.updateAsync(quest),true);
            }).exceptionally(ex -> {
                logger.error(ex.getMessage(), ex);
                return null;
            });
        }else{
            EditUtils.updateAndReloadQuest(player, quest, databaseHandler.updateAsync(objective),true);
        }

    }
}
