package com.schotzgoblin.utils;

import com.schotzgoblin.main.DatabaseHandler;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;


public class ParticleUtils {
    private static final DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
    public static void createParticle(Player player) {
        var playerQuestsFuture = databaseHandler.getPlayerQuestsAsync(player.getUniqueId(), "IN_PROGRESS");
        playerQuestsFuture.thenAccept(playerQuests -> {
            if (!playerQuests.isEmpty()) {
                playerQuests.forEach(playerQuest -> {
                    if(playerQuest.getQuest().getObjective().getType().equals("MOVE")){
                        var location = Utils.convertStringToLocation(playerQuest.getQuest().getObjective().getValue());
                        spawnParticle(player, location);
                        spawnDirectionParticle(player, location);
                    }
                });
            }
        });
    }

    private static void spawnDirectionParticle(Player player, Location location) {
        Location playerLocation = player.getLocation();
        Vector direction = location.toVector().subtract(playerLocation.toVector()).normalize();

        double spacing = 0.5; // Distance between each particle
        double distance = 8;

        for (double d = 0; d < distance; d += spacing) {
            Location particleLocation = playerLocation.clone().add(direction.clone().multiply(d));
            player.spawnParticle(Particle.PORTAL, particleLocation, 10, 0, 0, 0, 0.1);
        }
    }

    private static void spawnParticle(Player player, Location location) {
        for (int i = 0; i < 10; i++) {
            location.add(0,i,0);
            player.spawnParticle(Particle.FLAME, location, 15, 0.1, 0.5, 0.1, 0.02);
            location.subtract(0,i,0);
        }
    }
}
