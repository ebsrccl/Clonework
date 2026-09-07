package id.mas.codexprobe;

import android.app.Instrumentation;
import android.os.Bundle;
import android.util.Log;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import org.json.JSONObject;

/** Feasibility gate only: no credentials, router operations, or model requests. */
public final class Probe extends Instrumentation {
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        Process process = null;
        ExecutorService reader = Executors.newSingleThreadExecutor();
        try {
            File home = new File(getTargetContext().getFilesDir(), "codex");
            if (!home.mkdirs() && !home.isDirectory()) throw new IOException("Home unavailable");
            String binary = getTargetContext().getApplicationInfo().nativeLibraryDir + "/libcodex.so";
            ProcessBuilder builder = new ProcessBuilder(binary, "--listen", "stdio://", "-c", "features.code_mode_host=false", "-c", "features.apps=false", "-c", "features.plugins=false");
            builder.directory(home);
            builder.environment().put("CODEX_HOME", home.getAbsolutePath());
            builder.environment().put("HOME", home.getAbsolutePath());
            builder.environment().put("TMPDIR", getTargetContext().getCacheDir().getAbsolutePath());
            builder.redirectError(new File(home, "stderr.txt"));
            process = builder.start();
            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            Writer output = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8);
            output.write("{\"id\":1,\"method\":\"initialize\",\"params\":{\"clientInfo\":{\"name\":\"mikrotik_android_probe\",\"title\":\"MikroTik Android Probe\",\"version\":\"0.1\"},\"capabilities\":{\"experimentalApi\":true}}}\n"); output.flush();
            JSONObject reply = reader.submit(() -> {
                String line;
                while ((line = input.readLine()) != null) {
                    JSONObject message = new JSONObject(line);
                    if (message.optInt("id", -1) == 1) return message;
                }
                throw new IOException("Runtime exited before initialize");
            }).get(45, TimeUnit.SECONDS);
            if (!reply.has("result")) throw new IOException("Initialize rejected: " + reply);
            Log.i("CodexProbe", "INITIALIZE_OK " + reply.getJSONObject("result"));
            output.write("{\"method\":\"initialized\"}\n{\"id\":2,\"method\":\"account/read\",\"params\":{\"refreshToken\":false}}\n"); output.flush();
            JSONObject account = reader.submit(() -> {
                String line;
                while ((line = input.readLine()) != null) {
                    JSONObject message = new JSONObject(line);
                    if (message.optInt("id", -1) == 2) return message;
                }
                throw new IOException("Runtime exited before account/read");
            }).get(20, TimeUnit.SECONDS);
            if (!account.has("result") || !account.getJSONObject("result").isNull("account")) throw new IOException("Unexpected account state");
            result.putString("stream", "CODEX_ANDROID_PROBE_PASS\n");
            Log.i("CodexProbe", "CODEX_ANDROID_PROBE_PASS");
            finish(-1, result);
        } catch (Exception error) {
            Log.e("CodexProbe", "CODEX_ANDROID_PROBE_FAIL", error);
            result.putString("stream", "CODEX_ANDROID_PROBE_FAIL " + error + "\n");
            finish(0, result);
        } finally {
            if (process != null) process.destroy();
            reader.shutdownNow();
        }
    }
}
