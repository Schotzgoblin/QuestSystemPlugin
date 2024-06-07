package com.schotzgoblin.main;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

public class InitLobby implements Listener {

    QuestSystem plugin;
    BukkitTask task;
    Location npcLocation;
    Villager npc;
    FileConfiguration config;

    public InitLobby(QuestSystem plugin) {
        this.plugin = plugin;
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
                    && (entity.getType()==EntityType.VILLAGER || entity.getType() == EntityType.ARMOR_STAND)) {
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
                if (nearestPlayer!= null) {
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

        // Convert pitch from radians to degrees
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
        if(Bukkit.getOnlinePlayers().toArray().length>2)return;
        deleteEntities(npcLocation,4);
        spawnNPC(npcLocation);
    }

    @EventHandler
    public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getRightClicked() != npc) {
            return;
        }
        var player = event.getPlayer();
        var location = new Location(event.getPlayer().getWorld(),46.5,128,25.5);
        player.teleport(location);
    }
}
