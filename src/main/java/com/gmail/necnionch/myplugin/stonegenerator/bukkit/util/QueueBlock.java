package com.gmail.necnionch.myplugin.stonegenerator.bukkit.util;

import org.bukkit.World;
import org.bukkit.block.Block;

public class QueueBlock {

    private final World world;
    private final int x;
    private final int y;
    private final int z;
    private final long queuedTime;

    public QueueBlock(World world, int x, int y, int z, long queuedTime) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.queuedTime = queuedTime;
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

    public long getQueuedTime() {
        return queuedTime;
    }

    public String getKey() {
        return x + "," + y + "," + z + "," + world.getName();
    }


    public static QueueBlock of(Block block, long queuedTime) {
        return new QueueBlock(block.getWorld(), block.getX(), block.getY(), block.getZ(), queuedTime);
    }

    public static String getKeyOfBlock(Block block) {
        return block.getX() + "," + block.getY() + "," + block.getZ() + "," + block.getWorld().getName();
    }

}
