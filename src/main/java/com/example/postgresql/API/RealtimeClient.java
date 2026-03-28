package com.example.postgresql.API;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class RealtimeClient {

    private static final String REALTIME_URL =
            SupabaseClient.SUPABASE_URL.replace("https://", "wss://")
            + "/realtime/v1/websocket?apikey=" + SupabaseClient.SUPABASE_ANON_KEY
            + "&vsn=1.0.0";

    private static final String CHANNEL_TOPIC = "realtime:public:messages";

    private final Gson gson = new Gson();
    private WebSocket webSocket;
    private Consumer<JsonObject> onNewMessage;

    private final AtomicBoolean connected  = new AtomicBoolean(false);
    private final AtomicBoolean shouldStop = new AtomicBoolean(false);

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "realtime-heartbeat");
                t.setDaemon(true);
                return t;
            });

    private ScheduledFuture<?> heartbeatTask;
    private int refCounter = 0;

    

    public void connect(Consumer<JsonObject> onNewMessage) {
        this.onNewMessage = onNewMessage;
        shouldStop.set(false);
        doConnect();
    }

    public void disconnect() {
        shouldStop.set(true);
        connected.set(false);
        stopHeartbeat();
        if (webSocket != null) {
            try { webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "bye"); }
            catch (Exception ignored) {}
        }
    }

    

    private void doConnect() {
        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create(REALTIME_URL), new WebSocket.Listener() {

                    private final StringBuilder buffer = new StringBuilder();

                    @Override
                    public void onOpen(WebSocket ws) {
                        webSocket = ws;
                        connected.set(true);
                        ws.request(1);
                        sendHeartbeat();
                        startHeartbeatLoop();
                        subscribeToMessages();
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                        buffer.append(data);
                        if (last) {
                            String text = buffer.toString();
                            buffer.setLength(0);
                            handleMessage(text);
                        }
                        ws.request(1);
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket ws, int code, String reason) {
                        onDisconnected();
                        return null;
                    }

                    @Override
                    public void onError(WebSocket ws, Throwable error) {
                        System.err.println("[Realtime] error: " + error.getMessage());
                        onDisconnected();
                    }
                });
    }

    private void onDisconnected() {
        connected.set(false);
        stopHeartbeat();
        if (!shouldStop.get()) {
            scheduler.schedule(this::doConnect, 5, TimeUnit.SECONDS);
        }
    }

    

    private void sendHeartbeat() {
        JsonObject hb = new JsonObject();
        hb.addProperty("topic", "phoenix");
        hb.addProperty("event", "heartbeat");
        hb.add("payload", new JsonObject());
        hb.addProperty("ref", String.valueOf(++refCounter));
        sendJson(hb);
    }

    private void startHeartbeatLoop() {
        stopHeartbeat();
        heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
            if (connected.get()) sendHeartbeat();
        }, 25, 25, TimeUnit.SECONDS);
    }

    private void stopHeartbeat() {
        if (heartbeatTask != null && !heartbeatTask.isCancelled()) {
            heartbeatTask.cancel(false);
        }
    }

    

    private void subscribeToMessages() {
        
        JsonObject pgChange = new JsonObject();
        pgChange.addProperty("event",  "INSERT");
        pgChange.addProperty("schema", "public");
        pgChange.addProperty("table",  "messages");

        com.google.gson.JsonArray pgChanges = new com.google.gson.JsonArray();
        pgChanges.add(pgChange);

        JsonObject broadcastCfg = new JsonObject();
        broadcastCfg.addProperty("ack",  false);
        broadcastCfg.addProperty("self", false);

        JsonObject presenceCfg = new JsonObject();
        presenceCfg.addProperty("key", "");

        JsonObject config = new JsonObject();
        config.add("broadcast",       broadcastCfg);
        config.add("presence",        presenceCfg);
        config.add("postgres_changes", pgChanges);

        JsonObject payload = new JsonObject();
        payload.add("config", config);

        JsonObject sub = new JsonObject();
        sub.addProperty("topic", CHANNEL_TOPIC);
        sub.addProperty("event", "phx_join");
        sub.add("payload", payload);
        sub.addProperty("ref", String.valueOf(++refCounter));

        sendJson(sub);
    }

    

    private void handleMessage(String raw) {
        try {
            JsonObject msg = gson.fromJson(raw, JsonObject.class);
            if (msg == null) return;

            String event = msg.has("event") ? msg.get("event").getAsString() : "";

            
            if ("postgres_changes".equals(event) && msg.has("payload")) {
                JsonObject payload = msg.getAsJsonObject("payload");
                if (payload.has("data")) {
                    JsonObject data = payload.getAsJsonObject("data");
                    if (data.has("record")) {
                        if (onNewMessage != null) onNewMessage.accept(data.getAsJsonObject("record"));
                        return;
                    }
                }
            }

            
            if (msg.has("payload")) {
                JsonObject payload = msg.getAsJsonObject("payload");
                if (payload.has("type") && "INSERT".equals(payload.get("type").getAsString())
                        && payload.has("record")) {
                    if (onNewMessage != null) onNewMessage.accept(payload.getAsJsonObject("record"));
                }
            }

        } catch (Exception ignored) {}
    }

    

    private void sendJson(JsonObject obj) {
        if (webSocket != null && connected.get()) {
            try { webSocket.sendText(gson.toJson(obj), true); }
            catch (Exception e) { System.err.println("[Realtime] send error: " + e.getMessage()); }
        }
    }
}
