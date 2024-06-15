package com.schotzgoblin.utils;

import com.google.common.base.Preconditions;
import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.database.PlayerQuest;
import com.schotzgoblin.main.DatabaseHandler;
import com.schotzgoblin.main.QuestSystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
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
import java.util.concurrent.CompletableFuture;

public class SignUtils {
    private static final QuestSystem plugin = QuestSystem.getInstance();
    private static final ConfigHandler configHandler = ConfigHandler.getInstance();
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
        List<Sign> nearbySigns = SignUtils.getNearbyTileEntities(player.getLocation(), Bukkit.getViewDistance(), Sign.class);
        for (Sign sign : nearbySigns) {
            sendSignUpdate(player, sign);
        }
    }

    public static void sendSignUpdate(Player player, Sign sign) {
        Preconditions.checkNotNull(player, "player is null");
        Preconditions.checkNotNull(sign, "sign is null");
        var playerQuests = PlayerMoveUtils.playerQuestConfig.get(player.getUniqueId());
        if (playerQuests == null) return;
        var side = sign.getSide(Side.FRONT);
        var firstLine = side.line(0);
        if (!playerSignChange.containsKey(player.getUniqueId())) {
            playerSignChange.put(player.getUniqueId(), 0);
        }
        var index = playerSignChange.getOrDefault(player.getUniqueId(), 0);
        if (index >= playerQuests.size()) {
            index = 0;
            playerSignChange.put(player.getUniqueId(), index);
        }
        changeSign(side, (TextComponent) firstLine, playerQuests, index).thenAccept(x -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendSignChange(sign.getLocation(),
                        sign.getSide(Side.FRONT).lines(), SignUtils.getSignTextColor(sign), sign.getSide(Side.FRONT).isGlowingText());
            });
        });
    }

    private static CompletableFuture<Void> changeSign(SignSide side, TextComponent firstLine, List<PlayerQuest> playerQuests, int index) {
        String content = firstLine.content().toLowerCase();
        CompletableFuture<String> questsTitle = configHandler.getStringAsync("sign-messages.quests-title");
        CompletableFuture<String> questTitle = configHandler.getStringAsync("sign-messages.quest-title");

        return CompletableFuture.allOf(questsTitle, questTitle).thenCompose(aVoid -> {
            if (content.contains(questsTitle.join())) {
                return handleQuestsSign(side, playerQuests);
            } else if (content.contains(questTitle.join())) {
                return handleQuestSign(side, playerQuests, index);
            }
            return CompletableFuture.completedFuture(null);
        });
    }

    private static CompletableFuture<Void> handleQuestsSign(SignSide side, List<PlayerQuest> playerQuests) {
        CompletableFuture<String> line0Future = configHandler.getStringAsync("sign-messages.quests_sign.line_0.text");
        CompletableFuture<String> line3Future = configHandler.getStringAsync("sign-messages.quests_sign.line_3.text");
        CompletableFuture<String> line0ColorFuture = configHandler.getStringAsync("sign-messages.quests_sign.line_0.color");
        CompletableFuture<String> line3ColorFuture = configHandler.getStringAsync("sign-messages.quests_sign.line_3.color");

        return CompletableFuture.allOf(line0Future, line3Future, line0ColorFuture, line3ColorFuture).thenRun(() -> {
            side.line(0, Component.text(line0Future.join().replace("%size%", String.valueOf(playerQuests.size()))).color(TextColor.fromHexString(line0ColorFuture.join())));
            side.line(3, Component.text(line3Future.join()).color(TextColor.fromHexString(line3ColorFuture.join())));
        });
    }

    private static CompletableFuture<Void> handleQuestSign(SignSide side, List<PlayerQuest> playerQuests, int index) {
        if (playerQuests.isEmpty()) {
            return handleNoQuestsSign(side);
        } else {
            return handleActiveQuestSign(side, playerQuests, index);
        }
    }

    private static CompletableFuture<Void> handleNoQuestsSign(SignSide side) {
        CompletableFuture<String> line0Future = configHandler.getStringAsync("sign-messages.quest_sign.no_quests.line_0.text");
        CompletableFuture<String> line1Future = configHandler.getStringAsync("sign-messages.quest_sign.no_quests.line_1.text");
        CompletableFuture<String> line2Future = configHandler.getStringAsync("sign-messages.quest_sign.no_quests.line_2.text");
        CompletableFuture<String> line3Future = configHandler.getStringAsync("sign-messages.quest_sign.no_quests.line_3.text");

        CompletableFuture<String> line0ColorFuture = configHandler.getStringAsync("sign-messages.quest_sign.no_quests.line_0.color");
        CompletableFuture<String> line1ColorFuture = configHandler.getStringAsync("sign-messages.quest_sign.no_quests.line_1.color");
        CompletableFuture<String> line2ColorFuture = configHandler.getStringAsync("sign-messages.quest_sign.no_quests.line_2.color");
        CompletableFuture<String> line3ColorFuture = configHandler.getStringAsync("sign-messages.quest_sign.no_quests.line_3.color");

        return CompletableFuture.allOf(line0Future, line1Future, line2Future, line3Future, line0ColorFuture, line1ColorFuture, line2ColorFuture, line3ColorFuture).thenRun(() -> {
            side.line(0, Component.text(line0Future.join()).color(TextColor.fromHexString(line0ColorFuture.join())));
            side.line(1, Component.text(line1Future.join()).color(TextColor.fromHexString(line1ColorFuture.join())));
            side.line(2, Component.text(line2Future.join()).color(TextColor.fromHexString(line2ColorFuture.join())));
            side.line(3, Component.text(line3Future.join()).color(TextColor.fromHexString(line3ColorFuture.join())));
        });
    }

    private static CompletableFuture<Void> handleActiveQuestSign(SignSide side, List<PlayerQuest> playerQuests, int index) {
        var playerQuest = playerQuests.get(index);
        CompletableFuture<String> line0Future = configHandler.getStringAsync("sign-messages.quest_sign.quest.line_0.text");
        CompletableFuture<String> line1Future = configHandler.getStringAsync("sign-messages.quest_sign.quest.line_1.text");
        CompletableFuture<String> line2Future = configHandler.getStringAsync("sign-messages.quest_sign.quest.line_2.text");
        CompletableFuture<String> line3Future = playerQuests.size() > 1
                ? configHandler.getStringAsync("sign-messages.quest_sign.quest.line_3_multiple.text")
                : configHandler.getStringAsync("sign-messages.quest_sign.quest.line_3_single.text");

        CompletableFuture<String> line0ColorFuture = configHandler.getStringAsync("sign-messages.quest_sign.quest.line_0.color");
        CompletableFuture<String> line1ColorFuture = configHandler.getStringAsync("sign-messages.quest_sign.quest.line_1.color");
        CompletableFuture<String> line2ColorFuture = configHandler.getStringAsync("sign-messages.quest_sign.quest.line_2.color");
        CompletableFuture<String> line3ColorFuture = playerQuests.size() > 1
                ? configHandler.getStringAsync("sign-messages.quest_sign.quest.line_3_multiple.color")
                : configHandler.getStringAsync("sign-messages.quest_sign.quest.line_3_single.color");

        return CompletableFuture.allOf(line0Future, line1Future, line2Future, line3Future, line0ColorFuture, line1ColorFuture, line2ColorFuture, line3ColorFuture).thenRun(() -> {
            side.line(0, Component.text(line0Future.join().replace("%name%", playerQuest.getQuest().getName())).color(TextColor.fromHexString(line0ColorFuture.join())));
            side.line(1, Component.text(line1Future.join().replace("%progress%", String.valueOf(Math.round(Utils.calculateProgress(playerQuest) * 100)))).color(TextColor.fromHexString(line1ColorFuture.join())));
            side.line(2, Component.text(line2Future.join().replace("%time_left%", Utils.getTimeStringFromSecs(playerQuest.getQuest().getTimeLimit() - playerQuest.getTime()))).color(TextColor.fromHexString(line2ColorFuture.join())));
            side.line(3, Component.text(line3Future.join()).color(TextColor.fromHexString(line3ColorFuture.join())));
        });
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
