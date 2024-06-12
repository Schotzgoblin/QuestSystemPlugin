package com.schotzgoblin.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;

import java.time.Duration;

public class MessageUtils {
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
