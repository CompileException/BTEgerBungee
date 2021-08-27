package de.lucas.btegermany.bteteleport.bungee;

/*
 * Copyright (c) 2021.
 * Plugin by Lucas L. - CompileException.
 */

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import de.lucas.btegermany.main.Core;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BungeeSend {
    public static void teleport(ProxiedPlayer from, ProxiedPlayer to) {
        ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteArrayOut);
        try {
            out.writeUTF("Teleport");
            out.writeUTF(from.getName());
            out.writeUTF(to.getName());
            from.getServer().getInfo()
                    .sendData(Core.oldChannel, byteArrayOut.toByteArray());
            from.getServer().getInfo()
                    .sendData(Core.channel, byteArrayOut.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
