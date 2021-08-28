package de.lucas.btegermany.main;

/*
 * Copyright (c) BTE Germany 2021.
 * Plugin by Lucas L. - CompileException.
 */

import de.lucas.btegermany.bteteleport.bungee.AutoCompletion;
import de.lucas.btegermany.bteteleport.bungee.BungeeReceive;
import de.lucas.btegermany.bteteleport.bungee.Metrics;
import de.lucas.btegermany.bteteleport.bungee.commands.bta;
import de.lucas.btegermany.datasave.Strings;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

public class Core extends Plugin {
    private static Plugin instance;
    public static String oldChannel = "BTETeleport";
    public static String channel = "bteteleport:main";

    public void onEnable() {
        try {
            BungeeCord.getInstance().getConsole().sendMessage(Strings.prefix + "§6BungeeCord-System fährt hoch ......");
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

            BungeeCord.getInstance().getConsole().sendMessage(Strings.prefix + " ...... §aBungeeCord-System Hochgefahren");
        } catch (Exception e) {
            BungeeCord.getInstance().getConsole().sendMessage(Strings.prefix + "§4Fehler beim Hochfahren des Bungeecord-Systems Server wird gestoppt!");
            BungeeCord.getInstance().stop();
            e.printStackTrace();
        }
    }

    public void onDisable() {
        instance = null;
    }

    public static Plugin getInstance() {
        return instance;
    }
}