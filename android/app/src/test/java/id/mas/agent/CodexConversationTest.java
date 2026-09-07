package id.mas.agent;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class CodexConversationTest {
    static JSONObject take(InputStream input) throws Exception {
        return new JSONObject(new String(CodexRpc.readPacket(input), StandardCharsets.UTF_8));
    }
    static void send(OutputStream output, JSONObject message) throws Exception {
        output.write((message + "\n").getBytes(StandardCharsets.UTF_8)); output.flush();
    }
    static void answer(OutputStream output, JSONObject request, JSONObject result) throws Exception {
        send(output, new JSONObject().put("id", request.get("id")).put("result", result));
    }
    static JSONObject tool(OutputStream output, InputStream input, String id, String name, JSONObject args) throws Exception {
        send(output, new JSONObject().put("id", id).put("method", "item/tool/call").put("params", new JSONObject()
            .put("threadId", "thread-test").put("turnId", "turn-test").put("callId", id).put("tool", name).put("arguments", args)));
        JSONObject response = take(input); assertEquals(id, response.getString("id"));
        return response.getJSONObject("result");
    }
    @Test(timeout = 10000) public void chatCreatesProposalButOnlyApplicationCanApply() throws Exception {
        PipedInputStream clientInput = new PipedInputStream(16384), serverInput = new PipedInputStream(16384);
        PipedOutputStream serverOutput = new PipedOutputStream(clientInput), clientOutput = new PipedOutputStream(serverInput);
        ExecutorService worker = Executors.newSingleThreadExecutor();
        LocalAgentTest.Memory memory = new LocalAgentTest.Memory();
        LocalAgentTest.FakeRouter router = new LocalAgentTest.FakeRouter();
        try (CodexRpc rpc = new CodexRpc(clientInput, clientOutput)) {
            Future<?> server = worker.submit(() -> {
                try {
                    JSONObject thread = take(serverInput); assertEquals("thread/start", thread.getString("method"));
                    JSONObject params = thread.getJSONObject("params");
                    assertEquals(0, params.getJSONArray("environments").length());
                    assertTrue(params.getBoolean("ephemeral")); assertEquals("never", params.getString("approvalPolicy"));
                    assertFalse(params.toString().contains("router-secret-test"));
                    answer(serverOutput, thread, new JSONObject().put("thread", new JSONObject().put("id", "thread-test")));
                    JSONObject turn = take(serverInput); assertEquals("turn/start", turn.getString("method"));
                    assertEquals(0, turn.getJSONObject("params").getJSONArray("environments").length());
                    answer(serverOutput, turn, new JSONObject().put("turn", new JSONObject().put("id", "turn-test")));
                    assertFalse(tool(serverOutput, serverInput, "bad-1", "apply", new JSONObject().put("id", "x")).getBoolean("success"));
                    assertFalse(tool(serverOutput, serverInput, "bad-2", "shell", new JSONObject().put("command", "reboot")).getBoolean("success"));
                    JSONObject proposal = tool(serverOutput, serverInput, "ok-1", "prepare_queue_limit", LocalAgentTest.args());
                    assertTrue(proposal.getBoolean("success"));
                    assertTrue(proposal.getJSONArray("contentItems").getJSONObject(0).getString("text").contains("pending"));
                    send(serverOutput, new JSONObject().put("method", "item/completed").put("params", new JSONObject().put("threadId", "thread-test")
                        .put("turnId", "turn-test").put("item", new JSONObject().put("type", "agentMessage").put("text", "Usulan siap."))));
                    send(serverOutput, new JSONObject().put("method", "turn/completed").put("params", new JSONObject().put("threadId", "thread-test")
                        .put("turn", new JSONObject().put("id", "turn-test").put("status", "completed"))));
                    JSONObject unsubscribe = take(serverInput); assertEquals("thread/unsubscribe", unsubscribe.getString("method"));
                    answer(serverOutput, unsubscribe, new JSONObject());
                } catch (Exception e) { throw new RuntimeException(e); }
            });
            LocalAgent agent = new LocalAgent(memory, router, (message, history, tools) -> CodexConversation.reply(rpc, message, history, tools));
            JSONObject state = agent.chat("Ubah batas bandwidth queue.");
            server.get(3, TimeUnit.SECONDS);
            assertEquals("Usulan siap.", state.getJSONArray("messages").getJSONObject(1).getString("text"));
            assertEquals(0, router.writes);
            JSONObject plan = state.getJSONArray("plans").getJSONObject(0);
            assertEquals("pending", plan.getString("status"));
            assertEquals("verified", agent.apply(plan.getString("id")).getString("status"));
            assertEquals(1, router.writes);
        } finally { worker.shutdownNow(); serverOutput.close(); serverInput.close(); }
    }
    @Test public void browserLoginRejectsNonOfficialDestinations() {
        assertTrue(CodexBrain.validLoginUrl("https://auth.openai.com/oauth/authorize?state=test"));
        assertFalse(CodexBrain.validLoginUrl("https://auth.openai.com.attacker.invalid/"));
        assertFalse(CodexBrain.validLoginUrl("https://auth.openai.com@attacker.invalid/"));
        assertFalse(CodexBrain.validLoginUrl("http://auth.openai.com/"));
        assertFalse(CodexBrain.validLoginUrl("javascript:alert(1)"));
    }
}
