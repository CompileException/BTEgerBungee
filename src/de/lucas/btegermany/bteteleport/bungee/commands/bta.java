package de.lucas.btegermany.bteteleport.bungee.commands;

/*
 * Copyright (c) BTE Germany 2021.
 * Plugin by Lucas L. - CompileException.
 */

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.lucas.btegermany.bteteleport.bungee.BungeeSend;
import de.lucas.btegermany.datasave.Strings;
import de.lucas.btegermany.main.Core;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import org.bukkit.entity.Player;

public class bta extends Command {
    private static List<String> cmds = Arrays.asList(new String[] { "btr", "bungeeteleportrequest" });

    public bta(String name) {
        super(name);
    }

    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(ChatColor.RED + "Command only for player !");
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /bta <player> [<player>]");
            return;
        }
        if (args.length == 1) {
            ProxiedPlayer from = (ProxiedPlayer)sender;
            ProxiedPlayer to = ProxyServer.getInstance().getPlayer(args[0]);
            if (args[0] != null && to == null) {
                from.sendMessage(ChatColor.RED + "This player is not online !");
                return;
            }
            teleport(from, to);
            from.sendMessage(ChatColor.GREEN + "You have been teleported to " + ChatColor.DARK_GREEN + "" + ChatColor.BOLD + to.getName());
            return;
        }
        if (args.length == 2) {
            ProxiedPlayer from = ProxyServer.getInstance().getPlayer(args[0]);
            ProxiedPlayer to = ProxyServer.getInstance().getPlayer(args[1]);
            if (from == null) {
                sender.sendMessage(ChatColor.RED + args[0] + " is not online !");
                return;
            }
            if (to == null) {
                sender.sendMessage(ChatColor.RED + args[1] + " is not online !");
                return;
            }
            teleport(from, to);
            sender.sendMessage(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + from.getName() + ChatColor.RESET + "" + ChatColor.GREEN + " has been teleported to " + ChatColor.DARK_GREEN + "" + ChatColor.BOLD + to.getName() + ".");
        }
    }

    public static void teleport(ProxiedPlayer from, ProxiedPlayer to) {
        if (from.getServer().getInfo() != to.getServer().getInfo())
            from.connect(to.getServer().getInfo());
        ScheduledTask schedule = ProxyServer.getInstance().getScheduler().schedule(Core.getInstance(), () -> BungeeSend.teleport(from, to), 1L, TimeUnit.SECONDS);
    }

    public static List<String> getCmds() {
        return cmds;
    }
}
