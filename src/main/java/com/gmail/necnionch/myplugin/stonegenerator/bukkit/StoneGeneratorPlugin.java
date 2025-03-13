package com.gmail.necnionch.myplugin.stonegenerator.bukkit;

import com.gmail.necnionch.myplugin.stonegenerator.bukkit.config.StoneGeneratorConfig;
import com.gmail.necnionch.myplugin.stonegenerator.bukkit.listener.BlockListener;
import com.gmail.necnionch.myplugin.stonegenerator.bukkit.listener.WorldListener;
import com.gmail.necnionch.myplugin.stonegenerator.bukkit.util.SGUtil;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Supplier;

public final class StoneGeneratorPlugin extends JavaPlugin implements SGUtil {

    private final StoneGeneratorConfig mainConfig = new StoneGeneratorConfig(this);
    private final StoneGenerateManager genManager = new StoneGenerateManager(this, this, mainConfig);

    @Override
    public void onEnable() {
        mainConfig.load();
        genManager.startTimer();
        d("restored " + restoreAllChunks() + " blocks (plugin load)");

        getServer().getPluginManager().registerEvents(new BlockListener(genManager), this);
        getServer().getPluginManager().registerEvents(new WorldListener(genManager, this), this);

    }

    @Override
    public void onDisable() {
        genManager.stopTimer();
        d("stored " + storeAllChunks() + " blocks (plugin unload)");
        genManager.clearQueueBlocks();
    }


    private int restoreAllChunks() {
        int count = 0;
        for (World world : getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                count += genManager.restoreQueueBlocksFromChunk(chunk).size();
            }
        }
        return count;
    }

    private int storeAllChunks() {
        int count = 0;
        for (World world : getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                count += genManager.storeQueueBlocksToChunk(chunk).size();
            }
        }
        return count;
    }


    @Override
    public void d(Supplier<String> message) {
        if (mainConfig.isDebug())
            d(message.get());
    }

    @Override
    public void d(String message) {
        if (mainConfig.isDebug())
            getLogger().warning("[DEBUG]: " + message);
    }
}
