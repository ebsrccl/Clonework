package id.mas.agent;

import android.content.Context;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/** Official Codex-managed ChatGPT login and local dynamic-tool execution. */
final class CodexBrain implements LocalAgent.Brain {
    private final Context context;
    private Process process;
    private CodexRpc rpc;
    private String loginId;
    private final File home;
    CodexBrain(Context context) { this.context = context.getApplicationContext(); home = new File(context.getNoBackupFilesDir(), "codex-chatgpt"); }

    private synchronized void start() throws Exception {
        if (process != null && process.isAlive() && rpc != null && rpc.isOpen()) return;
        close();
        File binary = new File(context.getApplicationInfo().nativeLibraryDir, "libcodex.so");
        if (!binary.isFile()) throw new IOException("Runtime ChatGPT untuk arsitektur HP ini tidak tersedia. Gunakan APK ARM64.");
        if (!home.mkdirs() && !home.isDirectory()) throw new IOException("Penyimpanan agen lokal tidak tersedia.");
        File workspace = new File(home, "workspace");
        if (!workspace.mkdirs() && !workspace.isDirectory()) throw new IOException("Ruang kerja agen tidak tersedia.");
        StringBuilder config = new StringBuilder("forced_login_method = \"chatgpt\"\ncli_auth_credentials_store = \"file\"\nweb_search = \"disabled\"\napproval_policy = \"never\"\nsandbox_mode = \"read-only\"\n")
            .append("[tools.experimental_request_user_input]\nenabled = false\n[tools.update_plan]\nenabled = false\n[features]\n");
        for (String feature : new String[]{"shell_tool", "shell_snapshot", "view_image", "sleep_tool", "code_mode", "code_mode_host", "js_repl",
            "apps", "plugins", "remote_plugin", "recommended_plugins", "tool_suggest", "hooks", "plugin_hooks", "multi_agent", "multi_agent_v2",
            "in_app_browser", "browser_use", "browser_use_external", "computer_use", "image_generation", "artifact", "goals", "memories",
            "skill_search", "skill_mcp_dependency_install", "request_permissions_tool", "remote_control", "workspace_dependencies", "standalone_web_search"})
            config.append(feature).append(" = false\n");
        config.append("skip_host_skill_discovery = true\n");
        Files.write(new File(home, "config.toml").toPath(), config.toString().getBytes(StandardCharsets.UTF_8));
        File roots = new File(home, "android-ca.pem"); writeSystemRoots(roots);
        ProcessBuilder builder = new ProcessBuilder(binary.getAbsolutePath(), "--listen", "stdio://");
        builder.directory(workspace);
        Map<String,String> env = builder.environment();
        env.remove("OPENAI_API_KEY"); env.remove("CODEX_ACCESS_TOKEN");
        env.put("CODEX_HOME", home.getAbsolutePath()); env.put("HOME", home.getAbsolutePath());
        env.put("TMPDIR", context.getCacheDir().getAbsolutePath());
        env.put("CODEX_CA_CERTIFICATE", roots.getAbsolutePath());
        env.put("LD_LIBRARY_PATH", context.getApplicationInfo().nativeLibraryDir);
        // Native diagnostics may contain account details; do not retain or expose them.
        builder.redirectError(new File("/dev/null"));
        process = builder.start(); rpc = new CodexRpc(process.getInputStream(), process.getOutputStream());
        try {
            rpc.request("initialize", new JSONObject().put("clientInfo", new JSONObject().put("name", "mikrotik_agent_android")
                .put("title", "MikroTik Agent Android").put("version", "0.4.0"))
                .put("capabilities", new JSONObject().put("experimentalApi", true)), 45);
            rpc.notify("initialized");
        } catch (Exception e) { close(); throw new IOException("Runtime ChatGPT gagal dimulai di HP ini."); }
    }

    private static void writeSystemRoots(File file) throws Exception {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init((KeyStore)null);
        StringBuilder pem = new StringBuilder();
        for (TrustManager manager : factory.getTrustManagers()) if (manager instanceof X509TrustManager)
            for (X509Certificate cert : ((X509TrustManager)manager).getAcceptedIssuers())
                pem.append("-----BEGIN CERTIFICATE-----\n").append(Base64.encodeToString(cert.getEncoded(), Base64.NO_WRAP))
                    .append("\n-----END CERTIFICATE-----\n");
        if (pem.length() == 0) throw new IOException("Sertifikat sistem Android tidak tersedia.");
        Files.write(file.toPath(), pem.toString().getBytes(StandardCharsets.US_ASCII));
    }

    synchronized JSONObject account() throws Exception {
        start();
        JSONObject account = rpc.request("account/read", new JSONObject().put("refreshToken", false), 30).optJSONObject("account");
        if (account == null || !"chatgpt".equals(account.optString("type"))) return new JSONObject().put("signed_in", false);
        loginId = null;
        return new JSONObject().put("signed_in", true).put("email", account.optString("email")).put("plan", account.optString("planType"));
    }

    synchronized JSONObject login() throws Exception {
        start(); cancelLogin();
        JSONObject login = rpc.request("account/login/start", new JSONObject().put("type", "chatgpt").put("appBrand", "chatgpt"), 45);
        loginId = login.getString("loginId");
        String url = login.getString("authUrl");
        if (!validLoginUrl(url)) { cancelLogin(); throw new IOException("Alamat login resmi tidak valid."); }
        return new JSONObject().put("auth_url", url);
    }

    static boolean validLoginUrl(String value) {
        try {
            URI uri = new URI(value);
            return "https".equals(uri.getScheme()) && uri.getRawUserInfo() == null && (uri.getPort() == -1 || uri.getPort() == 443)
                && ("auth.openai.com".equals(uri.getHost()) || "chatgpt.com".equals(uri.getHost()));
        } catch (Exception e) { return false; }
    }

    synchronized void cancelLogin() throws Exception {
        if (loginId != null && rpc != null) {
            String id = loginId; loginId = null;
            rpc.request("account/login/cancel", new JSONObject().put("loginId", id), 15);
        }
    }
    synchronized void logout() throws Exception {
        start(); cancelLogin(); rpc.request("account/logout", new JSONObject(), 30); close();
    }
    synchronized void requireAccount() throws Exception {
        if (!account().getBoolean("signed_in")) throw new IOException("Login ChatGPT dahulu, lalu tekan Periksa login.");
    }

    @Override public synchronized String reply(String message, JSONArray history, LocalAgent.ToolRunner tools) throws Exception {
        requireAccount();
        return CodexConversation.reply(rpc, message, history, tools);
    }

    synchronized void close() {
        if (process != null) {
            process.destroy();
            try { if (!process.waitFor(2, TimeUnit.SECONDS)) { process.destroyForcibly(); process.waitFor(2, TimeUnit.SECONDS); } }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); process.destroyForcibly(); }
        }
        if (rpc != null) rpc.close();
        process = null; rpc = null; loginId = null;
    }
    synchronized void clearAll() throws Exception {
        close();
        if (home.exists()) try (java.util.stream.Stream<java.nio.file.Path> paths = Files.walk(home.toPath())) {
            for (java.nio.file.Path path : paths.sorted(Comparator.reverseOrder()).toArray(java.nio.file.Path[]::new)) Files.delete(path);
        }
    }
}
