package com.schotzgoblin.listener;

import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.InventoryMapping;
import com.schotzgoblin.main.QuestManager;
import com.schotzgoblin.main.QuestSystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class QuestNpc implements Listener {

    private final QuestSystem plugin;
    private BukkitTask task;
    private final QuestManager questManager;
    private final Location npcLocation;
    private Villager npc;
    private final FileConfiguration config;
    private final DatabaseHandler databaseHandler;

    public QuestNpc(QuestSystem plugin, QuestManager questManager, DatabaseHandler databaseHandler) {
        this.plugin = plugin;
        this.questManager = questManager;
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
        questManager.setupInventory(player, "All Quests");

    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        var player = (Player) e.getWhoClicked();
        var inv = new InventoryMapping();
        if (questManager.inventories.containsKey(player.getUniqueId())) {
            inv = questManager.inventories.get(player.getUniqueId());
        }
        if (!Objects.equals(e.getClickedInventory(), inv.getInventory())) return;

        e.setCancelled(true);

        try {
            var clickedItem = e.getCurrentItem();
            if (clickedItem == null) return;
            if (clickedItem.getType().getKey().getKey().contains("glass_pane")) {
                if (e.getSlot() < 9) inv.setMenuSlot(e.getSlot());
                var content = ((TextComponent) Objects.requireNonNull(clickedItem.getItemMeta().displayName())).content();
                questManager.addQuestsToInventory(content, player, inv.getInventory());
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.0f);
                return;
            }
            var displayname = (TextComponent) clickedItem.getItemMeta().displayName();
            if (displayname == null) return;
            var quest = databaseHandler.getQuestByName(displayname.content());
            var playerQuest = databaseHandler.getPlayerQuestByQuestId(player.getUniqueId(), quest.getId());
            if (playerQuest.getId() == 0)
                questManager.acceptQuest(player, displayname, quest.getObjective().getObjective());
            else if (playerQuest.getQuestStatus().getStatus().equalsIgnoreCase("IN_PROGRESS"))
                questManager.cancelQuest(player, displayname);
            else if (playerQuest.getQuestStatus().getStatus().equalsIgnoreCase("CANCELED"))
                questManager.reactivateQuest(player, displayname);
            questManager.addQuestsToInventory(inv.getType(), player, inv.getInventory());
        } catch (Exception ignored) {
            // Exception ignored, because it is not necessary to handle it (Only happens if player spam accepts quests)
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        var player = (Player) e.getPlayer();
        if(questManager.inventories.containsKey(player.getUniqueId())&&e.getInventory().equals(questManager.inventories.get(player.getUniqueId()).getInventory()))
            questManager.inventories.remove(player.getUniqueId());
    }
}
