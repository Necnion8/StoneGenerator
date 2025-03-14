package com.gmail.necnionch.myplugin.stonegenerator.bukkit.config;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class WorldSetting {

    private final List<GenerateBlock> generateBlocks;
    private final Material fillBlockType;
    private final int genMinTime;
    private final int genMaxTime;
    private final @Nullable Sound generateSound;
    private final TargetBlocks targetBlocks;

    public WorldSetting(List<GenerateBlock> blocks, Material fillBlockType, int genMinTime, int genMaxTime, @Nullable Sound generateSound, TargetBlocks targetBlocks) {
        this.generateBlocks = blocks;
        this.fillBlockType = fillBlockType;
        this.genMinTime = genMinTime;
        this.genMaxTime = genMaxTime;
        this.generateSound = generateSound;
        this.targetBlocks = targetBlocks;
    }

    public List<GenerateBlock> blocks() {
        return generateBlocks;
    }

    public Material getFillBlockType() {
        return fillBlockType;
    }

    public int getGenerateMinTime() {
        return genMinTime;
    }

    public int getGenerateMaxTime() {
        return genMaxTime;
    }

    public @Nullable Sound getGenerateSound() {
        return generateSound;
    }

    public TargetBlocks getTargetBlocks() {
        return targetBlocks;
    }


    public Material getOverrideFillBlockType(GenerateBlock generateBlock) {
        return Optional.ofNullable(generateBlock.getFillBlockType()).orElse(fillBlockType);
    }

    public @Nullable Sound getOverrideGenerateSound(GenerateBlock generateBlock) {
        if (generateBlock.isOverrideGenerateSound()) {
            return generateBlock.getGenerateSound();
        }
        return generateSound;
    }


    public static final class GenerateBlock {

        private final Material type;
        private final int priority;
        private final @Nullable Material fillBlockType;
        private final @Nullable Sound generateSound;
        private final boolean overrideGenerateSound;

        public GenerateBlock(Material blockType, int priority, @Nullable Material fillBlockType, @Nullable Sound generateSound, boolean overrideGenerateSound) {
            this.type = blockType;
            this.priority = priority;
            this.fillBlockType = fillBlockType;
            this.generateSound = generateSound;
            this.overrideGenerateSound = overrideGenerateSound;
        }

        public Material getType() {
            return type;
        }

        public @Nullable Material getFillBlockType() {
            return fillBlockType;
        }

        public int getPriority() {
            return priority;
        }

        public @Nullable Sound getGenerateSound() {
            return generateSound;
        }

        public boolean isOverrideGenerateSound() {
            return overrideGenerateSound;
        }

    }


    public static final class TargetBlocks {

        private final List<Material> deepTypes;

        public TargetBlocks(List<Material> deepTypes) {
            this.deepTypes = deepTypes;
        }

        public List<Material> getDeepTypes() {
            return deepTypes;
        }

    }

}
