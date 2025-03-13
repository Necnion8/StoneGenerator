package com.gmail.necnionch.myplugin.stonegenerator.bukkit.listener;

import com.gmail.necnionch.myplugin.stonegenerator.bukkit.StoneGenerateManager;
import com.gmail.necnionch.myplugin.stonegenerator.bukkit.config.WorldSetting;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Optional;

public class BlockListener implements Listener {

    private final StoneGenerateManager gen;

    public BlockListener(StoneGenerateManager genManager) {
        this.gen = genManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (gen.containsQueue(block)) {
            event.setCancelled(true);
            return;
        }

        WorldSetting setting = gen.getConfig()
                .getWorld(block.getWorld())
                .orElse(null);

        if (setting == null)
            return;

        for (WorldSetting.GenerateBlock bSetting : setting.blocks()) {
            if (block.getType().equals(bSetting.getType())) {
                gen.queueBreakBlock(block, () ->  block.setType(Optional.ofNullable(bSetting.getReplaceType())
                        .orElse(setting.getFillBlockType())));
                break;
            }
        }

    }


}
