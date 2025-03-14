package com.gmail.necnionch.myplugin.stonegenerator.bukkit;

import com.gmail.necnionch.myplugin.stonegenerator.bukkit.config.StoneGeneratorConfig;
import com.gmail.necnionch.myplugin.stonegenerator.bukkit.config.WorldSetting;
import com.gmail.necnionch.myplugin.stonegenerator.bukkit.util.QueueBlock;
import com.gmail.necnionch.myplugin.stonegenerator.bukkit.util.SGUtil;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

public class StoneGenerateManager {

    private final Plugin owner;
    private final StoneGeneratorConfig config;
    private final SGUtil util;
    private final Random random = new Random();
    private final Multimap<World, QueueBlock> queuedBlocks = ArrayListMultimap.create();
    private final Map<String, QueueBlock> queuedBlockKeys = new HashMap<>();  // 位置キーとQueueBlockのマップ
    private final Map<String, Integer> storedQueueBlockGenerateDelays = new HashMap<>();  // 位置キーとアンロード済みQueueBlockの生成待ち時間
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
                boolean delete = true;
                try {
                    delete = tickToQueueBlock(now, queueBlock, setting);
                } catch (Exception e) {
                    util.getLogger().log(Level.SEVERE, "Exception in tickToQueueBlock", e);
                }
                if (delete) {
                    it.remove();
                    queuedBlockKeys.values().remove(queueBlock);
                    storedQueueBlockGenerateDelays.remove(queueBlock.getLocationKey());
                }
            });
        }

    }

    private boolean tickToQueueBlock(long nowTime, QueueBlock queueBlock, WorldSetting setting) {
        if (queueBlock.getGenerateDelay() * 1000L <= nowTime - queueBlock.getQueuedTime()) {
            List<WorldSetting.GenerateBlock> blocks = setting.blocks();
            if (!blocks.isEmpty()) {
                float targetPriority = blocks.stream().mapToInt(WorldSetting.GenerateBlock::getPriority).sum() * random.nextFloat();
                int currentPriority = 0;

                WorldSetting.GenerateBlock selected = null;
                for (WorldSetting.GenerateBlock bSetting : blocks) {
                    currentPriority += bSetting.getPriority();
                    if (targetPriority <= currentPriority) {
                        selected = bSetting;
                        break;
                    }
                }

                World world = queueBlock.getWorld();
                Block block = world.getBlockAt(queueBlock.getX(), queueBlock.getY(), queueBlock.getZ());
                block.setType(Objects.requireNonNull(selected, "priority bug?").getType());

                Sound sound = setting.getOverrideGenerateSound(selected);
                if (sound != null) {
                    world.playSound(block.getLocation().add(.5, .5, .5), sound, SoundCategory.BLOCKS, 1f, 1f);
                }
            }
            return true;
        }
        return false;
    }


    /**
     * メモリ内のデータをクリアします
     */
    public void clear() {
        queuedBlocks.clear();
        queuedBlockKeys.clear();
        storedQueueBlockGenerateDelays.clear();
    }

    /**
     * {@link QueueBlock} を処理リストに追加します
     */
    public void putQueueBlock(QueueBlock queueBlock) {
        queuedBlocks.put(queueBlock.getWorld(), queueBlock);
        queuedBlockKeys.put(queueBlock.getLocationKey(), queueBlock);
    }

    /**
     * 採掘されたブロックをキューして、丸石を設置します
     */
    public void queueBreakBlock(WorldSetting setting, Block block, QueueBlock.BreakInfo breakInfo, Runnable placer) {
        int min = Math.min(setting.getGenerateMinTime(), setting.getGenerateMaxTime());
        int max = Math.max(setting.getGenerateMinTime(), setting.getGenerateMaxTime());
        int generateDelay = (int) (min + (max - min) * random.nextFloat());

        putQueueBlock(QueueBlock.of(block, System.currentTimeMillis(), generateDelay, breakInfo));
        runTask(placer);
    }

    /**
     * キューされたブロックなら true を返します
     */
    public boolean containsQueue(Block block) {
        return queuedBlockKeys.containsKey(QueueBlock.getLocationKeyOfBlock(block));
    }

    /**
     * キューされたブロックを削除します
     */
    public @Nullable QueueBlock removeQueue(Block block) {
        String key = QueueBlock.getLocationKeyOfBlock(block);
        QueueBlock queueBlock = queuedBlockKeys.remove(key);
        if (queueBlock != null) {
            queuedBlocks.values().remove(queueBlock);
            storedQueueBlockGenerateDelays.remove(key);
        }
        return queueBlock;
    }

    /**
     * キューされたブロックを返します
     */
    public @Nullable QueueBlock getQueue(Block block) {
        return queuedBlockKeys.get(QueueBlock.getLocationKeyOfBlock(block));
    }

    /**
     * キューされている全てのブロックを返します
     */
    public Multimap<World, QueueBlock> queuedBlocks() {
        return queuedBlocks;
    }

    /**
     * チャンク内の {@link QueueBlock} を全てチャンクのPDCに保存し、キューから削除します
     */
    public Set<QueueBlock> storeQueueBlocksToChunk(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();

        Set<QueueBlock> storeBlocks = new HashSet<>();
        for (Iterator<QueueBlock> it = queuedBlocks.get(chunk.getWorld()).iterator(); it.hasNext(); ) {
            QueueBlock queueBlock = it.next();
            if (Math.floor(queueBlock.getX() / 16f) == chunk.getX() && Math.floor(queueBlock.getZ() / 16f) == chunk.getZ()) {
                storeBlocks.add(queueBlock);
                it.remove();

                // プラグインがアンロードされるまで生成時間を保持することで
                // チャンクアンロードを繰り返すことによる再抽選を防ぐ
                storedQueueBlockGenerateDelays.put(queueBlock.getLocationKey(), queueBlock.getGenerateDelay());
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

    /**
     * チャンクのPDCに保存された {@link QueueBlock} を全てキューに復元します<br>
     * この処理で生成までの遅延が再設定されます。ワールド設定が存在しない場合は無視されます
     */
    public Set<QueueBlock> restoreQueueBlocksFromChunk(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        int[] xValues = pdc.get(queueBlockXPDCKey, PersistentDataType.INTEGER_ARRAY);
        int[] yValues = pdc.get(queueBlockYPDCKey, PersistentDataType.INTEGER_ARRAY);
        int[] zValues = pdc.get(queueBlockZPDCKey, PersistentDataType.INTEGER_ARRAY);
        long[] timeValues = pdc.get(queueBlockTimePDCKey, PersistentDataType.LONG_ARRAY);

        if (xValues == null || yValues == null || zValues == null || timeValues == null)
            return Collections.emptySet();

        WorldSetting setting = getConfig().getWorld(chunk.getWorld()).orElse(null);
        if (setting == null) {
            util.d(() -> "Not configured in " + chunk.getWorld().getName());
            return Collections.emptySet();
        }

        Set<QueueBlock> queueBlocks = new HashSet<>();
        int min = Math.min(setting.getGenerateMinTime(), setting.getGenerateMaxTime());
        int max = Math.max(setting.getGenerateMinTime(), setting.getGenerateMaxTime());

        for (int i = 0; i < xValues.length; i++) {
            QueueBlock queueBlock = new QueueBlock(
                    chunk.getWorld(),
                    chunk.getX() * 16 + xValues[i],
                    yValues[i],
                    chunk.getZ() * 16 + zValues[i],
                    timeValues[i],
                    0,
                    null
            );
            queueBlock.setGenerateDelay(
                    Optional.ofNullable(storedQueueBlockGenerateDelays.remove(queueBlock.getLocationKey()))
                            .orElseGet(() -> (int) (min + (max - min) * random.nextFloat())));
            queueBlocks.add(queueBlock);
        }

        queueBlocks.forEach(this::putQueueBlock);
        return queueBlocks;
    }

}
