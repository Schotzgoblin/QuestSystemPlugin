package com.schotzgoblin.main;

import com.schotzgoblin.database.Objective;
import com.schotzgoblin.database.PlayerQuest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.Duration;

public class Utils {
    public static String getTimeStringFromSecs(int time) {
        int hours = time / 3600;
        int minutes = (time % 3600) / 60;
        int secondsRemaining = time % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, secondsRemaining);

    }

    public static float calculateProgress(PlayerQuest playerQuest) {
        var objective = playerQuest.getQuest().getObjective();
        switch (objective.getType()) {
            case "KILL":
            case "PICKUP":
                return Math.clamp(Float.parseFloat(playerQuest.getProgress()) / objective.getCount(),0,1);
            case "MOVE":
                var startLocation = convertStringToLocation(playerQuest.getStartLocation());
                var location = convertStringToLocation(playerQuest.getProgress());
                var endLocation = convertStringToLocation(objective.getValue());
                double radius = objective.getCount();
                if(!startLocation.getWorld().getName().equals(endLocation.getWorld().getName())||!endLocation.getWorld().getName().equals(location.getWorld().getName())){
                    return 0;
                }
                double distanceToEnd = startLocation.distance(endLocation);
                double playerDistanceToEnd = location.distance(endLocation);
                if(playerDistanceToEnd<radius){
                    return 1;
                }
                return Float.parseFloat(Math.clamp(1 - (playerDistanceToEnd / distanceToEnd), 0, 1)+"");
            default:
                return 0;
        }
    }

    public static void sendAlertToAllPlayers(String title, String subtitle, int fadeIn, int stay, int fadeOut, Color color) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(
                    Title.title(Component.text(title, TextColor.color(color.getRed(), color.getGreen(), color.getBlue())),
                            Component.text(subtitle, TextColor.color(color.getRed(), color.getGreen(), color.getBlue())),
                            Title.Times.times(Duration.ofMillis(fadeIn), Duration.ofMillis(stay), Duration.ofMillis(fadeOut))));
        }
    }

    public static void sendAlertToPlayer(String title, String subtitle, int fadeIn, int stay, int fadeOut, Color color, Player player) {
        player.showTitle(
                Title.title(Component.text(title, TextColor.color(color.getRed(), color.getGreen(), color.getBlue())),
                        Component.text(subtitle, TextColor.color(color.getRed(), color.getGreen(), color.getBlue())),
                        Title.Times.times(Duration.ofMillis(fadeIn), Duration.ofMillis(stay), Duration.ofMillis(fadeOut))));
    }

    public static String convertLocationToString(Location location) {
        return location.getWorld().getName() + ";" +
                String.format("%.2f", location.getX()) + ";" +
                String.format("%.2f", location.getY()) + ";" +
                String.format("%.2f", location.getZ());
    }

    public static Location convertStringToLocation(String locationString) {
        String[] locationArray = locationString.split(";");
        return new Location(Bukkit.getWorld(locationArray[0]),
                Double.parseDouble(locationArray[1].replace(",",".")),
                Double.parseDouble(locationArray[2].replace(",",".")),
                Double.parseDouble(locationArray[3].replace(",",".")));
    }
}
