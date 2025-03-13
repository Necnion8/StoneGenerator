package com.gmail.necnionch.myplugin.stonegenerator.bukkit;

import com.gmail.necnionch.myplugin.stonegenerator.bukkit.config.StoneGeneratorConfig;
import com.gmail.necnionch.myplugin.stonegenerator.bukkit.config.WorldSetting;
import com.gmail.necnionch.myplugin.stonegenerator.bukkit.util.QueueBlock;
import com.gmail.necnionch.myplugin.stonegenerator.bukkit.util.SGUtil;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StoneGenerateManager {

    private static final Set<Material> AIR_TYPES = Stream.of(Material.values())
            .filter(m -> m.name().contains("AIR"))
            .collect(Collectors.toSet());
    private final Plugin owner;
    private final StoneGeneratorConfig config;
    private final SGUtil util;
    private final Random random = new Random();
    private final Multimap<World, QueueBlock> queuedBlocks = ArrayListMultimap.create();
    private final Map<String, QueueBlock> queuedBlockKeys = new HashMap<>();
    private final NamespacedKey queueBlockXPDCKey;
    private final NamespacedKey queueBlockYPDCKey;
    private final NamespacedKey queueBlockZPDCKey;
    private final NamespacedKey queueBlockTimePDCKey;
    private @Nullable BukkitTask task;


    public StoneGenerateManager(Plugin owner, SGUtil util, StoneGeneratorConfig config) {
        this.owner = owner;
        this.util = util;
        this.config = config;
        this.queueBlockXPDCKey = new NamespacedKey(owner, "qx");
        this.queueBlockYPDCKey = new NamespacedKey(owner, "qy");
        this.queueBlockZPDCKey = new NamespacedKey(owner, "qz");
        this.queueBlockTimePDCKey = new NamespacedKey(owner, "qt");
    }

    public BukkitTask runTask(Runnable task) {
        return owner.getServer().getScheduler().runTask(owner, task);
    }


    public StoneGeneratorConfig getConfig() {
        return config;
    }

    public void startTimer() {
        if (task != null && !task.isCancelled())
            return;
        task = owner.getServer().getScheduler().runTaskTimer(owner, this::tick, 0, 0);
    }

    public void stopTimer() {
        if (task != null)
            task.cancel();
        task = null;
    }

    private void tick() {
        long now = System.currentTimeMillis();

        for (Iterator<QueueBlock> it = queuedBlocks.values().iterator(); it.hasNext(); ) {
            QueueBlock queueBlock = it.next();
            getConfig().getWorld(queueBlock.getWorld()).ifPresent(setting -> {
                if (setting.getGenerateMinTime() * 1000 <= now - queueBlock.getQueuedTime()) {
                    it.remove();
                    queuedBlockKeys.values().remove(queueBlock);

                    Block block = queueBlock.getWorld().getBlockAt(queueBlock.getX(), queueBlock.getY(), queueBlock.getZ());
                    if (setting.blocks().isEmpty())
                        return;

                    WorldSetting.GenerateBlock bSetting = setting.blocks().get(random.nextInt(setting.blocks().size()));
                    block.setType(bSetting.getType());  // TODO: set priority
                }
            });
        }

    }


    public void clearQueueBlocks() {
        queuedBlocks.clear();
        queuedBlockKeys.clear();
    }

    public void clearQueueBlocks(World world) {
        Collection<QueueBlock> blocks = queuedBlocks.removeAll(world);
        queuedBlockKeys.values().removeAll(blocks);
        // TODO: rollback? or commit pdc?

    }

    public void putQueueBlock(QueueBlock queueBlock) {
        queuedBlocks.put(queueBlock.getWorld(), queueBlock);
        queuedBlockKeys.put(queueBlock.getKey(), queueBlock);
    }

    public void queueBreakBlock(Block block, Runnable placer) {
        long queuedTime = System.currentTimeMillis();
        runTask(() -> {
            Block newBlock = block.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ());
            if (!AIR_TYPES.contains(newBlock.getType()))  // ignore
                return;

            putQueueBlock(QueueBlock.of(block, queuedTime));
            placer.run();
        });

    }

    public boolean containsQueue(Block block) {
        return queuedBlockKeys.containsKey(QueueBlock.getKeyOfBlock(block));
    }

    public Multimap<World, QueueBlock> queuedBlocks() {
        return queuedBlocks;
    }


    public Set<QueueBlock> storeQueueBlocksToChunk(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();

        Set<QueueBlock> storeBlocks = new HashSet<>();
        for (Iterator<QueueBlock> it = queuedBlocks.get(chunk.getWorld()).iterator(); it.hasNext(); ) {
            QueueBlock queueBlock = it.next();
            if (Math.floor(queueBlock.getX() / 16f) == chunk.getX() && Math.floor(queueBlock.getZ() / 16f) == chunk.getZ()) {
                storeBlocks.add(queueBlock);
                it.remove();
            }
        }

        if (storeBlocks.isEmpty()) {
            pdc.remove(queueBlockXPDCKey);
            pdc.remove(queueBlockYPDCKey);
            pdc.remove(queueBlockZPDCKey);
            pdc.remove(queueBlockTimePDCKey);
            return Collections.emptySet();
        }

        List<Integer> xValues = new ArrayList<>();
        List<Integer> yValues = new ArrayList<>();
        List<Integer> zValues = new ArrayList<>();
        List<Long> timeValues = new ArrayList<>();

        for (QueueBlock queueBlock : storeBlocks) {
            xValues.add(Math.floorMod(queueBlock.getX(), 16));
            yValues.add(queueBlock.getY());
            zValues.add(Math.floorMod(queueBlock.getZ(), 16));
            timeValues.add(queueBlock.getQueuedTime());
        }

        pdc.set(queueBlockXPDCKey, PersistentDataType.INTEGER_ARRAY, xValues.stream().mapToInt(i -> i).toArray());
        pdc.set(queueBlockYPDCKey, PersistentDataType.INTEGER_ARRAY, yValues.stream().mapToInt(i -> i).toArray());
        pdc.set(queueBlockZPDCKey, PersistentDataType.INTEGER_ARRAY, zValues.stream().mapToInt(i -> i).toArray());
        pdc.set(queueBlockTimePDCKey, PersistentDataType.LONG_ARRAY, timeValues.stream().mapToLong(l -> l).toArray());

        return storeBlocks;
    }

    public Set<QueueBlock> restoreQueueBlocksFromChunk(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();

        int[] xValues = pdc.get(queueBlockXPDCKey, PersistentDataType.INTEGER_ARRAY);
        int[] yValues = pdc.get(queueBlockYPDCKey, PersistentDataType.INTEGER_ARRAY);
        int[] zValues = pdc.get(queueBlockZPDCKey, PersistentDataType.INTEGER_ARRAY);
        long[] timeValues = pdc.get(queueBlockTimePDCKey, PersistentDataType.LONG_ARRAY);

        if (xValues == null || yValues == null || zValues == null || timeValues == null)
            return Collections.emptySet();

        Set<QueueBlock> queueBlocks = new HashSet<>();
        for (int i = 0; i < xValues.length; i++) {
            queueBlocks.add(new QueueBlock(
                    chunk.getWorld(),
                    chunk.getX() * 16 + xValues[i],
                    yValues[i],
                    chunk.getZ() * 16 + zValues[i],
                    timeValues[i]
            ));
        }

        queueBlocks.forEach(this::putQueueBlock);
        return queueBlocks;
    }

}
