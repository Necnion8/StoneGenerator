package com.gmail.necnionch.myplugin.stonegenerator.bukkit.listener;

import com.gmail.necnionch.myplugin.stonegenerator.bukkit.StoneGenerateManager;
import com.gmail.necnionch.myplugin.stonegenerator.bukkit.util.SGUtil;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class WorldListener implements Listener {

    private final StoneGenerateManager gen;
    private final SGUtil util;

    public WorldListener(StoneGenerateManager genManager, SGUtil util) {
        this.gen = genManager;
        this.util = util;
    }

    @EventHandler
    public void onUnload(WorldUnloadEvent event) {
        for (Chunk chunk : event.getWorld().getLoadedChunks()) {
            int count = gen.storeQueueBlocksToChunk(chunk).size();
            if (0 < count) {
                util.d(() -> "stored " + count + " blocks in " + event.getWorld().getName() + " (world unload)");
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        int count = gen.restoreQueueBlocksFromChunk(chunk).size();
        if (0 < count) {
            util.d(() -> "restore " + count + " blocks in " + event.getWorld().getName() + " " + chunk.getX() + "," + chunk.getZ() + " (chunk load)");
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        int count = gen.storeQueueBlocksToChunk(chunk).size();
        if (0 < count) {
            util.d(() -> "store " + count + " blocks in " + event.getWorld().getName() + " " + chunk.getX() + "," + chunk.getZ() + " (chunk unload)");
        }
    }

}
