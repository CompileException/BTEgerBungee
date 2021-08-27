package de.lucas.btegermany.bteteleport.bungee;

/*
 * Copyright (c) BTE Germany 2021.
 * Plugin by Lucas L. - CompileException.
 */

import de.lucas.btegermany.bteteleport.bungee.commands.bta;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class AutoCompletion implements Listener {
    @EventHandler(priority = 32)
    public void onTab(TabCompleteEvent e) {
        String[] args = e.getCursor().toLowerCase().split(" ");
        if (args.length >= 1)
            if (args[0].startsWith("/"))
                if (bta.getCmds().contains(args[0].replaceAll("/", "")) && e
                        .getCursor().contains(" ")) {
                    e.getSuggestions().clear();
                    ProxiedPlayer p = (ProxiedPlayer)e.getSender();
                    if (args.length == 1) {
                        for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers())
                            e.getSuggestions().add(all.getName());
                        return;
                    }
                    if (args.length == 2 && getSpace(e.getCursor()) == 1) {
                        addSuggestions(e, args);
                        return;
                    }
                    if (args.length == 2) {
                        for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers())
                            e.getSuggestions().add(all.getName());
                        return;
                    }
                    if (args.length == 3 && getSpace(e.getCursor()) == 2)
                        addSuggestions(e, args);
                }
    }

    private void addSuggestions(TabCompleteEvent e, String[] args) {
        String check = args[args.length - 1];
        for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
            if (all.getName().toLowerCase().startsWith(check))
                e.getSuggestions().add(all.getName());
        }
    }

    public static int getSpace(String s) {
        int space = 0;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i)))
                space++;
        }
        return space;
    }
}
