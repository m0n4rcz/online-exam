package client.net;

import shared.OpCodes;
import shared.Packet;
import shared.PacketCodec;
import shared.dtos.*;

import javax.swing.SwingUtilities;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * High-performance TCP Socket Client managing connection lifecycle,
 * packet encoding/decoding, asynchronous request-response correlation,
 * and push notification dispatch to UI components.
 */
public class NetworkClient {

    private static final Logger LOGGER = Logger.getLogger(NetworkClient.class.getName());

    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private Thread readerThread;

    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final List<ServerEventListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<Short, CompletableFuture<Packet>> pendingRequests = new ConcurrentHashMap<>();

    private UserProfileDTO currentUser;
    private String authToken;
    private String currentRoomCode;
    private String serverHost = "127.0.0.1";
    private int serverPort = 8888;

    public NetworkClient() {}

    public synchronized void connect(String host, int port) throws Exception {
        if (connected.get()) {
            disconnect();
        }

        this.serverHost = host;
        this.serverPort = port;

        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000);
        socket.setTcpNoDelay(true);

        in = socket.getInputStream();
        out = new BufferedOutputStream(socket.getOutputStream());
        connected.set(true);

        readerThread = new Thread(this::readLoop, "NetworkClient-Reader");
        readerThread.setDaemon(true);
        readerThread.start();

        LOGGER.info(String.format("Connected to server at %s:%d", host, port));
    }

    public synchronized void disconnect() {
        if (!connected.getAndSet(false)) return;

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception ignored) {}

        currentUser = null;
        authToken = null;
        currentRoomCode = null;

        // Cancel all pending futures
        for (CompletableFuture<Packet> f : pendingRequests.values()) {
            f.completeExceptionally(new Exception("Connection closed"));
        }
        pendingRequests.clear();

        dispatchToListeners(l -> l.onConnectionLost("Disconnected from server"));
        LOGGER.info("Disconnected from server.");
    }

    public boolean isConnected() {
        return connected.get() && socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Sends a packet asynchronously and returns a CompletableFuture resolving when the expected response arrives.
     */
    public CompletableFuture<Packet> sendRequest(Packet packet, short expectedResponseOpCode) {
        if (!isConnected()) {
            CompletableFuture<Packet> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalStateException("Not connected to server"));
            return failed;
        }

        CompletableFuture<Packet> future = new CompletableFuture<>();
        pendingRequests.put(expectedResponseOpCode, future);

        // Schedule timeout (15 seconds)
        CompletableFuture.delayedExecutor(15, TimeUnit.SECONDS).execute(() -> {
            if (pendingRequests.remove(expectedResponseOpCode, future)) {
                future.completeExceptionally(new TimeoutException("Request timed out for " + OpCodes.getName(expectedResponseOpCode)));
            }
        });

        try {
            synchronized (out) {
                PacketCodec.writePacket(out, packet);
            }
        } catch (Exception e) {
            pendingRequests.remove(expectedResponseOpCode, future);
            future.completeExceptionally(e);
            disconnect();
        }

        return future;
    }

    /**
     * Sends a fire-and-forget packet without awaiting a specific response.
     */
    public void sendPacket(Packet packet) {
        if (!isConnected()) return;
        try {
            synchronized (out) {
                PacketCodec.writePacket(out, packet);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send packet: " + e.getMessage(), e);
            disconnect();
        }
    }

    /**
     * Background packet reading loop.
     */
    private void readLoop() {
        try {
            while (connected.get() && !socket.isClosed()) {
                Packet packet = PacketCodec.readPacket(in);
                if (packet == null) {
                    LOGGER.info("Server closed connection (EOF).");
                    break;
                }
                handleIncomingPacket(packet);
            }
        } catch (Exception e) {
            if (connected.get()) {
                LOGGER.log(Level.WARNING, "Connection error: " + e.getMessage());
            }
        } finally {
            disconnect();
        }
    }

    private void handleIncomingPacket(Packet packet) {
        short op = packet.getOpCode();
        LOGGER.fine("Received: " + OpCodes.getName(op) + " [status=" + packet.getStatus() + "]");

        // Check if there is a pending request waiting for this response OpCode
        CompletableFuture<Packet> future = pendingRequests.remove(op);
        if (future != null) {
            future.complete(packet);
            return;
        }

        // If not a pending request future, process as server push event or broadcast
        switch (op) {
            case OpCodes.EVENT_ROOM_UPDATE -> {
                RoomUpdateEventDTO event = packet.getPayloadAs(RoomUpdateEventDTO.class);
                dispatchToListeners(l -> l.onRoomUpdate(event));
            }
            case OpCodes.EVENT_ROOM_START -> {
                int countdown = 3;
                try {
                    countdown = Integer.parseInt(packet.getPayloadAsString().replaceAll("[^0-9]", ""));
                } catch (Exception ignored) {}
                final int cd = countdown;
                dispatchToListeners(l -> l.onRoomStart(cd));
            }
            case OpCodes.EXAM_INIT_DATA -> {
                ExamInitDataDTO examData = packet.getPayloadAs(ExamInitDataDTO.class);
                dispatchToListeners(l -> l.onExamInit(examData));
            }
            case OpCodes.EVENT_EXAM_TICK -> {
                int remaining = 0;
                try {
                    remaining = Integer.parseInt(packet.getPayloadAsString().replaceAll("[^0-9]", ""));
                } catch (Exception ignored) {}
                final int rem = remaining;
                dispatchToListeners(l -> l.onExamTick(rem));
            }
            case OpCodes.EVENT_PARTICIPANT_SUBMITTED -> {
                String raw = packet.getPayloadAsString();
                String user = raw;
                int sub = 0, tot = 0;
                try {
                    // format might be "username (1/2 submitted)" or JSON
                    if (raw.contains(":")) {
                        Map<String, Object> map = shared.JsonUtil.parseMap(raw);
                        user = String.valueOf(map.getOrDefault("username", "A student"));
                        sub = Integer.parseInt(String.valueOf(map.getOrDefault("submittedCount", "1")));
                        tot = Integer.parseInt(String.valueOf(map.getOrDefault("totalCount", "1")));
                    }
                } catch (Exception ignored) {}
                final String u = user;
                final int s = sub, t = tot;
                dispatchToListeners(l -> l.onParticipantSubmitted(u, s, t));
            }
            case OpCodes.EVENT_EXAM_ENDED -> {
                String reason = packet.getPayloadAsString();
                dispatchToListeners(l -> l.onExamEnded(reason));
            }
            case OpCodes.EXAM_RESULT_RES -> {
                ExamResultDTO result = packet.getPayloadAs(ExamResultDTO.class);
                dispatchToListeners(l -> l.onExamResult(result));
            }
            case OpCodes.EVENT_ROOM_CHAT -> {
                ChatMessageDTO chat = packet.getPayloadAs(ChatMessageDTO.class);
                dispatchToListeners(l -> l.onRoomChat(chat));
            }
            case OpCodes.PONG -> {
                // Heartbeat response acknowledged
            }
            default -> {
                LOGGER.info("Uncaught packet: " + OpCodes.getName(op) + " -> " + packet.getPayloadAsString());
            }
        }
    }

    private void dispatchToListeners(java.util.function.Consumer<ServerEventListener> consumer) {
        SwingUtilities.invokeLater(() -> {
            for (ServerEventListener listener : listeners) {
                try {
                    consumer.accept(listener);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error in event listener: " + e.getMessage(), e);
                }
            }
        });
    }

    public void addListener(ServerEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(ServerEventListener listener) {
        listeners.remove(listener);
    }

    // ==========================================
    // GETTERS & SETTERS FOR SESSION STATE
    // ==========================================

    public UserProfileDTO getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(UserProfileDTO user) {
        this.currentUser = user;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getCurrentRoomCode() {
        return currentRoomCode;
    }

    public void setCurrentRoomCode(String roomCode) {
        this.currentRoomCode = roomCode;
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }
}
