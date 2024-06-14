package com.schotzgoblin.listener;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.dtos.InventoryMapping;
import com.schotzgoblin.main.QuestManager;
import com.schotzgoblin.main.QuestSystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class QuestNpc implements Listener {

    private final QuestSystem plugin;
    private BukkitTask task;
    private final QuestManager questManager;
    private Location npcLocation;
    private Villager npc;
    private final DatabaseHandler databaseHandler;
    private final ConfigHandler configHandler = ConfigHandler.getInstance();

    public QuestNpc() {
        this.plugin = QuestSystem.getInstance();
        this.questManager = QuestManager.getInstance();
        this.databaseHandler = DatabaseHandler.getInstance();

        // Fetch NPC location asynchronously
        fetchNPCLocationAsync().thenAccept(location -> {
            this.npcLocation = location;
            deleteEntities(npcLocation, 4);
            spawnNPC(npcLocation);
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    private CompletableFuture<Location> fetchNPCLocationAsync() {

        CompletableFuture<Double> xFuture = configHandler.getDoubleAsync("npc-location.x");
        CompletableFuture<Double> yFuture = configHandler.getDoubleAsync("npc-location.y");
        CompletableFuture<Double> zFuture = configHandler.getDoubleAsync("npc-location.z");

        return CompletableFuture.allOf(xFuture, yFuture, zFuture).thenApply(v -> {
            try {
                double x = xFuture.get();
                double y = yFuture.get();
                double z = zFuture.get();
                World world = Bukkit.getWorld("world");
                return new Location(world, x, y, z);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
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

        fetchConfigValuesAsync().thenAccept(configValues -> {
            try {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    EntityType entityType = EntityType.valueOf(configValues.entityType);
                    npc = (Villager) npcLocation.getWorld().spawnEntity(npcLocation, entityType);

                    npc.customName(Component.text(Objects.requireNonNull(configValues.customName)));
                    npc.setCustomNameVisible(configValues.customNameVisible);
                    npc.setInvulnerable(configValues.invulnerable);
                    npc.setAI(configValues.aiEnabled);
                    double maxHealth = configValues.maxHealth;
                    Objects.requireNonNull(npc.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(maxHealth);
                    npc.setHealth(maxHealth);

                    ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);
                    armorStand.setInvisible(true);
                    armorStand.setGravity(false);
                    armorStand.addPassenger(npc);
                });
                startNPCTask(configValues.interval, configValues.range);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    private CompletableFuture<ConfigValues> fetchConfigValuesAsync() {

        CompletableFuture<String> entityTypeFuture = configHandler.getStringAsync("npc-settings.type");
        CompletableFuture<String> customNameFuture = configHandler.getStringAsync("npc-settings.custom-name");
        CompletableFuture<Boolean> customNameVisibleFuture = configHandler.getBooleanAsync("npc-settings.custom-name-visible");
        CompletableFuture<Boolean> invulnerableFuture = configHandler.getBooleanAsync("npc-settings.invulnerable");
        CompletableFuture<Boolean> aiEnabledFuture = configHandler.getBooleanAsync("npc-settings.ai-enabled");
        CompletableFuture<Double> maxHealthFuture = configHandler.getDoubleAsync("npc-settings.max-health");
        CompletableFuture<Integer> intervalFuture = configHandler.getIntAsync("npc-task.interval");
        CompletableFuture<Integer> rangeFuture = configHandler.getIntAsync("npc-task.range");

        return CompletableFuture.allOf(
                entityTypeFuture,
                customNameFuture,
                customNameVisibleFuture,
                invulnerableFuture,
                aiEnabledFuture,
                maxHealthFuture,
                intervalFuture,
                rangeFuture
        ).thenApply(v -> {
            try {
                return new ConfigValues(
                        entityTypeFuture.get(),
                        customNameFuture.get(),
                        customNameVisibleFuture.get(),
                        invulnerableFuture.get(),
                        aiEnabledFuture.get(),
                        maxHealthFuture.get(),
                        intervalFuture.get(),
                        rangeFuture.get()
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    private void startNPCTask(int interval, int range) {
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

    private static class ConfigValues {
        String entityType;
        String customName;
        boolean customNameVisible;
        boolean invulnerable;
        boolean aiEnabled;
        double maxHealth;
        int interval;
        int range;

        ConfigValues(String entityType, String customName, boolean customNameVisible, boolean invulnerable,
                     boolean aiEnabled, double maxHealth, int interval, int range) {
            this.entityType = entityType;
            this.customName = customName;
            this.customNameVisible = customNameVisible;
            this.invulnerable = invulnerable;
            this.aiEnabled = aiEnabled;
            this.maxHealth = maxHealth;
            this.interval = interval;
            this.range = range;
        }
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
        Player player = (Player) e.getWhoClicked();
        UUID playerId = player.getUniqueId();
        InventoryMapping inv = questManager.inventories.getOrDefault(playerId, new InventoryMapping());

        if (!Objects.equals(e.getClickedInventory(), inv.getInventory())) {
            return;
        }

        e.setCancelled(true);

        try {
            ItemStack clickedItem = e.getCurrentItem();
            if (clickedItem == null) {
                return;
            }

            ItemMeta itemMeta = clickedItem.getItemMeta();
            if (itemMeta == null || !(itemMeta.displayName() instanceof TextComponent displayName)) {
                return;
            }

            if (clickedItem.getType().getKey().getKey().contains("glass_pane")) {
                handleGlassPaneClick(e.getSlot(), player, inv, displayName.content());
            } else {
                handleQuestItemClick(player, displayName, inv);
            }

        } catch (Exception ignored) {
            // Exception ignored, because it is not necessary to handle it (Only happens if player spam accepts quests)
        }
    }

    private void handleGlassPaneClick(int slot, Player player, InventoryMapping inv, String displayName) {
        if (slot < 9) {
            inv.setMenuSlot(slot);
        }

        questManager.addQuestsToInventory(displayName, player, inv.getInventory());
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.0f);
    }

    private void handleQuestItemClick(Player player, TextComponent displayName, InventoryMapping inv) {
        var questFuture = databaseHandler.getQuestByNameAsync(displayName.content());
        questFuture.thenAccept(quest -> {
            var playerQuestFuture = databaseHandler.getPlayerQuestByQuestIdAsync(player.getUniqueId(), quest.getId());
            CompletableFuture.allOf(playerQuestFuture).thenAccept(x -> {
                try {
                    var playerQuest = playerQuestFuture.get();
                    if (playerQuest.getId() == 0) {
                        questManager.acceptQuest(player, displayName, quest.getObjective().getObjective());

                    } else {
                        switch (playerQuest.getQuestStatus().getStatus().toUpperCase()) {
                            case "IN_PROGRESS":
                                questManager.cancelQuest(player, displayName).join();
                                break;
                            case "CANCELED":
                                questManager.reactivateQuest(player, displayName).join();
                                break;
                            default:
                                break;
                        }
                    }
                    questManager.addQuestsToInventory(inv.getType(), player, inv.getInventory());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).exceptionally(ex -> {
                ex.printStackTrace();
                return null;
            });
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }


    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        var player = (Player) e.getPlayer();
        if (questManager.inventories.containsKey(player.getUniqueId()) && e.getInventory().equals(questManager.inventories.get(player.getUniqueId()).getInventory()))
            questManager.inventories.remove(player.getUniqueId());
    }
}
