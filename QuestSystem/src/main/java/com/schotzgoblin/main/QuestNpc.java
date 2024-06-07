package com.schotzgoblin.main;

import com.schotzgoblin.database.PlayerQuest;
import com.schotzgoblin.database.Quest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class QuestNpc implements Listener {

    private QuestSystem plugin;
    private BukkitTask task;
    private Location npcLocation;
    private Villager npc;
    private FileConfiguration config;
    private DatabaseHandler databaseHandler;
    private Inventory inventory;
    private String type = "All Quests";

    public QuestNpc(QuestSystem plugin, DatabaseHandler databaseHandler) {
        this.plugin = plugin;
        this.databaseHandler = databaseHandler;
        config = plugin.getConfig();
        World world = Bukkit.getWorld("world");
        double x = config.getDouble("npc-location.x");
        double y = config.getDouble("npc-location.y");
        double z = config.getDouble("npc-location.z");
        npcLocation = new Location(world, x, y, z);
        deleteEntities(npcLocation, 4);
        spawnNPC(npcLocation);
    }

    private void initInventory(Player player) {
        inventory = Bukkit.createInventory(null, 54, Component.text("Quests", TextColor.color(0, 170, 255))); // Bright blue color
        ItemStack whiteGlassPane = new ItemStack(Material.WHITE_STAINED_GLASS_PANE, 1); // White
        ItemStack redGlassPane = new ItemStack(Material.RED_STAINED_GLASS_PANE, 1); // Red
        ItemStack orangeGlassPane = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE, 1); // Orange
        ItemStack greenGlassPane = new ItemStack(Material.GREEN_STAINED_GLASS_PANE, 1); // Green

        ItemMeta whiteGlassPaneMeta = whiteGlassPane.getItemMeta();
        whiteGlassPaneMeta.displayName(Component.text("All Quests"));
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

        inventory.setItem(2, whiteGlassPane);
        inventory.setItem(3, redGlassPane);
        inventory.setItem(4, orangeGlassPane);
        inventory.setItem(5, greenGlassPane);

        addQuestsToInventory("All Quests", player);

    }

    private void addQuestsToInventory(String type, Player player) {
        this.type = type;
        resetInventory();
        List<Quest> quests = databaseHandler.getAll(Quest.class);
        List<PlayerQuest> playerQuests = databaseHandler.getPlayerQuests(player.getUniqueId(), type);

        if (!type.equals("All Quests")) {
            quests = playerQuests.stream()
                    .map(PlayerQuest::getQuest)
                    .toList();
        }

        for (int i = 0; i < quests.size(); i++) {
            Quest quest = quests.get(i);
            ItemStack itemStack = createQuestItemStack(quest, type, player);
            inventory.setItem(i + 18, itemStack);
        }

        finaliseInventory();
    }

    private ItemStack createQuestItemStack(Quest quest, String type, Player player) {
        ItemStack itemStack = new ItemStack(Material.PAPER);
        ItemMeta itemMeta = itemStack.getItemMeta();

        itemMeta.displayName(Component.text(quest.getName()));

        List<Component> lore = new ArrayList<>();
        var component = getQuestStatusComponent(type, quest, player);
        lore.add(component);
        lore.add(Component.text(quest.getDescription(), TextColor.color(128, 128, 128)));
        lore.add(Component.text("Objective: " + quest.getObjective(), TextColor.color(0, 128, 128)));
        if(((TextComponent)component).content().equals("Not Started"))
            lore.add(Component.text("Click to accept", TextColor.color(0, 255, 0)));
        else if(((TextComponent)component).content().equals("In Progress"))
            lore.add(Component.text("Click to cancel", TextColor.color(255, 0, 0)));
        else if(((TextComponent)component).content().equals("Completed"))
            lore.add(Component.text("Quest Completed", TextColor.color(0, 255, 0)));
        else if(((TextComponent)component).content().equals("Canceled"))
            lore.add(Component.text("Click to try again", TextColor.color(0, 0, 255)));

        itemMeta.lore(lore);
        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    private Component getQuestStatusComponent(String type, Quest quest, Player player) {
        if (!type.equals("All Quests")) {
            return getComponentFromType(type);
        } else {
            List<PlayerQuest> allPlayerQuests = databaseHandler.getPlayerQuests(player.getUniqueId(), "");
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

    private void finaliseInventory() {
        for (int i = 0; i < 54; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
            }
        }
    }

    private void resetInventory() {
        for (int i = 18; i < 54; i++) {
            inventory.setItem(i, new ItemStack(Material.AIR));
        }
    }

    public void deleteEntities(Location location, double radius) {
        World world = location.getWorld();
        if (world == null) return;

        for (Entity entity : world.getEntities()) {
            if (entity.getLocation().distance(location) <= radius
                    && (entity.getType() == EntityType.VILLAGER || entity.getType() == EntityType.ARMOR_STAND)) {
                entity.remove();
            }
        }
    }

    public void spawnNPC(Location location) {
        if (npc != null) npc.remove();
        EntityType entityType = EntityType.valueOf(config.getString("npc-settings.type"));
        npc = (Villager) npcLocation.getWorld().spawnEntity(npcLocation, entityType);

        npc.customName(Component.text(Objects.requireNonNull(config.getString("npc-settings.custom-name"))));
        npc.setCustomNameVisible(config.getBoolean("npc-settings.custom-name-visible"));
        npc.setInvulnerable(config.getBoolean("npc-settings.invulnerable"));
        npc.setAI(config.getBoolean("npc-settings.ai-enabled"));
        double maxHealth = config.getDouble("npc-settings.max-health");
        Objects.requireNonNull(npc.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(maxHealth);
        npc.setHealth(maxHealth);
        ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);
        armorStand.setInvisible(true);
        armorStand.setGravity(false);
        armorStand.addPassenger(npc);
        int interval = config.getInt("npc-task.interval");
        int range = config.getInt("npc-task.range");

        task = new BukkitRunnable() {
            @Override
            public void run() {
                Player nearestPlayer = null;
                double closestDistance = Double.MAX_VALUE;

                for (Player player : npcLocation.getWorld().getNearbyPlayers(npcLocation, range)) {
                    double distance = npc.getLocation().distance(player.getLocation());
                    if (distance < closestDistance) {
                        nearestPlayer = player;
                        closestDistance = distance;
                    }
                }
                if (nearestPlayer != null) {
                    lookAtNearestPlayer(npc, nearestPlayer);
                }
            }
        }.runTaskTimer(plugin, 0L, interval);
    }

    public static void lookAtNearestPlayer(Entity npc, Player nearestPlayer) {
        Location npcLocation = npc.getLocation();
        Location playerLocation = nearestPlayer.getLocation();

        double dx = npcLocation.getX() - playerLocation.getX();
        double dy = npcLocation.getY() - playerLocation.getY();
        double dz = npcLocation.getZ() - playerLocation.getZ();

        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double pitch = Math.atan2(dy, horizontalDistance);

        pitch = Math.toDegrees(pitch);

        npc.setRotation(180 + nearestPlayer.getYaw(), (float) pitch);
    }

    @EventHandler
    public void onEntityDeathEvent(EntityDeathEvent e) {
        if (!e.getEntity().equals(npc)) return;
        Objects.requireNonNull(npc.getVehicle()).remove();
        task.cancel();
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent e) {
        if (Bukkit.getOnlinePlayers().toArray().length > 2) return;
        deleteEntities(npcLocation, 4);
        spawnNPC(npcLocation);
    }

    @EventHandler
    public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getRightClicked() != npc) {
            return;
        }
        var player = event.getPlayer();
        initInventory(player);
        player.openInventory(inventory);
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!Objects.equals(e.getClickedInventory(), inventory)) return;

        e.setCancelled(true);

        var player = (Player) e.getWhoClicked();
        var clickedItem = e.getCurrentItem();
        if(clickedItem==null)return;
        if(clickedItem.getType().getKey().getKey().contains("glass_pane")){
            addQuestsToInventory(((TextComponent)clickedItem.getItemMeta().displayName()).content(),player);
            return;
        }

        var displayname = (TextComponent)clickedItem.getItemMeta().displayName();
        var objective = (TextComponent) Objects.requireNonNull(clickedItem.getItemMeta().lore()).get(2);
        var moveType = (TextComponent) Objects.requireNonNull(clickedItem.getItemMeta().lore()).get(0);

        if (displayname == null) return;
        if(moveType.content().equalsIgnoreCase("NOT STARTED"))
            acceptQuest(player,displayname,objective);
        else
            cancelQuest(player,displayname,objective);
    }

    private void cancelQuest(Player player, TextComponent displayname, TextComponent objective) {
        databaseHandler.changePlayerQuestType(player.getUniqueId(),displayname.content(), "CANCELED");
        addQuestsToInventory(type,player);
        player.sendMessage("You have canceled the quest: " + displayname.content());
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.0f);
    }

    private void acceptQuest(Player player, TextComponent displayname, TextComponent objective) {
        databaseHandler.addPlayerQuest(player.getUniqueId(),displayname.content());
        addQuestsToInventory(type,player);
        player.sendMessage("You have accepted the quest: " + displayname.content());
        player.sendMessage("Your objective is to: " + objective.content());
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.0f);
    }
}
