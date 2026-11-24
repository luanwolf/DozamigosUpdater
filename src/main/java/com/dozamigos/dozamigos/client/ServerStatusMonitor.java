package com.dozamigos.dozamigos.client;

import com.dozamigos.dozamigos.DozamigosMod;
import com.dozamigos.dozamigos.config.DozamigosConfig;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Server List Ping (1.7+) nos enderecos do Dozamigos.
 * TCP puro mente no Playit (tunel aceita connect com o MC offline) — precisa do status.
 * ONLINE se qualquer host:porta devolver JSON de status; banner so em OFFLINE.
 */
public final class ServerStatusMonitor {
    private static final int PROTOCOL_1_20_1 = 763;

    public enum Status {
        UNKNOWN,
        ONLINE,
        OFFLINE
    }

    public record HostPort(String host, int port) {}

    private static final AtomicReference<Status> STATUS = new AtomicReference<>(Status.UNKNOWN);
    private static final AtomicBoolean PROBING = new AtomicBoolean(false);
    private static volatile long lastProbeAtMs;

    private ServerStatusMonitor() {}

    public static Status status() {
        return STATUS.get();
    }

    /** Chamar no render do title: re-probe no intervalo sem criar thread a cada frame. */
    public static void ensureProbing() {
        if (!DozamigosConfig.SHOW_MAINTENANCE.get()) {
            return;
        }
        long now = System.currentTimeMillis();
        long intervalMs = DozamigosConfig.SERVER_PROBE_INTERVAL_SEC.get() * 1000L;
        if (STATUS.get() != Status.UNKNOWN && now - lastProbeAtMs < intervalMs) {
            return;
        }
        if (!PROBING.compareAndSet(false, true)) {
            return;
        }
        lastProbeAtMs = now;
        Thread t = new Thread(ServerStatusMonitor::probe, "dozamigos-server-probe");
        t.setDaemon(true);
        t.start();
    }

    private static void probe() {
        try {
            List<HostPort> targets = parseAddresses(DozamigosConfig.SERVER_ADDRESSES.get());
            boolean anyUp = false;
            for (HostPort hp : targets) {
                if (statusPing(hp.host(), hp.port(), DozamigosConfig.SERVER_PROBE_TIMEOUT_MS.get())) {
                    anyUp = true;
                    break;
                }
            }
            Status next = anyUp ? Status.ONLINE : Status.OFFLINE;
            Status prev = STATUS.getAndSet(next);
            if (prev != next) {
                DozamigosMod.LOGGER.info("Servidor Dozamigos: {} -> {}", prev, next);
            }
        } finally {
            PROBING.set(false);
        }
    }

    /** Handshake + status request; true so se vier packet id 0 com JSON. */
    static boolean statusPing(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);
            OutputStream rawOut = socket.getOutputStream();
            InputStream rawIn = socket.getInputStream();

            ByteArrayOutputStream handshake = new ByteArrayOutputStream();
            DataOutputStream hs = new DataOutputStream(handshake);
            writeVarInt(hs, 0);
            writeVarInt(hs, PROTOCOL_1_20_1);
            writeString(hs, host);
            hs.writeShort(port & 0xFFFF);
            writeVarInt(hs, 1);
            writePacket(rawOut, handshake.toByteArray());

            writePacket(rawOut, new byte[] {0});
            rawOut.flush();

            DataInputStream in = new DataInputStream(rawIn);
            int packetLen = readVarInt(in);
            if (packetLen <= 0) {
                return false;
            }
            int packetId = readVarInt(in);
            if (packetId != 0) {
                return false;
            }
            String json = readString(in);
            return json != null && !json.isBlank() && json.charAt(0) == '{';
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void writePacket(OutputStream out, byte[] payload) throws IOException {
        writeVarInt(out, payload.length);
        out.write(payload);
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int len = readVarInt(in);
        if (len < 0 || len > 32767) {
            return null;
        }
        byte[] buf = in.readNBytes(len);
        if (buf.length != len) {
            return null;
        }
        return new String(buf, StandardCharsets.UTF_8);
    }

    static void writeVarInt(OutputStream out, int value) throws IOException {
        int v = value;
        while ((v & ~0x7F) != 0) {
            out.write((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        out.write(v);
    }

    static int readVarInt(InputStream in) throws IOException {
        int value = 0;
        int position = 0;
        while (true) {
            int b = in.read();
            if (b < 0) {
                throw new IOException("eof varint");
            }
            value |= (b & 0x7F) << position;
            if ((b & 0x80) == 0) {
                return value;
            }
            position += 7;
            if (position >= 32) {
                throw new IOException("varint too big");
            }
        }
    }

    /** "host:port, host2" — porta default 25565. */
    public static List<HostPort> parseAddresses(String raw) {
        List<HostPort> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : raw.split(",")) {
            String token = part.trim();
            if (token.isEmpty()) {
                continue;
            }
            int colon = token.lastIndexOf(':');
            if (colon > 0 && colon < token.length() - 1) {
                String host = token.substring(0, colon).trim();
                String portStr = token.substring(colon + 1).trim();
                try {
                    int port = Integer.parseInt(portStr);
                    if (!host.isEmpty() && port > 0 && port <= 65535) {
                        out.add(new HostPort(host, port));
                        continue;
                    }
                } catch (NumberFormatException ignored) {
                    // cai no default abaixo
                }
            }
            out.add(new HostPort(token, 25565));
        }
        return out;
    }
}
