package com.gmail.necnionch.myplugin.stonegenerator.bukkit.command;

import com.gmail.necnionch.myplugin.stonegenerator.bukkit.StoneGenerateManager;
import com.gmail.necnionch.myplugin.stonegenerator.bukkit.util.QueueBlock;
import com.gmail.necnionch.myplugin.stonegenerator.bukkit.util.SGUtil;
import com.google.common.collect.Multimap;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StoneGeneratorCommand implements TabExecutor {

    private final SGUtil util;
    private final StoneGenerateManager gen;

    public StoneGeneratorCommand(SGUtil util, StoneGenerateManager genManager) {
        this.util = util;
        this.gen = genManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (1 <= args.length && "status".equalsIgnoreCase(args[0])) {
            Multimap<World, QueueBlock> queuedBlocks = gen.queuedBlocks();
            if (queuedBlocks.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "生成がキューされているアクティブなブロックは現在ありません");
                return true;
            }

            sender.sendMessage(ChatColor.GOLD + "生成がキューされているアクティブなブロックが " + queuedBlocks.values().size() + "個 あります");
            queuedBlocks.asMap().forEach((w, blocks) -> {
                sender.sendMessage(ChatColor.GRAY + "- ワールド: " + ChatColor.WHITE + w.getName() + ChatColor.YELLOW + " -> " + ChatColor.WHITE + blocks.size() + " ブロック");
            });

        } else if (1 <= args.length && "reload".equalsIgnoreCase(args[0])) {
            util.reloadPluginConfig();
            sender.sendMessage(ChatColor.GOLD + "設定ファイルを再読み込みしました");

        } else {
            return false;
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Stream.of("status", "reload")
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

}
