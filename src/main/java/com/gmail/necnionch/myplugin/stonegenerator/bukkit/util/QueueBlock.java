package com.gmail.necnionch.myplugin.stonegenerator.bukkit.util;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class QueueBlock {

    private final World world;
    private final int x;
    private final int y;
    private final int z;
    private final long queuedTime;
    private final @Nullable BreakInfo breakInfo;
    private final String key;
    private int generateDelay;

    /**
     * 鉱石の生成がキューされているブロックを表します
     * @param queuedTime キューに追加されたエポック時間 (millis)
     * @param generateDelay 生成に必要な待ち時間 (seconds)
     * @param breakBlockFace 採掘時に触っていたブロックの面
     */
    public QueueBlock(World world, int x, int y, int z, long queuedTime, int generateDelay, @Nullable BreakInfo breakInfo) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.queuedTime = queuedTime;
        this.generateDelay = generateDelay;
        this.breakInfo = breakInfo;
        this.key = formatLocationKey(x, y, z, world.getName());
    }

    public World getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    /**
     * キューに追加されたエポック時間 (millis)
     */
    public long getQueuedTime() {
        return queuedTime;
    }

    /**
     * 生成に必要な待ち時間 (seconds)
     * @see #setGenerateDelay(int)
     */
    public int getGenerateDelay() {
        return generateDelay;
    }

    /**
     * @see #getGenerateDelay()
     */
    public void setGenerateDelay(int delay) {
        this.generateDelay = delay;
    }

    public @Nullable BreakInfo getBreakInfo() {
        return breakInfo;
    }

    public String getLocationKey() {
        return key;
    }


    public static QueueBlock of(Block block, long queuedTime, int generateDelay, @Nullable BreakInfo breakInfo) {
        return new QueueBlock(block.getWorld(), block.getX(), block.getY(), block.getZ(), queuedTime, generateDelay, breakInfo);
    }

    public static String formatLocationKey(int x, int y, int z, String worldName) {
        return x + "," + y + "," + z + "," + worldName;
    }

    public static String getLocationKeyOfBlock(Block block) {
        return formatLocationKey(block.getX(), block.getY(), block.getZ(), block.getWorld().getName());
    }


    public static class BreakInfo {

        private final Player player;
        private final @Nullable BlockFace face;

        public BreakInfo(Player player, @Nullable BlockFace face) {
            this.player = player;
            this.face = face;
        }

        public Player getPlayer() {
            return player;
        }

        public @Nullable BlockFace getFace() {
            return face;
        }

    }

}
