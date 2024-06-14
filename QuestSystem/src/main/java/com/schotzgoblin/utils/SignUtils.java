package com.schotzgoblin.utils;

import com.google.common.base.Preconditions;
import com.schotzgoblin.database.PlayerQuest;
import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.QuestSystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.HangingSign;
import org.bukkit.block.Sign;
import org.bukkit.block.TileState;
import org.bukkit.block.data.type.WallHangingSign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;

import java.util.*;

public class SignUtils {
    private static final QuestSystem plugin = QuestSystem.getInstance();
    private static final DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
    public static final Map<UUID, Integer> playerSignChange = new HashMap<>();

    public static boolean isSign(Material material) {
        Preconditions.checkNotNull(material, "material is null");// 36
        return material.data == Sign.class || material.data == WallSign.class || material.data == HangingSign.class || material.data == WallHangingSign.class;// 37
    }

    public static DyeColor getSignTextColor(org.bukkit.block.Sign sign) {
        Preconditions.checkNotNull(sign, "sign is null");// 50
        DyeColor color = sign.getSide(Side.FRONT).getColor();// 51
        return color != null ? color : DyeColor.BLACK;// 52
    }

    public static void updateNearbySigns(Player player) {
        assert player != null && player.isOnline();
        Bukkit.getScheduler().runTask(plugin, () -> {
            List<Sign> nearbySigns = SignUtils.getNearbyTileEntities(player.getLocation(), Bukkit.getViewDistance(), Sign.class);
            for (Sign sign : nearbySigns) {
                sendSignUpdate(player, sign);
            }
        });
    }

    public static void sendSignUpdate(Player player, Sign sign) {
        Preconditions.checkNotNull(player, "player is null");
        Preconditions.checkNotNull(sign, "sign is null");
        var playerQuestsFuture = databaseHandler.getPlayerQuestsAsync(player.getUniqueId(), "IN_PROGRESS");
        playerQuestsFuture.thenAccept(playerQuests -> {
            var side = sign.getSide(Side.FRONT);
            var firstLine = side.line(0);
            if (!playerSignChange.containsKey(player.getUniqueId())) {
                playerSignChange.put(player.getUniqueId(), 0);
            }
            var index = playerSignChange.getOrDefault(player.getUniqueId(), 0);
            if (index == playerQuests.size()) {
                index = 0;
                playerSignChange.put(player.getUniqueId(), index);
            }
            changeSign(side, (TextComponent) firstLine, playerQuests, index);
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendSignChange(sign.getLocation(),
                        sign.getSide(Side.FRONT).lines(), SignUtils.getSignTextColor(sign), sign.getSide(Side.FRONT).isGlowingText());
            });
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });

    }

    private static void changeSign(SignSide side, TextComponent firstLine, List<PlayerQuest> playerQuests, int index) {
        String content = firstLine.content();
        if (content.toLowerCase().contains("quests")) {
            side.line(0, Component.text("Quests: " + playerQuests.size()));
            side.line(3, Component.text(""));
        } else if (content.toLowerCase().contains("quest")) {
            if (playerQuests.isEmpty()) {
                side.line(0, Component.text("No quests started"));
                side.line(1, Component.text(""));
                side.line(2, Component.text(""));
                side.line(3, Component.text(""));
                return;
            }
            var playerQuest = playerQuests.get(index);
            side.line(0, Component.text("Quest: " + playerQuest.getQuest().getName()));
            side.line(1, Component.text("Progress: " + Math.round(Utils.calculateProgress(playerQuest) * 100) + "%"));
            side.line(2, Component.text("Time left: " + Utils.getTimeStringFromSecs(playerQuest.getQuest().getTimeLimit()-playerQuest.getTime())));
            if (playerQuests.size() > 1) side.line(3, Component.text("Right click to change"));
            else side.line(3, Component.text(""));
        }
    }

    public static <T extends BlockState> List<T> getNearbyTileEntities(Location location, int chunkRadius, Class<T> type) {
        //From https://dev.bukkit.org/projects/individual-signs
        Preconditions.checkNotNull(location, "location is null");// 86
        World world = location.getWorld();// 87
        Preconditions.checkNotNull(world, "The location's world is null!");// 88
        Preconditions.checkNotNull(type, "type is null");// 89
        Preconditions.checkArgument(chunkRadius >= 0, "chunkRadius cannot be negative");// 90
        List<T> tileEntities = new ArrayList<>();// 92
        Chunk center = location.getChunk();// 93
        int startX = center.getX() - chunkRadius;// 94
        int endX = center.getX() + chunkRadius;// 95
        int startZ = center.getZ() - chunkRadius;// 96
        int endZ = center.getZ() + chunkRadius;// 97

        for (int chunkX = startX; chunkX <= endX; ++chunkX) {// 98
            for (int chunkZ = startZ; chunkZ <= endZ; ++chunkZ) {// 99
                if (world.isChunkLoaded(chunkX, chunkZ)) {// 100
                    Chunk chunk = world.getChunkAt(chunkX, chunkZ);// 102
                    BlockState[] var13 = chunk.getTileEntities();
                    for (BlockState tileEntity : var13) {// 103
                        if (type.isInstance(tileEntity)) {// 104 105
                            tileEntities.add(type.cast(tileEntity));// 106
                        }
                    }
                }
            }
        }

        return tileEntities;// 111
    }
}
