package com.dozamigos.dozamigos.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

/** ponytail: um check assert-based do parse + varint; sem framework. */
public final class ServerAddressSelfCheck {
    public static void main(String[] args) throws Exception {
        List<ServerStatusMonitor.HostPort> both = ServerStatusMonitor.parseAddresses(
                "signaling-usability.tun.ply.gg:25565, 131.72.108.139:25565");
        assert both.size() == 2 : both;
        assert both.get(0).host().equals("signaling-usability.tun.ply.gg");
        assert both.get(0).port() == 25565;
        assert both.get(1).host().equals("131.72.108.139");
        assert both.get(1).port() == 25565;

        List<ServerStatusMonitor.HostPort> noPort = ServerStatusMonitor.parseAddresses("example.com");
        assert noPort.size() == 1 && noPort.get(0).port() == 25565 : noPort;

        List<ServerStatusMonitor.HostPort> empty = ServerStatusMonitor.parseAddresses(" , ");
        assert empty.isEmpty() : empty;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ServerStatusMonitor.writeVarInt(out, 0);
        ServerStatusMonitor.writeVarInt(out, 763);
        ServerStatusMonitor.writeVarInt(out, 300);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        assert ServerStatusMonitor.readVarInt(in) == 0;
        assert ServerStatusMonitor.readVarInt(in) == 763;
        assert ServerStatusMonitor.readVarInt(in) == 300;

        System.out.println("ServerAddressSelfCheck OK");
    }

    private ServerAddressSelfCheck() {}
}
