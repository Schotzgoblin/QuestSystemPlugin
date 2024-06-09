package com.schotzgoblin.main;

import com.schotzgoblin.database.Objective;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;

import java.time.Duration;

public class Utils {
    public static String getTimeStringFromSecs(int time) {
        int hours = time / 3600;
        int minutes = (time % 3600) / 60;
        int secondsRemaining = time % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, secondsRemaining);

    }

    public static float calculateProgress(Objective objective, String progress) {
        switch (objective.getType()) {
            case "KILL":
            case "PICKUP":
                return Math.clamp(Float.parseFloat(progress) / objective.getCount(),0,1);
            case "MOVE":
                // TODO
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
}
