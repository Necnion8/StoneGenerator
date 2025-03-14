package com.gmail.necnionch.myplugin.stonegenerator.bukkit.listener;

import com.gmail.necnionch.myplugin.stonegenerator.bukkit.StoneGenerateManager;
import com.gmail.necnionch.myplugin.stonegenerator.bukkit.config.WorldSetting;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class BlockListener implements Listener {

    private final StoneGenerateManager gen;

    public BlockListener(StoneGenerateManager genManager) {
        this.gen = genManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // クリエイティブによる破壊は常にキューから削除して無視
        if (GameMode.CREATIVE.equals(player.getGameMode())) {
            gen.removeQueue(block);
            return;
        }

        // 生成待機してるならキャンセル
        if (gen.containsQueue(block)) {
            event.setCancelled(true);
            return;
        }


        WorldSetting setting = gen.getConfig()
                .getWorld(block.getWorld())
                .orElse(null);

        // ワールドの設定がない？
        if (setting == null)
            return;

        // ブロックをテストする？
        if (!setting.getTargetBlocks().getDeepTypes().isEmpty()) {
            Location eyeLocation = player.getEyeLocation();
            RayTraceResult result = player.getWorld().rayTraceBlocks(eyeLocation, eyeLocation.getDirection(), eyeLocation.distance(block.getLocation().add(.5, .5, .5)) + 1, FluidCollisionMode.NEVER);

            if (result == null || result.getHitBlockFace() == null || !block.equals(result.getHitBlock())) {
                event.setCancelled(true);  // bug?
                return;
            }

            Vector testDirection = result.getHitBlockFace().getDirection().multiply(-1);
            Location testBlockPos = block.getLocation();

            for (Material deepType : setting.getTargetBlocks().getDeepTypes()) {
                testBlockPos.add(testDirection);

                // テストにマッチしない？
                if (deepType != testBlockPos.getBlock().getType())
                    return;
            }
        }

        // 自動生成処理の対象ブロック？
        for (WorldSetting.GenerateBlock bSetting : setting.blocks()) {
            if (block.getType().equals(bSetting.getType())) {
                gen.queueBreakBlock(setting, block, () -> block.setType(setting.getOverrideFillBlockType(bSetting)));
                break;
            }
        }

    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (gen.containsQueue(event.getBlock()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> gen.queuedBlocks().values().stream().anyMatch(q ->
                block.getWorld().equals(q.getWorld()) && block.getX() == q.getX() && block.getY() == q.getY() && block.getZ() == q.getZ()
        ));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> gen.queuedBlocks().values().stream().anyMatch(q ->
                block.getWorld().equals(q.getWorld()) && block.getX() == q.getX() && block.getY() == q.getY() && block.getZ() == q.getZ()
        ));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (gen.containsQueue(event.getBlock()))
            event.setCancelled(true);
    }

}
