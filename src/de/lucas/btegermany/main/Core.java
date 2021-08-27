package de.lucas.btegermany.main;

/*
 * Copyright (c) BTE Germany 2021.2
 * Plugin by Lucas L. - CompileException.
 */

import de.lucas.btegermany.bteteleport.bungee.AutoCompletion;
import de.lucas.btegermany.bteteleport.bungee.BungeeReceive;
import de.lucas.btegermany.bteteleport.bungee.Metrics;
import de.lucas.btegermany.bteteleport.bungee.commands.bta;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

public class Core extends Plugin {
    private static Plugin instance;
    public static String oldChannel = "BungeeTeleport";
    public static String channel = "bungeeteleport:main";

    public void onEnable() {
        instance = this;

        /*
        / Teleport
        */
        ProxyServer.getInstance().registerChannel(oldChannel);
        ProxyServer.getInstance().registerChannel(channel);
        ProxyServer.getInstance().getPluginManager().registerListener(this, new BungeeReceive());
        ProxyServer.getInstance().getPluginManager().registerListener(this, new AutoCompletion());
        for (String cmd : bta.getCmds())
            ProxyServer.getInstance().getPluginManager()
                    .registerCommand(this, (Command)new bta(cmd));
        Metrics metrics = new Metrics(this);
        /*
        / Teleport
        */
    }

    public void onDisable() {
        instance = null;
    }

    public static Plugin getInstance() {
        return instance;
    }
}