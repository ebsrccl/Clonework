package id.mas.agent;

import android.app.Instrumentation;
import android.os.Bundle;
import android.util.Log;
import org.json.JSONObject;

/** Installed only in CI: no user credentials, model requests, or router contact. */
public final class SmokeProbe extends Instrumentation {
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        CodexBrain brain = new CodexBrain(getTargetContext());
        try {
            if (brain.account().getBoolean("signed_in")) throw new AssertionError("Fresh account state");
            JSONObject login = brain.login();
            if (!CodexBrain.validLoginUrl(login.getString("auth_url"))) throw new AssertionError("Login URL");
            brain.cancelLogin();
            if (brain.account().getBoolean("signed_in")) throw new AssertionError("Cancel state");
            brain.close();
            if (brain.account().getBoolean("signed_in")) throw new AssertionError("Restart state");
            SecureSession store = new SecureSession(getTargetContext());
            RouterVault vault = new RouterVault(store);
            JSONObject config = new JSONObject().put("name","CI router").put("host","127.0.0.1").put("port",8728).put("tls",false).put("username","ci").put("password","test-only").put("certificate_sha256","");
            String first = vault.saveConfig(null,config), second = vault.saveConfig(null,new JSONObject(config.toString()).put("name","Second"));
            RouterVault restored = new RouterVault(store);
            if(restored.list().length()!=2 || !second.equals(restored.active())) throw new AssertionError("Encrypted multirouter persistence");
            byte[] backup = VaultBackup.encode(restored.exportData(),"ci-only-long-password".toCharArray());
            if(VaultBackup.decode(backup,"ci-only-long-password".toCharArray()).getJSONObject("routers").length()!=2) throw new AssertionError("Android portable backup");
            restored.remove(first);restored.remove(second);
            Log.i("ChatGptSmoke","MULTIROUTER_ANDROID_STORAGE_PASS");
            result.putString("stream", "CHATGPT_ANDROID_LOGIN_PROTOCOL_PASS\n");
            Log.i("ChatGptSmoke", "CHATGPT_ANDROID_LOGIN_PROTOCOL_PASS");
            finish(-1, result);
        } catch (Throwable error) {
            Log.e("ChatGptSmoke", "CHATGPT_ANDROID_LOGIN_PROTOCOL_FAIL", error);
            result.putString("stream", "CHATGPT_ANDROID_LOGIN_PROTOCOL_FAIL\n");
            finish(0, result);
        } finally { brain.close(); }
    }
}
