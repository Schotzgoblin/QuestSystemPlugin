package com.schotzgoblin.listener;

import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.QuestSystem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import static com.schotzgoblin.utils.ParticleUtils.createParticle;


public class ParticalListener implements Listener {
    private final QuestSystem plugin = QuestSystem.getInstance();

    public ParticalListener() {
        this.updateNearbySignsOfOnlinePlayersDelayed();
    }

    @EventHandler
    void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        updateNearbySignsDelayed(player);
    }

    private void updateNearbySignsOfOnlinePlayersDelayed() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                createParticle(player);
            }
        }, 2,20);
    }

    void updateNearbySignsDelayed(Player player) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (player.isOnline()) {
                createParticle(player);
            }
        }, 2,20);
    }
}
