package com.schotzgoblin.utils;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.enums.ObjectiveType;
import com.schotzgoblin.main.DatabaseHandler;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;



public class ParticleUtils {
    private static final ConfigHandler configHandler = ConfigHandler.getInstance();

    public static void createParticle(Player player) {
        var playerQuests = PlayerMoveUtils.playerQuestConfig.get(player.getUniqueId());
        if(playerQuests==null) return;
        if (!playerQuests.isEmpty()) {
            playerQuests.forEach(playerQuest -> {
                if (ObjectiveType.valueOf(playerQuest.getQuest().getObjective().getType()) == ObjectiveType.MOVE){
                    var location = Utils.convertStringToLocation(playerQuest.getQuest().getObjective().getValue());
                    spawnParticle(player, location).join();
                    spawnDirectionParticle(player, location).join();
                }
            });
        }
    }

    public static CompletableFuture<Void> spawnDirectionParticle(Player player, Location location) {
        return getParticleSettings("particle-settings.direction").thenAccept(settings -> {
            Location playerLocation = player.getLocation();
            Vector direction = location.toVector().subtract(playerLocation.toVector()).normalize();

            double spacing = Double.parseDouble(settings.get("spacing"));
            double distance = Double.parseDouble(settings.get("distance"));
            Particle particle = Particle.valueOf(settings.get("particle"));
            int count = Integer.parseInt(settings.get("count"));
            double offsetX = Double.parseDouble(settings.get("offset-x"));
            double offsetY = Double.parseDouble(settings.get("offset-y"));
            double offsetZ = Double.parseDouble(settings.get("offset-z"));
            double extra = Double.parseDouble(settings.get("extra"));

            for (double d = 0; d < distance; d += spacing) {
                Location particleLocation = playerLocation.clone().add(direction.clone().multiply(d));
                player.spawnParticle(particle, particleLocation, count, offsetX, offsetY, offsetZ, extra);
            }
        });
    }

    public static CompletableFuture<Void> spawnParticle(Player player, Location location) {
        return getParticleSettings("particle-settings.spawn").thenAccept(settings -> {
            Particle particle = Particle.valueOf(settings.get("particle"));
            int iterations = Integer.parseInt(settings.get("iterations"));
            int count = Integer.parseInt(settings.get("count"));
            double offsetX = Double.parseDouble(settings.get("offset-x"));
            double offsetY = Double.parseDouble(settings.get("offset-y"));
            double offsetZ = Double.parseDouble(settings.get("offset-z"));
            double extra = Double.parseDouble(settings.get("extra"));

            for (int i = 0; i < iterations; i++) {
                location.add(0, i, 0);
                player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
                location.subtract(0, i, 0);
            }
        });
    }

    private static CompletableFuture<Map<String, String>> getParticleSettings(String path) {
        CompletableFuture<String> particleFuture = configHandler.getStringAsync(path + ".particle");
        CompletableFuture<String> spacingFuture = configHandler.getStringAsync(path + ".spacing");
        CompletableFuture<String> distanceFuture = configHandler.getStringAsync(path + ".distance");
        CompletableFuture<String> countFuture = configHandler.getStringAsync(path + ".count");
        CompletableFuture<String> offsetXFuture = configHandler.getStringAsync(path + ".offset-x");
        CompletableFuture<String> offsetYFuture = configHandler.getStringAsync(path + ".offset-y");
        CompletableFuture<String> offsetZFuture = configHandler.getStringAsync(path + ".offset-z");
        CompletableFuture<String> extraFuture = configHandler.getStringAsync(path + ".extra");
        CompletableFuture<String> iterationsFuture = configHandler.getStringAsync(path + ".iterations");

        return CompletableFuture.allOf(particleFuture, spacingFuture, distanceFuture, countFuture, offsetXFuture, offsetYFuture, offsetZFuture, extraFuture, iterationsFuture)
                .thenApply(v -> {
                    Map<String, String> settings = new HashMap<>();
                    settings.put("particle", particleFuture.join());
                    settings.put("spacing", spacingFuture.join());
                    settings.put("distance", distanceFuture.join());
                    settings.put("count", countFuture.join());
                    settings.put("offset-x", offsetXFuture.join());
                    settings.put("offset-y", offsetYFuture.join());
                    settings.put("offset-z", offsetZFuture.join());
                    settings.put("extra", extraFuture.join());
                    if (iterationsFuture.isDone()) {
                        settings.put("iterations", iterationsFuture.join());
                    }
                    return settings;
                });
    }
}
