package com.gmail.necnionch.myplugin.stonegenerator.bukkit.config;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class WorldSetting {

    private final List<GenerateBlock> generateBlocks;
    private final Material fillBlockType;
    private final int genMinTime;
    private final int genMaxTime;
    private final TargetBlocks targetBlocks;

    public WorldSetting(List<GenerateBlock> blocks, Material fillBlockType, int genMinTime, int genMaxTime, TargetBlocks targetBlocks) {
        this.generateBlocks = blocks;
        this.fillBlockType = fillBlockType;
        this.genMinTime = genMinTime;
        this.genMaxTime = genMaxTime;
        this.targetBlocks = targetBlocks;
    }

    public List<GenerateBlock> blocks() {
        return generateBlocks;
    }

    public Material getFillBlockType() {
        return fillBlockType;
    }

    public long getGenerateMinTime() {
        return genMinTime;
    }

    public int getGenerateMaxTime() {
        return genMaxTime;
    }

    public TargetBlocks getTargetBlocks() {
        return targetBlocks;
    }

    public Optional<GenerateBlock> getBlock(Material type) {
        return generateBlocks.stream().filter(b -> type.equals(b.type)).findAny();
    }


    public static final class GenerateBlock {

        private final Material type;
        private final int priority;
        private final @Nullable Material replaceType;

        public GenerateBlock(Material blockType, int priority, @Nullable Material replaceType) {
            this.type = blockType;
            this.priority = priority;
            this.replaceType = replaceType;
        }

        public Material getType() {
            return type;
        }

        public @Nullable Material getReplaceType() {
            return replaceType;
        }

        public int getPriority() {
            return priority;
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
