package com.schotzgoblin.listener;

import com.google.common.base.Preconditions;
import com.schotzgoblin.database.PlayerQuest;
import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.QuestSystem;
import com.schotzgoblin.utils.SignUtils;
import com.schotzgoblin.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;
import java.util.Random;


public class SignListener implements Listener {
    private QuestSystem plugin;
    private DatabaseHandler databaseHandler;

    public SignListener(QuestSystem plugin, DatabaseHandler databaseHandler) {
        this.plugin = plugin;
        this.databaseHandler = databaseHandler;
        this.updateNearbySignsOfOnlinePlayersDelayed();
    }

    @EventHandler
    void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        int updateDelay = getPlayerJoinSignUpdateDelay();
        updateNearbySignsDelayed(player, updateDelay);
    }
    @EventHandler(
            ignoreCancelled = true
    )
    void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.HAND) {// 34
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {// 36
                Block block = event.getClickedBlock();// 37
                Material blockType = block.getType();// 38
                if (SignUtils.isSign(blockType)) {// 39
                    Player player = event.getPlayer();// 40
                    Sign sign = (Sign)block.getState();// 41
                    sendSignUpdate(player, sign);// 42
                }
            }

        }
    }

    public void sendSignUpdate(Player player, Sign sign) {
        Preconditions.checkNotNull(player, "player is null");
        Preconditions.checkNotNull(sign, "sign is null");
        var playerQuests = databaseHandler.getPlayerQuests(player.getUniqueId(), "IN_PROGRESS");
        var side = sign.getSide(Side.FRONT);
        var firstLine = side.line(0);
        changeSign(side, (TextComponent) firstLine, playerQuests);
        player.sendSignChange(sign.getLocation(), sign.getSide(Side.FRONT).lines(), SignUtils.getSignTextColor(sign), sign.getSide(Side.FRONT).isGlowingText());
    }

    private void changeSign(SignSide side, TextComponent firstLine, List<PlayerQuest> playerQuests) {
        String content = firstLine.content();
        if (content.toLowerCase().contains("quests")) {
            side.line(0, Component.text("Quests: " + playerQuests.size()));
            side.line(3, Component.text("Right click to update"));
        } else if (content.toLowerCase().contains("quest")) {
            if (playerQuests.isEmpty()) {
                side.line(0, Component.text("No quests started"));
                return;
            }
            var playerQuest = playerQuests.get(new Random().nextInt(playerQuests.size()));
            side.line(0, Component.text("Quest: " + playerQuest.getQuest().getName()));
            side.line(1, Component.text("Progress: " + Math.round(Utils.calculateProgress(playerQuest) * 100) + "%"));
            side.line(3, Component.text("Right click to update"));
        }
    }

    private void updateNearbySignsOfOnlinePlayersDelayed() {
        int delayTicks = getPlayerJoinSignUpdateDelay();
        if (delayTicks > 0) {
            Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    this.updateNearbySigns(player);
                }
            }, delayTicks, 20);
        }
    }

    private int getPlayerJoinSignUpdateDelay() {
        return 10;
    }

    void updateNearbySigns(Player player) {
        assert player != null && player.isOnline();
        List<Sign> nearbySigns = SignUtils.getNearbyTileEntities(player.getLocation(), Bukkit.getViewDistance(), Sign.class);
        for (Sign sign : nearbySigns) {
            sendSignUpdate(player, sign);
        }

    }

    void updateNearbySignsDelayed(Player player, int delayTicks) {
        if (delayTicks > 0) {
            Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (player.isOnline()) {
                    this.updateNearbySigns(player);
                }
            }, delayTicks,20);
        }
    }
}
