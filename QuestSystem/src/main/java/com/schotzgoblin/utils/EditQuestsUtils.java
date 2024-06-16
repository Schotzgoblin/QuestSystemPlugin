package com.schotzgoblin.utils;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.database.Quest;
import com.schotzgoblin.database.Reward;
import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.QuestManager;
import com.schotzgoblin.main.QuestSystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tsp.headdb.core.api.HeadAPI;
import tsp.headdb.implementation.head.Head;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class EditQuestsUtils {
    public static Map<UUID, Inventory> allQuestsInventory = Collections.synchronizedMap(new HashMap<>());
    public static Map<UUID, Inventory> editQuestInventory = Collections.synchronizedMap(new HashMap<>());
    public static Map<UUID, Integer> playerPage = Collections.synchronizedMap(new HashMap<>());
    public static Map<UUID, Quest> editingQuest = Collections.synchronizedMap(new HashMap<>());
    public static List<Quest> quests = Collections.synchronizedList(new ArrayList<>());
    private static final QuestSystem questSystem = QuestSystem.getInstance();
    private static final DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
    private static final ConfigHandler configHandler = ConfigHandler.getInstance();
    private static final QuestManager questManager = QuestManager.getInstance();
    private static final int questsPageSize = 36;

    public static void refreshInventory(List<Quest> quests, Inventory inventory, Player player) {
        initTopInventoryRow(inventory,player).join();

        addAllQuestsToInventory(quests, inventory, player);

        questManager.finaliseInventory(inventory,54);
    }

    public static Inventory createInventory(String title, String colour, int size) {
        return Bukkit.createInventory(null, size, Component.text(title, TextColor.fromCSSHexString(colour)));
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
            EditQuestsUtils.playerPage.put(player.getUniqueId(), 1);
            var inventory = createInventory(inventoryTitle, inventoryColour, 9 * 6);
            refreshInventory(quests, inventory, player);
            return CompletableFuture.completedFuture(null);
        });
    }

    private static void addAllQuestsToInventory(List<Quest> quests, Inventory inventory, Player player) {
        int startIndex = (EditQuestsUtils.playerPage.get(player.getUniqueId())-1) * questsPageSize;
        int endIndex = startIndex + questsPageSize;


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

    private static CompletableFuture<Void> initTopInventoryRow(Inventory inventory, Player player) {
        CompletableFuture<String> nextPageNameFuture = configHandler.getStringAsync("inventory.next-page.display-name");
        CompletableFuture<String> nextPageColourFuture = configHandler.getStringAsync("inventory.next-page.colour");

        CompletableFuture<String> prevPageNameFuture = configHandler.getStringAsync("inventory.prev-page.display-name");
        CompletableFuture<String> prevPageColourFuture = configHandler.getStringAsync("inventory.prev-page.colour");

        CompletableFuture<String> createQuestNameFuture = configHandler.getStringAsync("inventory.create-quest.display-name");
        CompletableFuture<Material> createQuestMaterialFuture = configHandler.getMaterialAsync("inventory.create-quest.material");
        CompletableFuture<String> createQuestColourFuture = configHandler.getStringAsync("inventory.create-quest.colour");

        return CompletableFuture.allOf(
                nextPageNameFuture, nextPageColourFuture,
                prevPageNameFuture, prevPageColourFuture,
                createQuestNameFuture, createQuestMaterialFuture, createQuestColourFuture
        ).thenAccept(voidResult -> {
            try {
                String nextPageName = nextPageNameFuture.get();
                var nextPageColour = TextColor.fromHexString(nextPageColourFuture.get());

                String prevPageName = prevPageNameFuture.get();
                var prevPageColour = TextColor.fromHexString(prevPageColourFuture.get());

                String createQuestName = createQuestNameFuture.get();
                Material createQuestMaterial = createQuestMaterialFuture.get();
                var createQuestColour = TextColor.fromHexString(createQuestColourFuture.get());
                var page = EditQuestsUtils.playerPage.get(player.getUniqueId())+"";
                ItemStack prevPageItem;
                ItemStack nextPageItem;
                if(!getNextPage(player).equals(page)){
                    nextPageItem = getPlayerHead(60386, nextPageName.replace("%page%", getNextPage(player)), nextPageColour);
                }else{
                    nextPageItem = new ItemStack(Material.AIR);
                }
                if(!getPrevPage(player).equals(page)) {
                    prevPageItem = getPlayerHead(60384, prevPageName.replace("%page%", getPrevPage(player)), prevPageColour);
                }else{
                    prevPageItem = new ItemStack(Material.AIR);
                }

                ItemStack createQuestItem = new ItemStack(createQuestMaterial);
                ItemMeta createQuestMeta = createQuestItem.getItemMeta();
                createQuestMeta.displayName(Component.text(createQuestName, createQuestColour));
                createQuestItem.setItemMeta(createQuestMeta);

                inventory.setItem(1, prevPageItem);
                inventory.setItem(4, createQuestItem);
                inventory.setItem(7, nextPageItem);

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace(); // Handle error fetching config values
            }
        });
    }

    public static String getPrevPage(Player player) {
        var page = EditQuestsUtils.playerPage.get(player.getUniqueId());
        if (page == 1) {
            return page + "";
        }
        return String.valueOf(page - 1);
    }

    public static String getNextPage(Player player) {
        var page = EditQuestsUtils.playerPage.get(player.getUniqueId());
        if(page == getMaxPage()) {
            return String.valueOf(page);
        }
        return String.valueOf(page + 1);
    }

    public static int getMaxPage() {
        return (int) (double) (EditQuestsUtils.quests.size() / questsPageSize)+(EditQuestsUtils.quests.size() % questsPageSize == 0 ? 0 : 1);
    }

    private static ItemStack getPlayerHead(int id, String displayName, TextColor colour) {
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

            defaultHeadMeta.displayName(Component.text("Default Head", TextColor.color(0xFF0000)));
            defaultHeadMeta.lore(List.of(Component.text("Default head for missing data")));

            defaultHead.setItemMeta(defaultHeadMeta);
            return defaultHead;
        }
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


}
