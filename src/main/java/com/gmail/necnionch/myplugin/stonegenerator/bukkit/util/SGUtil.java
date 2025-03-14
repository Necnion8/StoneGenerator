package com.gmail.necnionch.myplugin.stonegenerator.bukkit.util;

import java.util.function.Supplier;
import java.util.logging.Logger;

public interface SGUtil {

    Logger getLogger();

    void d(String message);

    void d(Supplier<String> message);

    void reloadPluginConfig();

}
