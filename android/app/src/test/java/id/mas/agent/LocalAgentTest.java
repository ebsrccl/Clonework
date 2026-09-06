package id.mas.agent;

import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class LocalAgentTest {
    static class Memory implements LocalAgent.Store {
        String data; boolean rejectApplying;
        Memory() throws Exception { data = profile().put("plans", new JSONArray()).toString(); }
        public JSONObject read() throws Exception { return data == null ? null : new JSONObject(data); }
        public void save(JSONObject state) throws Exception {
            if (rejectApplying && state.toString().contains("\"applying\"")) throw new IOException("disk full");
            data = state.toString();
        }
        public void clear() { data = null; }
    }
    static JSONObject profile() throws Exception {
        return new JSONObject().put("router", new JSONObject().put("name", "Test router").put("host", "127.0.0.1").put("port", 8728)
            .put("tls", false).put("certificate_sha256", "").put("username", "test-user").put("password", "router-secret-test"))
            .put("openai_key", "api-secret-test").put("model", "gpt-5-mini");
    }
    static JSONObject args() throws Exception { return new JSONObject().put("queue_id", "*A").put("upload_mbps", 5).put("download_mbps", 10); }
    static class FakeRouter implements LocalAgent.Router {
        int writes; boolean failWrite, changeTarget;
        JSONObject row;
        FakeRouter() throws Exception { row = new JSONObject().put(".id", "*A").put("name", "Client 1").put("target", "192.168.88.10/32")
            .put("max-limit", "1M/2M").put("disabled", "false").put("dynamic", "false"); }
        public JSONArray read(String path, String[] fields) throws Exception { return new JSONArray().put(new JSONObject(row.toString())); }
        public void setQueue(String id, String limit) throws Exception { writes++; if (failWrite) throw new IOException("uncertain"); row.put("max-limit", limit); if (changeTarget) row.put("target", "192.168.88.11/32"); }
    }
    static LocalAgent engine(Memory memory, FakeRouter router) throws Exception { return new LocalAgent(memory, router, (key, payload) -> { throw new AssertionError("Unexpected OpenAI request"); }); }
    static String proposal(LocalAgent agent) throws Exception { return agent.execute("prepare_queue_limit", args()).getJSONObject("plan").getString("id"); }
    @Test public void queueProposalPersistsAndAppliesOnceAcrossRestart() throws Exception {
        Memory memory = new Memory(); FakeRouter router = new FakeRouter(); LocalAgent a = engine(memory, router);
        String id = proposal(a); assertEquals(0, router.writes);
        LocalAgent b = engine(memory, router); assertEquals("verified", b.apply(id).getString("status")); assertEquals(1, router.writes);
        engine(memory, router).apply(id); assertEquals(1, router.writes);
    }
    @Test public void staleSnapshotRejectsWrite() throws Exception {
        Memory memory = new Memory(); FakeRouter router = new FakeRouter(); LocalAgent a = engine(memory, router);
        String id = proposal(a); router.row.put("target", "different");
        assertEquals("stale", a.apply(id).getString("status")); assertEquals(0, router.writes);
    }
    @Test public void uncertainWriteIsNeverRetried() throws Exception {
        Memory memory = new Memory(); FakeRouter router = new FakeRouter(); router.failWrite = true; LocalAgent a = engine(memory, router);
        String id = proposal(a); assertEquals("unknown", a.apply(id).getString("status"));
        engine(memory, router).apply(id); assertEquals(1, router.writes);
    }
    @Test public void failedPersistencePreventsNetworkMutation() throws Exception {
        Memory memory = new Memory(); FakeRouter router = new FakeRouter(); LocalAgent a = engine(memory, router);
        String id = proposal(a); memory.rejectApplying = true;
        assertThrows(IOException.class, () -> a.apply(id)); assertEquals(0, router.writes);
    }
    @Test public void expiredProposalCannotWrite() throws Exception {
        Memory memory = new Memory(); FakeRouter router = new FakeRouter(); String id = proposal(engine(memory, router));
        JSONObject state = memory.read(); state.getJSONArray("plans").getJSONObject(0).put("expires_at", 0); memory.save(state);
        assertEquals("expired", engine(memory, router).apply(id).getString("status")); assertEquals(0, router.writes);
    }
    @Test public void changedTargetDuringWriteIsUnverified() throws Exception {
        Memory memory = new Memory(); FakeRouter router = new FakeRouter(); router.changeTarget = true; LocalAgent a = engine(memory, router);
        assertEquals("unverified", a.apply(proposal(a)).getString("status"));
    }
    @Test public void applyingStateSurvivesProcessDeathWithoutReplay() throws Exception {
        Memory memory = new Memory(); FakeRouter router = new FakeRouter(); String id = proposal(engine(memory, router));
        JSONObject state = memory.read(); state.getJSONArray("plans").getJSONObject(0).put("status", "applying"); memory.save(state);
        assertEquals("applying", engine(memory, router).apply(id).getString("status")); assertEquals(0, router.writes);
    }
    @Test public void strictToolsRejectUnknownAndExtraAndStringRates() throws Exception {
        assertThrows(IOException.class, () -> LocalAgent.validateTool("shell", new JSONObject()));
        assertThrows(IOException.class, () -> LocalAgent.validateTool("list_interfaces", new JSONObject().put("password", "x")));
        assertThrows(IOException.class, () -> LocalAgent.validateTool("prepare_queue_limit", args().put("upload_mbps", "5")));
        assertThrows(IOException.class, () -> LocalAgent.validateTool("prepare_queue_limit", args().put("download_mbps", 0)));
        assertEquals("1000000/2000000", LocalAgent.normalizeLimit("1M/2M"));
    }
    static JSONObject done(String text) throws Exception {
        return new JSONObject().put("id", "resp_test").put("status", "completed").put("output", new JSONArray().put(new JSONObject().put("type", "message")
            .put("role", "assistant").put("content", new JSONArray().put(new JSONObject().put("type", "output_text").put("text", text)))));
    }
    @Test public void responsesLoopForwardsReasoningAndRedactsCredentials() throws Exception {
        Memory memory = new Memory(); FakeRouter router = new FakeRouter(); router.row.put("name", "router-secret-test"); int[] count = {0};
        LocalAgent agent = new LocalAgent(memory, router, (key, payload) -> {
            assertEquals("api-secret-test", key);
            assertFalse(payload.toString().contains("router-secret-test")); assertFalse(payload.toString().contains("api-secret-test"));
            assertFalse(payload.getBoolean("store")); assertFalse(payload.has("router")); assertFalse(payload.has("gateway"));
            if (count[0]++ == 0) return new JSONObject().put("status", "completed").put("output", new JSONArray()
                .put(new JSONObject().put("type", "reasoning").put("id", "rs_test").put("summary", new JSONArray()).put("encrypted_content", "cipher-test"))
                .put(new JSONObject().put("type", "function_call").put("call_id", "call_test").put("name", "list_simple_queues").put("arguments", "{}")));
            assertTrue(payload.getJSONArray("input").toString().contains("cipher-test"));
            assertTrue(payload.getJSONArray("input").toString().contains("function_call_output"));
            return done("Ada satu queue.");
        });
        JSONObject result = agent.chat("Cek queue. api-secret-test router-secret-test");
        assertEquals(2, count[0]); assertEquals(0, router.writes); assertEquals(2, result.getJSONArray("messages").length());
        assertFalse(result.toString().contains("api-secret-test"));
        assertEquals(2, engine(memory, router).snapshot().getJSONArray("messages").length());
    }
    @Test public void modelCannotApplyOrRunArbitraryCommands() throws Exception {
        Memory memory = new Memory(); FakeRouter router = new FakeRouter(); int[] n = {0};
        LocalAgent a = new LocalAgent(memory, router, (key, payload) -> {
            if (n[0]++ == 0) return new JSONObject().put("output", new JSONArray().put(new JSONObject().put("type", "function_call").put("call_id", "bad")
                .put("name", "setQueue").put("arguments", "{\"command\":\"/system/reboot\"}")));
            assertTrue(payload.toString().contains("parameter ditolak")); return done("Fungsi tidak tersedia.");
        });
        a.chat("Jalankan fungsi"); assertEquals(0, router.writes);
    }
    @Test public void snapshotCannotExposeKeyOrModifyStoredProposal() throws Exception {
        Memory memory = new Memory(); LocalAgent a = engine(memory, new FakeRouter()); proposal(a);
        JSONObject snapshot = a.snapshot(); assertFalse(snapshot.has("router")); assertFalse(snapshot.has("openai_key"));
        snapshot.getJSONArray("plans").getJSONObject(0).put("status", "verified");
        assertEquals("pending", a.snapshot().getJSONArray("plans").getJSONObject(0).getString("status"));
    }
    @Test public void configRejectsInvalidHostAndPin() throws Exception {
        JSONObject config = profile(); config.getJSONObject("router").put("host", "https://router/path"); assertThrows(IOException.class, () -> LocalAgent.validateConfig(config));
        JSONObject other = profile(); other.getJSONObject("router").put("certificate_sha256", "00"); assertThrows(IOException.class, () -> LocalAgent.validateConfig(other));
    }
    @Test public void directApiLoginAndReadFilterUnrequestedSecrets() throws Exception {
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            ExecutorService worker = Executors.newSingleThreadExecutor();
            Future<?> server = worker.submit(() -> { try (Socket socket = listener.accept()) {
                List<String> login = RouterApi.readSentence(socket.getInputStream()); assertEquals("/login", login.get(0)); assertTrue(login.contains("=name=test-user"));
                RouterApi.writeSentence(socket.getOutputStream(), "!done");
                List<String> request = RouterApi.readSentence(socket.getInputStream()); assertEquals("/system/identity/print", request.get(0)); assertTrue(request.contains("=.proplist=name"));
                RouterApi.writeSentence(socket.getOutputStream(), "!re", "=name=Router uji", "=password=never-return-this"); RouterApi.writeSentence(socket.getOutputStream(), "!done");
            } catch (Exception e) { throw new RuntimeException(e); } });
            try {
                JSONObject router = profile().getJSONObject("router").put("port", listener.getLocalPort());
                JSONArray rows = new RouterApi(router).read("/system/identity", new String[]{"name"});
                assertEquals("Router uji", rows.getJSONObject(0).getString("name")); assertFalse(rows.toString().contains("password")); server.get(5, TimeUnit.SECONDS);
            } finally { worker.shutdownNow(); }
        }
    }
    @Test public void plaintextConnectionToPublicAddressIsRejectedBeforeConnect() throws Exception {
        JSONObject config = profile().getJSONObject("router").put("host", "203.0.113.1");
        IOException error = assertThrows(IOException.class, () -> new RouterApi(config).read("/system/identity", new String[]{"name"}));
        assertTrue(error.getMessage().contains("API-SSL"));
    }
    @Test public void lengthVectorsAndFragmentedUtf8RoundTrip() throws Exception {
        long[] values = {0, 127, 128, 16383, 16384, 2097151, 2097152, 268435455, 268435456, 4294967295L};
        int[][] vectors = {{0},{127},{128,128},{191,255},{192,64,0},{223,255,255},{224,32,0,0},{239,255,255,255},{240,16,0,0,0},{240,255,255,255,255}};
        for (int i = 0; i < values.length; i++) { ByteArrayOutputStream out = new ByteArrayOutputStream(); RouterApi.writeLength(out, values[i]); byte[] expected = new byte[vectors[i].length]; for(int j=0;j<expected.length;j++)expected[j]=(byte)vectors[i][j]; assertArrayEquals(expected,out.toByteArray()); assertEquals(values[i], RouterApi.readLength(new ByteArrayInputStream(out.toByteArray()))); }
        ByteArrayOutputStream out = new ByteArrayOutputStream(); RouterApi.writeSentence(out, "!re", "=name=Jaringan é 🇮🇩", "=value=a=b");
        InputStream fragmented = new ByteArrayInputStream(out.toByteArray()) { @Override public synchronized int read(byte[] b, int off, int len) { return super.read(b, off, Math.min(1, len)); } };
        assertEquals(Arrays.asList("!re", "=name=Jaringan é 🇮🇩", "=value=a=b"), RouterApi.readSentence(fragmented));
        assertThrows(IOException.class, () -> RouterApi.readLength(new ByteArrayInputStream(new byte[]{(byte)0xff})));
        assertThrows(IOException.class, () -> RouterApi.readSentence(new ByteArrayInputStream(new byte[]{(byte)0xf0, 127, -1, -1, -1})));
    }
}
