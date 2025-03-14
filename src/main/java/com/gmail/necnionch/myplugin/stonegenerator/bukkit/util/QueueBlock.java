package com.gmail.necnionch.myplugin.stonegenerator.bukkit.util;

import org.bukkit.World;
import org.bukkit.block.Block;

public class QueueBlock {

    private final World world;
    private final int x;
    private final int y;
    private final int z;
    private final long queuedTime;
    private final String key;
    private int generateDelay;

    /**
     * 鉱石の生成がキューされているブロックを表します
     * @param queuedTime キューに追加されたエポック時間 (millis)
     * @param generateDelay 生成に必要な待ち時間 (seconds)
     */
    public QueueBlock(World world, int x, int y, int z, long queuedTime, int generateDelay) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.queuedTime = queuedTime;
        this.generateDelay = generateDelay;
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

    public String getLocationKey() {
        return key;
    }


    public static QueueBlock of(Block block, long queuedTime, int generateDelay) {
        return new QueueBlock(block.getWorld(), block.getX(), block.getY(), block.getZ(), queuedTime, generateDelay);
    }

    public static String formatLocationKey(int x, int y, int z, String worldName) {
        return x + "," + y + "," + z + "," + worldName;
    }

    public static String getLocationKeyOfBlock(Block block) {
        return formatLocationKey(block.getX(), block.getY(), block.getZ(), block.getWorld().getName());
    }

}
