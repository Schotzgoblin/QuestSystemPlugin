package com.schotzgoblin.utils;

import com.google.common.base.Preconditions;
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
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SignUtils {
    public static boolean isSign(Material material) {
        Preconditions.checkNotNull(material, "material is null");// 36
        return material.data == Sign.class || material.data == WallSign.class || material.data == HangingSign.class || material.data == WallHangingSign.class;// 37
    }
    public static DyeColor getSignTextColor(org.bukkit.block.Sign sign) {
        Preconditions.checkNotNull(sign, "sign is null");// 50
        DyeColor color = sign.getSide(Side.FRONT).getColor();// 51
        return color != null ? color : DyeColor.BLACK;// 52
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

        for(int chunkX = startX; chunkX <= endX; ++chunkX) {// 98
            for(int chunkZ = startZ; chunkZ <= endZ; ++chunkZ) {// 99
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
