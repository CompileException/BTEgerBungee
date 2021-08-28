package de.lucas.btegermany.bteteleport.bukkit;

/*
 * Copyright (c) BTE Germany 2021.
 * Plugin by Lucas L. - CompileException.
 */

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.ArrayList;

import de.lucas.btegermany.datasave.Strings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class BukkitReceive extends JavaPlugin implements PluginMessageListener, Listener {
    private static Plugin instance;

    private static String channel = "bteteleport:main";

    public void onEnable() {
        try {
            Bukkit.getConsoleSender().sendMessage(Strings.prefix + "§6Spigot-System fährt hoch ......");
            instance = (Plugin)this;
            channel = "BTETeleport";
            Bukkit.getMessenger().registerOutgoingPluginChannel((Plugin)this, channel);
            Bukkit.getMessenger().registerIncomingPluginChannel((Plugin)this, channel, this);
            Bukkit.getServer().getPluginManager().registerEvents(this, (Plugin)this);

            Bukkit.getConsoleSender().sendMessage(Strings.prefix + " ...... §aSpigot-System Hochgefahren");
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(Strings.prefix + "§4Fehler beim Hochfahren des Spigot-Systems Server wird gestoppt!");
            Bukkit.shutdown();
            e.printStackTrace();
        }
    }

    public void onDisable() {
        instance = null;
    }

    public static Plugin getInstance() {
        return instance;
    }

    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(BukkitReceive.channel))
            return;
        String action = null;
        ArrayList<String> received = new ArrayList<>();
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
        try {
            action = in.readUTF();
            while (in.available() > 0)
                received.add(in.readUTF());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (action == null)
            return;
        if (action.equalsIgnoreCase("teleport")) {
            Player from = Bukkit.getServer().getPlayer(received.get(0));
            Player to = Bukkit.getServer().getPlayer(received.get(1));
            from.teleport((Entity)to);
        }
    }
}
