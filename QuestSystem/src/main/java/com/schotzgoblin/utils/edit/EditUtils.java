package com.schotzgoblin.utils.edit;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.database.Identifiable;
import com.schotzgoblin.database.Objective;
import com.schotzgoblin.database.Quest;
import com.schotzgoblin.database.Reward;
import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.QuestSystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tsp.headdb.core.api.HeadAPI;
import tsp.headdb.implementation.head.Head;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class EditUtils {

    private static final Logger logger = LoggerFactory.getLogger(EditUtils.class);

    private static final QuestSystem questSystem = QuestSystem.getInstance();
    private static final DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
    private static final ConfigHandler configHandler = ConfigHandler.getInstance();
    public static Map<UUID, Integer> playerPage = Collections.synchronizedMap(new HashMap<>());
    public static final int pageSize = 36;

    public static void handleQuestItemDelete(Player player, TextComponent displayName, Inventory inv, String type) {
        CompletableFuture<? extends Identifiable> itemFuture = Objects.equals(type, "reward")
                ? databaseHandler.getRewardByNameAsync(displayName.content())
                : Objects.equals(type, "objective")
                ? databaseHandler.getObjectiveByNameAsync(displayName.content())
                : databaseHandler.getQuestByNameAsync(displayName.content());

        CompletableFuture<String> deleteSoundFuture = configHandler.getStringAsync("quest-manager.lore-entries.delete.sound");
        CompletableFuture<Integer> deleteVolumeFuture = configHandler.getIntAsync("quest-manager.lore-entries.delete.volume");
        CompletableFuture<Integer> deletePitchFuture = configHandler.getIntAsync("quest-manager.lore-entries.delete.pitch");

        CompletableFuture.allOf(
                itemFuture, deleteSoundFuture, deleteVolumeFuture, deletePitchFuture
        ).thenAccept(x -> {
            var item = itemFuture.join();
            String deleteSound = deleteSoundFuture.join();
            int deleteVolume = deleteVolumeFuture.join();
            int deletePitch = deletePitchFuture.join();

            if (item == null) return;

            if (type.equals("quest") || type.equals("reward")) {
                databaseHandler.deleteAllQuestRewardsAsync(item);
            }
            if (type.equals("quest")) {
                databaseHandler.deleteAllPlayerQuests(item);
            }
            if (type.equals("objective")) {
                databaseHandler.changeQuestObjectives(item);
            }
            databaseHandler.deleteAsync(item);

            if (type.equals("reward")) {
                EditRewardsUtils.rewards.remove((Reward) item);
                Bukkit.getScheduler().runTask(questSystem, () -> {
                    player.playSound(player.getLocation(), Sound.valueOf(deleteSound), deleteVolume, deletePitch);
                    EditRewardsUtils.refreshInventory(EditRewardsUtils.rewards, EditQuestsUtils.editingQuest.get(player.getUniqueId()), inv, player);
                });
            } else if (type.equals("quest")) {
                EditQuestsUtils.quests.remove((Quest) item);
                Bukkit.getScheduler().runTask(questSystem, () -> {
                    player.playSound(player.getLocation(), Sound.valueOf(deleteSound), deleteVolume, deletePitch);
                    EditQuestsUtils.refreshInventory(EditQuestsUtils.quests, inv, player);
                });
            } else {
                EditObjectivesUtils.objectives.remove((Objective) item);
                Bukkit.getScheduler().runTask(questSystem, () -> {
                    player.playSound(player.getLocation(), Sound.valueOf(deleteSound), deleteVolume, deletePitch);
                    EditObjectivesUtils.refreshInventory(EditObjectivesUtils.objectives, EditQuestsUtils.editingQuest.get(player.getUniqueId()), inv, player);
                });
            }
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(), ex);  // Use logger.error if applicable
            return null;
        });
    }

    public static void handlePageSwitch(Player player, Inventory inv, String content) {
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


            if (content.equals(nextPageName.replace("%page%", EditUtils.getNextPage(player, EditObjectivesUtils.objectives.size())))) {
                int currentPage = EditUtils.playerPage.get(player.getUniqueId());
                int totalPages = EditUtils.getMaxPage(EditObjectivesUtils.objectives.size());
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
                EditObjectivesUtils.refreshInventory(EditObjectivesUtils.objectives, EditQuestsUtils.editingQuest.get(player.getUniqueId()), inv, player);
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
                EditObjectivesUtils.refreshInventory(EditObjectivesUtils.objectives, EditQuestsUtils.editingQuest.get(player.getUniqueId()), inv, player);
            }
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(), ex);
            return null;
        });

    }

    public static void setItemLore(ItemStack item, List<TextComponent> loreLines) {
        ItemMeta meta = item.getItemMeta();
        meta.lore(loreLines);
        item.setItemMeta(meta);
    }

    public static ItemStack createItem(String materialName, String displayName, String colorHex) {
        Material material = Material.getMaterial(materialName);
        if (material == null) {
            throw new IllegalArgumentException("Invalid material name: " + materialName);
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(displayName, TextColor.fromCSSHexString(colorHex)));
        item.setItemMeta(meta);
        return item;
    }

    public static Inventory createInventory(String title, String colour, int size) {
        return Bukkit.createInventory(null, size, Component.text(title, TextColor.fromCSSHexString(colour)));
    }

    public static CompletableFuture<Void> initTopInventoryRow(Inventory inventory, Player player, String type, int size) {
        CompletableFuture<String> nextPageNameFuture = configHandler.getStringAsync("inventory.next-page.display-name");
        CompletableFuture<String> nextPageColourFuture = configHandler.getStringAsync("inventory.next-page.colour");

        CompletableFuture<String> backNameFuture = configHandler.getStringAsync("inventory.back.display-name");
        CompletableFuture<String> backColourFuture = configHandler.getStringAsync("inventory.back.colour");

        CompletableFuture<String> prevPageNameFuture = configHandler.getStringAsync("inventory.prev-page.display-name");
        CompletableFuture<String> prevPageColourFuture = configHandler.getStringAsync("inventory.prev-page.colour");

        CompletableFuture<String> createNameFuture = configHandler.getStringAsync("inventory.create-" + type + ".display-name");
        CompletableFuture<Material> createMaterialFuture = configHandler.getMaterialAsync("inventory.create-" + type + ".material");
        CompletableFuture<String> createColourFuture = configHandler.getStringAsync("inventory.create-" + type + ".colour");

        return CompletableFuture.allOf(
                backColourFuture, backNameFuture,
                nextPageNameFuture, nextPageColourFuture,
                prevPageNameFuture, prevPageColourFuture,
                createNameFuture, createMaterialFuture, createColourFuture
        ).thenAccept(voidResult -> {
            try {
                String nextPageName = nextPageNameFuture.get();
                var nextPageColour = TextColor.fromHexString(nextPageColourFuture.get());

                String prevPageName = prevPageNameFuture.get();
                var prevPageColour = TextColor.fromHexString(prevPageColourFuture.get());

                String backName = backNameFuture.get();
                var backColour = TextColor.fromHexString(backColourFuture.get());
                var backItem = new ItemStack(Material.BARRIER);
                ItemMeta backMeta = backItem.getItemMeta();
                backMeta.displayName(Component.text(backName, backColour));
                backItem.setItemMeta(backMeta);


                String createName = createNameFuture.get();
                Material createMaterial = createMaterialFuture.get();
                var createColour = TextColor.fromHexString(createColourFuture.get());
                var page = EditUtils.playerPage.get(player.getUniqueId()) + "";
                ItemStack prevPageItem;
                ItemStack nextPageItem;
                if (!getNextPage(player, size).equals(page)) {
                    nextPageItem = getPlayerHead(60386, nextPageName.replace("%page%", getNextPage(player, size)), nextPageColour);
                } else {
                    nextPageItem = new ItemStack(Material.AIR);
                }
                if (!getPrevPage(player).equals(page)) {
                    prevPageItem = getPlayerHead(60384, prevPageName.replace("%page%", getPrevPage(player)), prevPageColour);
                } else {
                    prevPageItem = new ItemStack(Material.AIR);
                }

                ItemStack createQuestItem = new ItemStack(createMaterial);
                ItemMeta createQuestMeta = createQuestItem.getItemMeta();
                createQuestMeta.displayName(Component.text(createName, createColour));
                createQuestItem.setItemMeta(createQuestMeta);

                if (Objects.equals(type, "reward") || Objects.equals(type, "objective"))
                    inventory.setItem(0, backItem);
                inventory.setItem(1, prevPageItem);
                inventory.setItem(4, createQuestItem);
                inventory.setItem(7, nextPageItem);

            } catch (InterruptedException | ExecutionException e) {
                logger.error(e.getMessage(), e); // Handle error fetching config values
            }
        });
    }

    public static String getPrevPage(Player player) {
        var page = playerPage.get(player.getUniqueId());
        if (page == 1) {
            return page + "";
        }
        return String.valueOf(page - 1);
    }

    public static String getNextPage(Player player, int size) {
        var page = EditUtils.playerPage.get(player.getUniqueId());
        if (page == getMaxPage(size)) {
            return String.valueOf(page);
        }
        return String.valueOf(page + 1);
    }

    public static int getMaxPage(int size) {
        return (int) (double) (size / pageSize) + (size % pageSize == 0 ? 0 : 1);
    }

    public static <T> void handleClickedItem(Player player, ItemStack clickedItem,
                                             Map<String, CompletableFuture<String>> futures,
                                             Map<String, BiConsumer<Player, T>> actionMap,
                                             Supplier<T> getObject) {
        CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).thenAcceptAsync(v -> {
            try {
                T obj = getObject.get();
                String clickedMaterialName = clickedItem.getType().name();

                for (Map.Entry<String, CompletableFuture<String>> entry : futures.entrySet()) {
                    String materialName = entry.getValue().join();
                    if (clickedMaterialName.equals(materialName)) {
                        actionMap.get(entry.getKey()).accept(player, obj);
                        break;
                    }
                }
            } catch (CompletionException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }).exceptionally(ex -> {
            logger.error(ex.getMessage(), ex);
            return null;
        });
    }

    public static ItemStack getPlayerHead(int id, String displayName, TextColor colour) {
        Optional<Head> headOptional = HeadAPI.getHeadById(id);

        if (headOptional.isPresent()) {
            Head head = headOptional.get();
            ItemStack playerHead = head.getItem(UUID.randomUUID());
            ItemMeta playerHeadMeta = playerHead.getItemMeta();

            playerHeadMeta.displayName(Component.text(displayName, colour));
            playerHeadMeta.lore(List.of(Component.text("")));

            playerHead.setItemMeta(playerHeadMeta);
            return playerHead;
        } else {
            ItemStack defaultHead = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta defaultHeadMeta = defaultHead.getItemMeta();

            defaultHeadMeta.displayName(Component.text(displayName, colour));
            defaultHeadMeta.lore(List.of(Component.text("")));

            defaultHead.setItemMeta(defaultHeadMeta);
            return defaultHead;
        }
    }

    public static void updateAndReloadQuest(Player player, Quest quest, @NotNull CompletableFuture<Void> voidCompletableFuture, boolean isObjective) {
        voidCompletableFuture.thenAccept(v2 -> navigateBack(player, quest, isObjective)).exceptionally(ex -> {
            logger.error(ex.getMessage(), ex);
            return null;
        });
    }

    public static void navigateBack(Player player, Quest quest, boolean isObjective) {
        CompletableFuture<Void> future;
        if (isObjective)
            future = EditObjectivesUtils.refreshObjectives(quest);
        else
            future = EditRewardsUtils.refreshRewards(quest);

        future.thenAccept(v3 -> Bukkit.getScheduler().runTask(questSystem, () -> {
            player.closeInventory();
            if (isObjective)
                player.openInventory(EditObjectivesUtils.allObjectivesInventory.get(player.getUniqueId()));
            else
                player.openInventory(EditRewardsUtils.allRewardsInventory.get(player.getUniqueId()));
        })).exceptionally(ex -> {
            logger.error(ex.getMessage(), ex);
            return null;
        });
    }
}
