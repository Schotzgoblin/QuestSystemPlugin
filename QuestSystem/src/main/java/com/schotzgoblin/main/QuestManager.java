package com.schotzgoblin.main;

import com.schotzgoblin.database.PlayerQuest;
import com.schotzgoblin.database.Quest;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
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
    private final QuestSystem plugin;
    private final DatabaseHandler databaseHandler;
    public Map<UUID, InventoryMapping> inventories = new HashMap<>();
    public Map<UUID, List<BossBar>> bossBars = new HashMap<>();
    private List<Quest> quests;
    private final Map<PlayerQuest, BukkitRunnable> runnables = new HashMap<>();

    public QuestManager(QuestSystem plugin, DatabaseHandler databaseHandler) {
        this.plugin = plugin;
        this.databaseHandler = databaseHandler;
        this.quests = new ArrayList<>();
        loadQuests();
    }

    private void loadQuests() {
        this.quests = databaseHandler.getAll(Quest.class);
    }

    public void deleteQuest(String name) {
        Quest savedQuest = databaseHandler.getQuestByName(name);
        databaseHandler.delete(savedQuest);
        reloadAllInventorys();
        loadQuests();
    }

    private void reloadAllInventorys() {
        inventories.forEach((uuid, inventoryMapping) -> addQuestsToInventory(inventoryMapping.getType(), Objects.requireNonNull(Bukkit.getPlayer(uuid)), inventoryMapping.getInventory()));
    }

    public Quest getQuest(String name) {
        return quests.stream().filter(quest -> quest.getName().equals(name)).findFirst().orElse(null);
    }

    public List<Quest> getQuests() {
        return quests;
    }

    public void initInventory(Player player) {
        var inventory = Bukkit.createInventory(null, 54, Component.text("Quests", TextColor.color(0, 170, 255))); // Bright blue color
        ItemStack blueGlassPane = new ItemStack(Material.BLUE_STAINED_GLASS_PANE, 1); // Blue
        ItemStack whiteGlassPane = new ItemStack(Material.WHITE_STAINED_GLASS_PANE, 1); // White
        ItemStack redGlassPane = new ItemStack(Material.RED_STAINED_GLASS_PANE, 1); // Red
        ItemStack orangeGlassPane = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE, 1); // Orange
        ItemStack greenGlassPane = new ItemStack(Material.GREEN_STAINED_GLASS_PANE, 1); // Green

        ItemMeta blueGlassPaneMeta = blueGlassPane.getItemMeta();
        blueGlassPaneMeta.displayName(Component.text("All Quests"));
        blueGlassPane.setItemMeta(blueGlassPaneMeta);

        ItemMeta whiteGlassPaneMeta = whiteGlassPane.getItemMeta();
        whiteGlassPaneMeta.displayName(Component.text("NOT_STARTED"));
        whiteGlassPane.setItemMeta(whiteGlassPaneMeta);

        ItemMeta redGlassPaneMeta = redGlassPane.getItemMeta();
        redGlassPaneMeta.displayName(Component.text("CANCELED"));
        redGlassPane.setItemMeta(redGlassPaneMeta);

        ItemMeta orangeGlassPaneMeta = orangeGlassPane.getItemMeta();
        orangeGlassPaneMeta.displayName(Component.text("IN_PROGRESS"));
        orangeGlassPane.setItemMeta(orangeGlassPaneMeta);

        ItemMeta greenGlassPaneMeta = greenGlassPane.getItemMeta();
        greenGlassPaneMeta.displayName(Component.text("COMPLETED"));
        greenGlassPane.setItemMeta(greenGlassPaneMeta);

        inventory.setItem(2, blueGlassPane);
        inventory.setItem(3, whiteGlassPane);
        inventory.setItem(4, orangeGlassPane);
        inventory.setItem(5, greenGlassPane);
        inventory.setItem(6, redGlassPane);
        if(!inventories.containsKey(player.getUniqueId()))
            inventories.put(player.getUniqueId(),addQuestsToInventory("All Quests", player, inventory));

    }

    public InventoryMapping addQuestsToInventory(String type, Player player, Inventory inventory) {
        var inventoryMapping = new InventoryMapping();
        if(inventories.containsKey(player.getUniqueId())){
            inventoryMapping = inventories.get(player.getUniqueId());
        }else{
            inventoryMapping = new InventoryMapping(inventory, type, 2);
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

    private ItemStack createQuestItemStack(Quest quest, String type, Player player) {
        ItemStack itemStack = new ItemStack(Material.PAPER);
        ItemMeta itemMeta = itemStack.getItemMeta();

        itemMeta.displayName(Component.text(quest.getName()));

        List<Component> lore = new ArrayList<>();
        var component = getQuestStatusComponent(type, quest, player);
        lore.add(component);
        lore.add(Component.text(quest.getDescription(), TextColor.color(128, 128, 128)));
        lore.add(Component.text("Objective: " + quest.getObjective().getObjective(), TextColor.color(0, 128, 128)));
        lore.add(Component.text("Time limit: " + Utils.getTimeStringFromSecs(quest.getTimeLimit()), TextColor.color(128, 79, 0)));
        if(((TextComponent)component).content().equals("Not Started")) {
            lore.add(Component.text("Click to accept", TextColor.color(0, 255, 0)));
        } else if(((TextComponent)component).content().equals("In Progress")) {
            var playerQuest = databaseHandler.getPlayerQuestByQuestId(player.getUniqueId(), quest.getId());
            lore.add(Component.text("Time left: "+Utils.getTimeStringFromSecs(quest.getTimeLimit()-playerQuest.getTime()), TextColor.color(255, 255, 255)));
            lore.add(Component.text("Click to cancel", TextColor.color(255, 0, 0)));
        } else if(((TextComponent)component).content().equals("Completed")) {
            lore.add(Component.text("Quest Completed", TextColor.color(0, 255, 0)));
        } else if(((TextComponent)component).content().equals("Canceled")) {
            lore.add(Component.text("Click to try again", TextColor.color(255, 0, 68)));
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
            default -> Component.text("Not Started", TextColor.color(255, 255, 255)); // White
        };
    }

    public void reactivateQuest(Player player, TextComponent displayname, String objective) {
        var quest = databaseHandler.getQuestByName(displayname.content());
        databaseHandler.changePlayerQuestType(player.getUniqueId(), displayname.content(), "IN_PROGRESS");
        player.sendMessage("You have reactivated the quest: " + displayname.content());
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
        databaseHandler.changePlayerQuestType(player.getUniqueId(), displayname.content(), "CANCELED");
        int questId = databaseHandler.getQuestByName(displayname.content()).getId();
        var playerQuest = databaseHandler.getPlayerQuestByQuestId(player.getUniqueId(), questId);
        if (runnables.containsKey(playerQuest)) {
            runnables.get(playerQuest).cancel();
            runnables.remove(playerQuest);
        }
        player.sendMessage("You have canceled the quest: " + displayname.content());
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.0f);
    }

    public void acceptQuest(Player player, TextComponent displayname, String objective) {
        var quest = databaseHandler.getQuestByName(displayname.content());

        databaseHandler.addPlayerQuest(player.getUniqueId(), displayname.content());
        player.sendMessage("You have accepted the quest: " + displayname.content());
        player.sendMessage("Your objective is to: " + objective);
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
       return bossBars.get(player.getUniqueId()).stream().filter(bossBar -> ((TextComponent)bossBar.name()).content().equals(title)).findFirst().get();
    }

    public void updateBossBar(Player player, String title, float progress) {
        var bossbar = getBossBarFromTitle(player, title);
        bossbar.progress(progress);
        if(progress == 1.0f) {
            bossbar.color(BossBar.Color.GREEN);
            completeQuest(player, title);
        }
    }

    private void completeQuest(Player player, String title) {
        var playerQuest = databaseHandler.getPlayerQuestByQuestId(player.getUniqueId(), databaseHandler.getQuestByName(title).getId());
        databaseHandler.changePlayerQuestType(player.getUniqueId(),title,"COMPLETED");
        player.sendMessage("You have completed the quest: " + title);
        Utils.sendAlertToPlayer("Quest Completed", "You have completed the quest: " + title, 1000, 4000, 1000, Color.fromRGB(0,255,0), player);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        runnables.get(playerQuest).cancel();
        runnables.remove(playerQuest);
        synchronized (player) { // Add a synchronized block
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                hideBossBar(player, title);
            }, 100);
        }
        //TODO give rewards
    }

    public void hideBossBar(Player player, String title) {
        var bossBar = getBossBarFromTitle(player, title);
        player.hideBossBar(bossBar);
        bossBars.get(player.getUniqueId()).remove(bossBar);
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
                    Utils.sendAlertToPlayer("Quest Failed", "You did not complete the quest: " + questName + " in the given time limit of " + getTimeStringFromSecs(quest.getTimeLimit()), 1000, 4000, 1000, Color.fromRGB(255,0,0), player);
                    player.sendMessage("You did not complete the quest: " + questName + " in the given time limit of " + getTimeStringFromSecs(quest.getTimeLimit()));
                    player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 0.0f);
                    cancel();
                    runnables.remove(playerQuest);
                    hideBossBar(player, questName);
                    databaseHandler.changePlayerQuestType(player.getUniqueId(), questName, "CANCELED");
                }
            }else{
                cancel();
                runnables.remove(playerQuest);
            }
            if(inv.getInventory()!=null)
                addQuestsToInventory(inv.getType(), player, inv.getInventory());
        }
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent e) {
        initInventory(e.getPlayer());
        var playerQuests = databaseHandler.getPlayerQuests(e.getPlayer().getUniqueId(), "IN_PROGRESS");
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
            System.out.println(String.join(" ",runnables.keySet().stream().map(x->x.getId()+"").toList()));
            runnables.get(playerQuest).cancel();
            runnables.remove(playerQuest);
        }
    }

}
