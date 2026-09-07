package id.mas.agent;

import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Bounded stdio client for the official Codex App Server protocol.
 * The reader never executes tools: the application's task worker handles server requests.
 */
final class CodexRpc implements AutoCloseable {
    private static final int MAX_PACKET = 1048576;
    private final InputStream input;
    private final OutputStream output;
    private final AtomicLong ids = new AtomicLong();
    private final Map<String, CompletableFuture<JSONObject>> pending = new ConcurrentHashMap<>();
    private final BlockingQueue<JSONObject> events = new ArrayBlockingQueue<>(64);
    private volatile IOException failure;
    private volatile boolean closed;

    CodexRpc(InputStream input, OutputStream output) {
        this.input = new BufferedInputStream(input); this.output = output;
        Thread reader = new Thread(this::receive, "codex-json-rpc");
        reader.setDaemon(true); reader.start();
    }

    JSONObject request(String method, JSONObject params, long timeoutSeconds) throws Exception {
        if (timeoutSeconds < 1 || timeoutSeconds > 180) throw new IllegalArgumentException("Invalid RPC deadline");
        if (pending.size() >= 16) throw new IOException("Terlalu banyak permintaan ke agen lokal.");
        long id = ids.incrementAndGet();
        CompletableFuture<JSONObject> reply = new CompletableFuture<>();
        pending.put(Long.toString(id), reply);
        try {
            send(new JSONObject().put("id", id).put("method", method).put("params", params));
            JSONObject message = reply.get(timeoutSeconds, TimeUnit.SECONDS);
            if (message.has("error")) throw new IOException("Agen lokal menolak " + method + " (kode "
                + message.getJSONObject("error").optInt("code") + ").");
            JSONObject result = message.optJSONObject("result");
            if (result == null) throw new IOException("Respons agen lokal tidak valid.");
            return result;
        } catch (TimeoutException e) {
            throw new IOException("Agen lokal melewati batas waktu. Periksa status sebelum mengulangi tugas.");
        } catch (ExecutionException e) {
            throw new IOException("Proses agen lokal terputus.");
        } finally { pending.remove(Long.toString(id)); }
    }

    void notify(String method) throws Exception { send(new JSONObject().put("method", method)); }
    void respond(Object requestId, JSONObject result) throws Exception {
        send(new JSONObject().put("id", requestId).put("result", result));
    }
    void reject(Object requestId) throws Exception {
        send(new JSONObject().put("id", requestId).put("error", new JSONObject()
            .put("code", -32601).put("message", "Operation is not exposed by the MikroTik application.")));
    }

    JSONObject nextEvent(long timeoutMillis) throws Exception {
        if (timeoutMillis < 0 || timeoutMillis > 180000) throw new IllegalArgumentException("Invalid event deadline");
        checkOpen();
        JSONObject event = events.poll(timeoutMillis, TimeUnit.MILLISECONDS);
        if (event == null) checkOpen();
        return event;
    }

    private synchronized void send(JSONObject message) throws IOException {
        checkOpen();
        byte[] bytes = (message.toString() + "\n").getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_PACKET) throw new IOException("Data tugas melebihi batas agen lokal.");
        output.write(bytes); output.flush();
    }

    private void checkOpen() throws IOException {
        if (failure != null) throw new IOException("Proses agen lokal terputus.");
        if (closed) throw new IOException("Proses agen lokal sudah ditutup.");
    }

    static byte[] readPacket(InputStream input) throws IOException {
        ByteArrayOutputStream packet = new ByteArrayOutputStream();
        for (;;) {
            int next = input.read();
            if (next == -1) {
                if (packet.size() == 0) return null;
                throw new IOException("Truncated JSON-RPC packet");
            }
            if (next == '\n') return packet.toByteArray();
            if (packet.size() >= MAX_PACKET) throw new IOException("Oversized JSON-RPC packet");
            packet.write(next);
        }
    }

    private void receive() {
        try {
            byte[] bytes;
            while (!closed && (bytes = readPacket(input)) != null) {
                if (bytes.length == 0) continue;
                JSONObject message = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
                if (message.has("id") && !message.has("method")) {
                    CompletableFuture<JSONObject> reply = pending.get(message.get("id").toString());
                    if (reply != null) reply.complete(message);
                    continue;
                }
                String method = message.optString("method");
                // Completed items contain the full assistant text. Drop token deltas and telemetry.
                boolean wanted = message.has("id") || method.equals("item/completed") || method.equals("turn/completed")
                    || method.equals("account/login/completed") || method.equals("account/updated") || method.equals("error");
                if (wanted && !events.offer(message, 15, TimeUnit.SECONDS)) throw new IOException("Event queue full");
            }
            if (!closed) fail(new IOException("Native process EOF"));
        } catch (Exception error) {
            if (!closed) fail(new IOException("Invalid or interrupted native protocol"));
        }
    }

    private void fail(IOException error) {
        failure = error;
        for (CompletableFuture<JSONObject> request : pending.values()) request.completeExceptionally(error);
    }

    @Override public void close() {
        closed = true;
        fail(new IOException("Closed"));
        try { output.close(); } catch (IOException ignored) { }
        try { input.close(); } catch (IOException ignored) { }
        events.clear();
    }
}
