package id.mas.agent;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class CodexRpcTest {
    @Test public void serverRequestsStaySeparateFromRpcResponses() throws Exception {
        PipedInputStream clientInput = new PipedInputStream(8192);
        PipedOutputStream serverOutput = new PipedOutputStream(clientInput);
        PipedInputStream serverInput = new PipedInputStream(8192);
        PipedOutputStream clientOutput = new PipedOutputStream(serverInput);
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try (CodexRpc rpc = new CodexRpc(clientInput, clientOutput)) {
            Future<?> server = worker.submit(() -> {
                try {
                    JSONObject request = new JSONObject(new String(CodexRpc.readPacket(serverInput), StandardCharsets.UTF_8));
                    serverOutput.write(("{\"method\":\"item/tool/call\",\"id\":\"tool-1\",\"params\":{\"tool\":\"list_simple_queues\"}}\n"
                        + "{\"method\":\"item/agentMessage/delta\",\"params\":{\"delta\":\"ignored partial\"}}\n"
                        + new JSONObject().put("id", request.get("id")).put("result", new JSONObject().put("ok", true)) + "\n").getBytes(StandardCharsets.UTF_8));
                    serverOutput.flush();
                    JSONObject response = new JSONObject(new String(CodexRpc.readPacket(serverInput), StandardCharsets.UTF_8));
                    assertEquals("tool-1", response.getString("id"));
                    assertEquals(-32601, response.getJSONObject("error").getInt("code"));
                } catch (Exception e) { throw new RuntimeException(e); }
            });
            assertTrue(rpc.request("turn/start", new JSONObject(), 2).getBoolean("ok"));
            JSONObject event = rpc.nextEvent(1000);
            assertEquals("item/tool/call", event.getString("method"));
            rpc.reject(event.get("id"));
            server.get(2, TimeUnit.SECONDS);
        } finally { worker.shutdownNow(); serverInput.close(); serverOutput.close(); }
    }

    @Test public void malformedAndOversizedPacketsFailClosed() throws Exception {
        assertThrows(IOException.class, () -> CodexRpc.readPacket(new ByteArrayInputStream("{broken".getBytes(StandardCharsets.UTF_8))));
        byte[] oversized = new byte[1048577]; java.util.Arrays.fill(oversized, (byte)'x');
        assertThrows(IOException.class, () -> CodexRpc.readPacket(new ByteArrayInputStream(oversized)));
        assertNull(CodexRpc.readPacket(new ByteArrayInputStream(new byte[0])));
    }

    @Test public void processDisconnectUnblocksPendingRequest() throws Exception {
        PipedInputStream clientInput = new PipedInputStream(8192);
        PipedOutputStream serverOutput = new PipedOutputStream(clientInput);
        PipedInputStream serverInput = new PipedInputStream(8192);
        PipedOutputStream clientOutput = new PipedOutputStream(serverInput);
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try (CodexRpc rpc = new CodexRpc(clientInput, clientOutput)) {
            Future<?> server = worker.submit(() -> {
                try { CodexRpc.readPacket(serverInput); serverOutput.close(); }
                catch (IOException e) { throw new RuntimeException(e); }
            });
            assertThrows(IOException.class, () -> rpc.request("account/read", new JSONObject(), 2));
            server.get(2, TimeUnit.SECONDS);
        } finally { worker.shutdownNow(); serverInput.close(); serverOutput.close(); }
    }
}
