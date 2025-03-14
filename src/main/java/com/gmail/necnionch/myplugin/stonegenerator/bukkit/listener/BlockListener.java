package com.gmail.necnionch.myplugin.stonegenerator.bukkit.listener;

import com.gmail.necnionch.myplugin.stonegenerator.bukkit.StoneGenerateManager;
import com.gmail.necnionch.myplugin.stonegenerator.bukkit.config.WorldSetting;
import com.gmail.necnionch.myplugin.stonegenerator.bukkit.util.QueueBlock;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Optional;

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

        // 採掘された面を取得
        Location eyeLocation = player.getEyeLocation();
        RayTraceResult result = player.getWorld().rayTraceBlocks(eyeLocation, eyeLocation.getDirection(), eyeLocation.distance(block.getLocation().add(.5, .5, .5)) + 1, FluidCollisionMode.NEVER);
        BlockFace hitBlockFace = (result == null || result.getHitBlockFace() == null || !block.equals(result.getHitBlock())) ? null : result.getHitBlockFace();

        // ブロックをテストする？
        if (!setting.getTargetBlocks().getDeepTypes().isEmpty()) {
            if (hitBlockFace == null) {
                event.setCancelled(true);  // bug?
                return;
            }

            Vector testDirection = hitBlockFace.getDirection().multiply(-1);
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
                QueueBlock.BreakInfo breakInfo = new QueueBlock.BreakInfo(player, hitBlockFace);
                gen.queueBreakBlock(setting, block, breakInfo, () -> block.setType(setting.getOverrideFillBlockType(bSetting)));
                break;
            }
        }

    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(BlockDropItemEvent event) {
        QueueBlock.BreakInfo breakInfo = Optional.ofNullable(gen.getQueue(event.getBlock()))
                .map(QueueBlock::getBreakInfo)
                .orElse(null);

        if (breakInfo == null || breakInfo.getFace() == null)
            return;

        Vector direction = breakInfo.getFace().getDirection();
        for (Item item : event.getItems()) {
            Location location = item.getLocation().add(direction);
            item.teleport(location);

            Vector mod = breakInfo.getPlayer().getEyeLocation().subtract(location).toVector().multiply(.1);
            item.setVelocity(mod);
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
