package com.schotzgoblin.utils;

import com.schotzgoblin.database.PlayerQuest;
import com.schotzgoblin.enums.ObjectiveType;
import com.schotzgoblin.main.QuestSystem;
import com.schotzgoblin.utils.edit.EditQuestsUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;

public class Utils {
    public static String getTimeStringFromSecs(int time) {
        int hours = time / 3600;
        int minutes = (time % 3600) / 60;
        int secondsRemaining = time % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, secondsRemaining);

    }

    public static int getSecondsFromTimeString(String timeString) {
        String[] timeArray = timeString.split(":");
        return Integer.parseInt(timeArray[0]) * 3600 + Integer.parseInt(timeArray[1]) * 60 + Integer.parseInt(timeArray[2]);
    }


    public static float calculateProgress(PlayerQuest playerQuest) {
        var objective = playerQuest.getQuest().getObjective();
        switch (ObjectiveType.valueOf(objective.getType())) {
            case ObjectiveType.KILL:
            case ObjectiveType.PICKUP:
                return Math.clamp(Float.parseFloat(playerQuest.getProgress()) / objective.getCount(), 0, 1);
            case ObjectiveType.MOVE:
                if (playerQuest.getProgress().equals("0")) return 0;
                var startLocation = convertStringToLocation(playerQuest.getStartLocation());
                var location = convertStringToLocation(playerQuest.getProgress());
                var endLocation = convertStringToLocation(objective.getValue());
                double radius = objective.getCount();
                if (!startLocation.getWorld().getName().equals(endLocation.getWorld().getName()) || !endLocation.getWorld().getName().equals(location.getWorld().getName())) {
                    return 0;
                }
                double distanceToEnd = startLocation.distance(endLocation);
                double playerDistanceToEnd = location.distance(endLocation);
                if (playerDistanceToEnd < radius) {
                    return 1;
                }
                return Float.parseFloat(Math.clamp(1 - (playerDistanceToEnd / distanceToEnd), 0, 1) + "");
            default:
                return 0;
        }
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
                Double.parseDouble(locationArray[1].replace(",", ".")),
                Double.parseDouble(locationArray[2].replace(",", ".")),
                Double.parseDouble(locationArray[3].replace(",", ".")));
    }

    public static void navigateToQuestsInventory(Player player) {
        EditQuestsUtils.refreshEditInventory(EditQuestsUtils.editingQuest.get(player.getUniqueId()), player).thenAccept(x -> {
            Bukkit.getScheduler().runTask(QuestSystem.getPlugin(QuestSystem.class), () -> {
                player.closeInventory();
                player.openInventory(EditQuestsUtils.editQuestInventory.get(player.getUniqueId()));
            });
        });

    }
}
