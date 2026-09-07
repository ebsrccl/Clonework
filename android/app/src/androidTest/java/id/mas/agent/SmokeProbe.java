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
