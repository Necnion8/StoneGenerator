package com.gmail.necnionch.myplugin.stonegenerator.bukkit.config;

import com.gmail.necnionch.myplugin.stonegenerator.common.BukkitConfigDriver;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class StoneGeneratorConfig extends BukkitConfigDriver {

    private final Map<String, WorldSetting> worlds = new HashMap<>();
    private boolean debug;

    public StoneGeneratorConfig(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onLoaded(FileConfiguration config) {
        worlds.clear();
        debug = config.getBoolean("debug", false);

        ConfigurationSection wMapSection = config.getConfigurationSection("worlds");
        if (wMapSection == null)
            return true;

        for (String worldName : wMapSection.getKeys(false)) {
            ConfigurationSection wSection = Objects.requireNonNull(wMapSection.getConfigurationSection(worldName));

            String tmp = Optional.ofNullable(wSection.getString("fill-type")).orElse("").toUpperCase(Locale.ROOT);
            Material fillBlockType;
            try {
                fillBlockType = Material.valueOf(tmp);
            } catch (IllegalArgumentException e) {
                getLogger().severe("Unknown block type: " + tmp + " (in " + worldName + " world, fill-type)");
                continue;
            }

            int genMinTime = wSection.getInt("generate-time-min", 30);
            int genMaxTime = wSection.getInt("generate-time-max", 600);
            List<WorldSetting.GenerateBlock> blocks = new ArrayList<>();

            Optional.ofNullable(getConfigList(wSection, "generate-blocks")).ifPresent(bListSection -> {
                for (ConfigurationSection bSection : bListSection) {
                    String tmp2 = Optional.ofNullable(bSection.getString("type")).orElse("").toUpperCase(Locale.ROOT);
                    Material type;
                    try {
                        type = Material.valueOf(tmp2);
                    } catch (IllegalArgumentException e) {
                        getLogger().severe("Unknown block type: " + tmp2 + " (in " + worldName + " world, generate-blocks.type)");
                        continue;
                    }

                    Material fillType = Optional.ofNullable(bSection.getString("fill-type"))
                            .map(s -> s.toUpperCase(Locale.ROOT))
                            .map(s -> {
                                try {
                                    return Material.valueOf(s);
                                } catch (IllegalArgumentException e) {
                                    getLogger().severe("Unknown block type: " + s + " (in " + worldName + " world, generate-blocks.fill-type)");
                                    return null;
                                }
                            })
                            .orElse(null);

                    Sound genSound = Optional.ofNullable(bSection.getString("generate-sound"))
                            .map(s -> s.toUpperCase(Locale.ROOT))
                            .map(s -> {
                                if ("-1".equalsIgnoreCase(s))
                                    return null;
                                try {
                                    return Sound.valueOf(s);
                                } catch (IllegalArgumentException e) {
                                    getLogger().warning("Unknown sound: " + s + " (in " + worldName + " world, generate-blocks.generate-sound)");
                                    return null;
                                }
                            })
                            .orElse(null);

                    int priority = bSection.getInt("priority", 1);
                    boolean overrideGenSound = bSection.getKeys(false).contains("generate-sound");

                    blocks.add(new WorldSetting.GenerateBlock(type, priority, fillType, genSound, overrideGenSound));
                }
            });

            List<Material> targetDeepTypes = new ArrayList<>();
            for (String typeName : wSection.getStringList("target-blocks.deep-types")) {
                typeName = typeName.toUpperCase(Locale.ROOT);
                try {
                    targetDeepTypes.add(Material.valueOf(typeName));
                } catch (IllegalArgumentException e) {
                    getLogger().severe("Unknown block type: " + typeName + " (in " + worldName + " world, generate-blocks.target-blocks.deep-types)");
                }
            }

            Sound genSound = Optional.ofNullable(wSection.getString("generate-sound"))
                    .map(s -> s.toUpperCase(Locale.ROOT))
                    .map(s -> {
                        if ("-1".equalsIgnoreCase(s))
                            return null;
                        try {
                            return Sound.valueOf(s);
                        } catch (IllegalArgumentException e) {
                            getLogger().warning("Unknown sound: " + s + " (in " + worldName + " world, generate-sound)");
                            return null;
                        }
                    })
                    .orElse(null);

            worlds.put(worldName, new WorldSetting(
                    blocks, fillBlockType, genMinTime, genMaxTime, genSound, new WorldSetting.TargetBlocks(targetDeepTypes)
            ));
        }
        return true;
    }

    public boolean isDebug() {
        return debug;
    }

    public Optional<WorldSetting> getWorld(String worldName) {
        return Optional.ofNullable(worlds.get(worldName));
    }

    public Optional<WorldSetting> getWorld(org.bukkit.World world) {
        return getWorld(world.getName());
    }


    //

    private static List<ConfigurationSection> getConfigList(ConfigurationSection parent, String key) {
        /*
          https://bukkit.org/threads/getting-a-list-of-configurationsections.157524/
         */
        List<?> list = parent.getList(key);
        return list != null ? list.stream()
                .filter(obj -> obj instanceof Map)
                .map(obj -> createMemoryConfigurationFromMap((Map<?, ?>) obj))
                .collect(Collectors.toList()) : null;
    }

    private static void putMapToMemoryConfiguration(MemoryConfiguration configuration, Map<?, ?> map) {
        map.forEach((k, v) -> {
            if (v instanceof Map) {
                configuration.set((String) k, createMemoryConfigurationFromMap((Map<?, ?>) v));
            } else {
                configuration.set((String) k, v);
            }
        });
    }

    private static MemoryConfiguration createMemoryConfigurationFromMap(Map<?, ?> map) {
        MemoryConfiguration nest = new MemoryConfiguration();
        putMapToMemoryConfiguration(nest, map);
        return nest;
    }


}
