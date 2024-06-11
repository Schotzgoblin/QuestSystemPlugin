package com.schotzgoblin.main;

import com.schotzgoblin.database.PlayerQuest;
import com.schotzgoblin.database.Quest;
import com.schotzgoblin.database.Reward;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static com.schotzgoblin.main.Utils.getTimeStringFromSecs;

public class QuestManager implements Listener {
    public final QuestSystem plugin;
    private final DatabaseHandler databaseHandler;
    public Map<UUID, InventoryMapping> inventories = new HashMap<>();
    public Map<UUID, List<BossBar>> bossBars = new HashMap<>();
    private final Map<PlayerQuest, BukkitRunnable> runnables = new HashMap<>();
    private final FileConfiguration config;

    public QuestManager(QuestSystem plugin, DatabaseHandler databaseHandler) {
        this.plugin = plugin;
        this.databaseHandler = databaseHandler;
        this.config = plugin.getConfig();
    }

    public void initInventory(Player player, String type) {
        Inventory inventory = createInventory();
        ItemStack blueGlassPane = createColoredGlassPane(Material.BLUE_STAINED_GLASS_PANE, "All Quests");
        ItemStack whiteGlassPane = createColoredGlassPane(Material.WHITE_STAINED_GLASS_PANE, "NOT_STARTED");
        ItemStack redGlassPane = createColoredGlassPane(Material.RED_STAINED_GLASS_PANE, "CANCELED");
        ItemStack orangeGlassPane = createColoredGlassPane(Material.ORANGE_STAINED_GLASS_PANE, "IN_PROGRESS");
        ItemStack greenGlassPane = createColoredGlassPane(Material.GREEN_STAINED_GLASS_PANE, "COMPLETED");

        inventory.setItem(2, blueGlassPane);
        inventory.setItem(3, whiteGlassPane);
        inventory.setItem(4, orangeGlassPane);
        inventory.setItem(5, greenGlassPane);
        inventory.setItem(6, redGlassPane);

        if(!inventories.containsKey(player.getUniqueId()))
            inventories.put(player.getUniqueId(), addQuestsToInventory(type, player, inventory));
    }

    private Inventory createInventory() {
        return Bukkit.createInventory(null, 54, Component.text("Quests", TextColor.color(0, 170, 255)));
    }

    private ItemStack createColoredGlassPane(Material material, String displayName) {
        ItemStack glassPane = new ItemStack(material, 1);
        ItemMeta glassPaneMeta = glassPane.getItemMeta();
        glassPaneMeta.displayName(Component.text(displayName));
        glassPane.setItemMeta(glassPaneMeta);
        return glassPane;
    }

    public InventoryMapping addQuestsToInventory(String type, Player player, Inventory inventory) {
        var inventoryMapping = new InventoryMapping();
        if(inventories.containsKey(player.getUniqueId())){
            inventoryMapping = inventories.get(player.getUniqueId());
        }else{
            inventoryMapping = new InventoryMapping(inventory, type, getMenuSlotFromType(type));
        }
        inventoryMapping.setType(type);
        resetInventory(inventory);
        List<Quest> quests = databaseHandler.getAllQuests();
        List<PlayerQuest> playerQuests = databaseHandler.getPlayerQuests(player.getUniqueId(), type);
        if (!type.equals("All Quests")) {
            if(type.equals("NOT_STARTED")){
                quests = quests.stream()
                        .filter(quest -> !playerQuests.stream().map(PlayerQuest::getQuestId).toList().contains(quest.getId())).toList();
            }else {
                quests = playerQuests.stream()
                        .map(PlayerQuest::getQuest)
                        .toList();
            }
        }

        inventory.setItem(inventoryMapping.getMenuSlot()+9, new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
        for (int i = 0; i < quests.size(); i++) {
            Quest quest = quests.get(i);
            ItemStack itemStack = createQuestItemStack(quest, type, player);
            inventory.setItem(i + 27, itemStack);
        }

        finaliseInventory(inventory);
        return inventoryMapping;
    }

    private int getMenuSlotFromType(String type) {
        return switch (type) {
            case "NOT_STARTED" -> 3;
            case "IN_PROGRESS" -> 4;
            case "COMPLETED" -> 5;
            case "CANCELED" -> 6;
            default -> 2;
        };
    }

    private ItemStack createQuestItemStack(Quest quest, String type, Player player) {
        var mainQuestColor = TextColor.color(255,255,255);

        ItemStack itemStack = new ItemStack(Material.PAPER);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text(quest.getName(), mainQuestColor));

        List<Component> lore = new ArrayList<>();
        var component = getQuestStatusComponent(type, quest, player);
        var rewards = databaseHandler.getQuestRewards(quest);
        var playerQuest = databaseHandler.getPlayerQuestByQuestId(player.getUniqueId(), quest.getId());

        if(((TextComponent)component).content().equals("Not Started")) {
            lore.add(Component.text(Objects.requireNonNull(config.getString("quest-manager.quest-click-not-started"))));
        } else if(((TextComponent)component).content().equals("In Progress")) {
            lore.add(Component.text(Objects.requireNonNull(config.getString("quest-manager.quest-click-in-progress"))));
        } else if(((TextComponent)component).content().equals("Completed")) {
            lore.add(Component.text(Objects.requireNonNull(config.getString("quest-manager.quest-click-completed"))));
        } else if(((TextComponent)component).content().equals("Canceled")) {
            lore.add(Component.text(Objects.requireNonNull(config.getString("quest-manager.quest-click-canceled"))));
        }
        lore.add(Component.text(""));
        lore.add(Component.text("Status:",mainQuestColor));
        lore.add(component);
        lore.add(Component.text(""));
        lore.add(Component.text("Description:",mainQuestColor));
        lore.add(Component.text(quest.getDescription()));
        lore.add(Component.text(""));
        lore.add(Component.text("Objective:",mainQuestColor));
        lore.add(Component.text(quest.getObjective().getObjective()));
        lore.add(Component.text(""));
        lore.add(Component.text("Time limit:",mainQuestColor));
        lore.add(Component.text(Utils.getTimeStringFromSecs(quest.getTimeLimit())));
        if(((TextComponent)component).content().equals("In Progress")) {
            lore.add(Component.text(""));
            lore.add(Component.text("Time left:", mainQuestColor));
            lore.add(Component.text(Utils.getTimeStringFromSecs(quest.getTimeLimit() - playerQuest.getTime())));
        }
        lore.add(Component.text(""));
        lore.add(Component.text("Rewards: ",mainQuestColor));
        for (Reward reward : rewards) {
            lore.add(Component.text(reward.getName(),TextColor.color(0,255,0)));
        }

        itemMeta.lore(lore);
        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    private void finaliseInventory(Inventory inventory) {
        if(inventory == null) return;
        for (int i = 0; i < 54; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
            }
        }
    }

    private void resetInventory(Inventory inventory) {
        if(inventory == null) return;
        for (int i = 9; i < 54; i++) {
            inventory.setItem(i, new ItemStack(Material.AIR));
        }
    }

    private Component getQuestStatusComponent(String type, Quest quest, Player player) {
        if (!type.equals("All Quests")) {
            return getComponentFromType(type);
        } else {
            List<PlayerQuest> allPlayerQuests = databaseHandler.getPlayerQuests(player.getUniqueId(), "NOT_STARTED");
            PlayerQuest playerQuest = allPlayerQuests.stream()
                    .filter(playerQuest1 -> playerQuest1.getQuest().getId() == quest.getId())
                    .findFirst()
                    .orElse(null);
            if(playerQuest == null) return getComponentFromType("NOT_STARTED");
            return getComponentFromType(playerQuest.getQuestStatus().getStatus());
        }
    }

    private TextComponent getComponentFromType(String type) {
        return switch (type) {
            case "CANCELED" -> Component.text("Canceled", TextColor.color(255, 0, 0)); // Red
            case "IN_PROGRESS" -> Component.text("In Progress", TextColor.color(255, 165, 0)); // Orange
            case "COMPLETED" -> Component.text("Completed", TextColor.color(0, 255, 0)); // Green
            default -> Component.text("Not Started", TextColor.color(100, 255, 255)); // White
        };
    }

    public void reactivateQuest(Player player, TextComponent displayname) {
        var quest = databaseHandler.getQuestByName(displayname.content());
        databaseHandler.changePlayerQuestType(player.getUniqueId(), displayname.content(), "IN_PROGRESS", player.getLocation());
        player.sendMessage(config.getString("quest-manager.quest-reactivated") + displayname.content());
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.0f);
        createAndShowBossBar(player,displayname.content(), 0.0f);
        var playerQuest = databaseHandler.getPlayerQuestByQuestId(player.getUniqueId(), quest.getId());
        if (runnables.containsKey(playerQuest)) {
            runnables.get(playerQuest).cancel();
            runnables.remove(playerQuest);
        }
        BukkitRunnable task = new QuestTimerTask(player, displayname.content(), quest.getId());
        runnables.put(playerQuest, task);
        task.runTaskTimer(plugin, 0L, 20L);
    }

    public void cancelQuest(Player player, TextComponent displayname) {
        databaseHandler.changePlayerQuestType(player.getUniqueId(), displayname.content(), "CANCELED",player.getLocation());
        int questId = databaseHandler.getQuestByName(displayname.content()).getId();
        var playerQuest = databaseHandler.getPlayerQuestByQuestId(player.getUniqueId(), questId);
        if (runnables.containsKey(playerQuest)) {
            runnables.get(playerQuest).cancel();
            runnables.remove(playerQuest);
        }
        player.sendMessage(config.getString("quest-manager.quest-canceled") + displayname.content());
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.0f);
        hideBossBar(player, displayname.content());
    }

    public void acceptQuest(Player player, TextComponent displayname, String objective) {
        var quest = databaseHandler.getQuestByName(displayname.content());

        databaseHandler.addPlayerQuest(player.getUniqueId(), displayname.content(),player.getLocation());
        player.sendMessage(config.getString("quest-manager.quest-accepted") + displayname.content());
        player.sendMessage(config.getString("quest-manager.quest-objective") + objective);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.0f);
        createAndShowBossBar(player, displayname.content(), 0.0f);
        BukkitRunnable task = new QuestTimerTask(player, displayname.content(), quest.getId());
        runnables.put(databaseHandler.getPlayerQuestByQuestId(player.getUniqueId(), quest.getId()), task);
        task.runTaskTimer(plugin, 0L, 20L);
    }

    public void createAndShowBossBar(Player player, String title, float progress) {
        var bossbar = BossBar.bossBar(Component.text(title), progress, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
        if(bossBars.containsKey(player.getUniqueId())) {
            bossBars.get(player.getUniqueId()).add(bossbar);
        } else
            bossBars.put(player.getUniqueId(), new ArrayList<>(List.of(bossbar)));
        player.showBossBar(bossbar);
    }

    private BossBar getBossBarFromTitle(Player player, String title) {
       return bossBars.get(player.getUniqueId()).stream().filter(bossBar -> ((TextComponent)bossBar.name()).content().equals(title)).findFirst().orElse(null);
    }

    public void updateBossBar(Player player, PlayerQuest playerQuest, float progress) {
        var title = playerQuest.getQuest().getName();
        var bossbar = getBossBarFromTitle(player, title);
        bossbar.progress(progress);
        if(progress == 1.0f) {
            bossbar.color(BossBar.Color.GREEN);
            databaseHandler.update(playerQuest);
            completeQuest(player, title);
        }
    }

    private void completeQuest(Player player, String title) {
        var questId = databaseHandler.getQuestByName(title).getId();
        var playerQuest = databaseHandler.getPlayerQuestByQuestId(player.getUniqueId(),questId);
        databaseHandler.changePlayerQuestType(player.getUniqueId(),title,"COMPLETED",player.getLocation());
        player.sendMessage(config.getString("quest-manager.quest-completed") + title);
        Utils.sendAlertToPlayer("Quest Completed", config.getString("quest-manager.quest-completed") + title, 1000, 4000, 1000, Color.fromRGB(0,255,0), player);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        runnables.get(playerQuest).cancel();
        runnables.remove(playerQuest);
        giveRewards(player, playerQuest);
        synchronized (player) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                hideBossBar(player, title);
            }, 100);
        }
    }

    private void giveRewards(Player player, PlayerQuest playerQuest) {
        var quest = playerQuest.getQuest();
        var rewards = databaseHandler.getQuestRewards(quest);
        for (Reward reward : rewards) {
            var rewardSplit = reward.getRewardType().getName();
            switch (rewardSplit) {
                case "XP" -> player.giveExp(reward.getAmount());
                case "ITEM" -> {
                    ItemStack itemStack = new ItemStack(Objects.requireNonNull(Material.getMaterial(reward.getValue())), reward.getAmount());
                    player.getInventory().addItem(itemStack);
                }
            }
            player.sendMessage(config.getString("quest-manager.quest-rewards") + reward.getName());
        }
    }

    public void hideBossBar(Player player, String title) {
        var bossBar = getBossBarFromTitle(player, title);
        player.hideBossBar(bossBar);
        bossBars.get(player.getUniqueId()).remove(bossBar);
    }

    public void setupInventory(Player player, String type) {
        initInventory(player, type);
        Inventory inventory = inventories.get(player.getUniqueId()).getInventory();
        player.openInventory(inventory);
        inventories.get(player.getUniqueId()).setInventory(inventory);
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent e) {
        var playerQuests = databaseHandler.getPlayerQuests(e.getPlayer().getUniqueId(), "IN_PROGRESS");
        if(playerQuests.isEmpty()){
            initInventory(e.getPlayer(),"All Quests");
        }else{
            initInventory(e.getPlayer(),"IN_PROGRESS");
        }
        for (PlayerQuest playerQuest : playerQuests) {
            BukkitRunnable task = new QuestTimerTask(e.getPlayer(), playerQuest.getQuest().getName(), playerQuest.getQuest().getId());
            runnables.put(playerQuest, task);
            task.runTaskTimer(plugin, 0L, 20L);
        }
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent e) {
        var playerQuests = databaseHandler.getPlayerQuests(e.getPlayer().getUniqueId(), "IN_PROGRESS");
        for (PlayerQuest playerQuest : playerQuests) {
            runnables.get(playerQuest).cancel();
            runnables.remove(playerQuest);
        }
    }
    private class QuestTimerTask extends BukkitRunnable {
        private final Player player;
        private final String questName;
        private final int questId;

        public QuestTimerTask(Player player, String questName, int questId) {
            this.player = player;
            this.questName = questName;
            this.questId = questId;
        }

        @Override
        public void run() {
            var playerQuest = databaseHandler.getPlayerQuestByQuestId(player.getUniqueId(), questId);
            var inv = new InventoryMapping();
            if(inventories.containsKey(player.getUniqueId())){
                inv = inventories.get(player.getUniqueId());
            }
            if (playerQuest.getQuestStatus().getStatus().equals("IN_PROGRESS")) {
                playerQuest.setTime(playerQuest.getTime() + 1);
                databaseHandler.update(playerQuest);
                var quest = databaseHandler.getQuestByName(questName);
                if (playerQuest.getTime() >= quest.getTimeLimit()) {
                    Utils.sendAlertToPlayer("Quest Failed", config.getString("quest-manager.quest-failed") + questName + config.getString("quest-manager.quest-time-limit") + getTimeStringFromSecs(quest.getTimeLimit()), 1000, 4000, 1000, Color.fromRGB(255,0,0), player);
                    player.sendMessage(config.getString("quest-manager.quest-failed") + questName + config.getString("quest-manager.quest-time-limit") + getTimeStringFromSecs(quest.getTimeLimit()));
                    player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 0.0f);
                    cancel();
                    runnables.remove(playerQuest);
                    hideBossBar(player, questName);
                    databaseHandler.changePlayerQuestType(player.getUniqueId(), questName, "CANCELED",player.getLocation());
                }
            }else{
                cancel();
                runnables.remove(playerQuest);
            }
            if(inv.getInventory()!=null)
                addQuestsToInventory(inv.getType(), player, inv.getInventory());
        }
    }
}
