package id.mas.agent;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class SecureSession {
    private static final String ALIAS = "mas-device-session-v1";
    private final SharedPreferences prefs;
    SecureSession(Context context) { prefs = context.getSharedPreferences("device_session", Context.MODE_PRIVATE); }
    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        if (store.containsAlias(ALIAS)) return ((KeyStore.SecretKeyEntry) store.getEntry(ALIAS, null)).getSecretKey();
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).setRandomizedEncryptionRequired(true).build());
        return generator.generateKey();
    }
    void save(JSONObject session) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] encrypted = cipher.doFinal(session.toString().getBytes(StandardCharsets.UTF_8));
        if (!prefs.edit().putString("iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
            .putString("body", Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit()) throw new Exception("Sesi tidak dapat disimpan di perangkat.");
    }
    JSONObject read() throws Exception {
        String body = prefs.getString("body", null);
        if (body == null) return null;
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(prefs.getString("iv", ""), Base64.NO_WRAP)));
        return new JSONObject(new String(cipher.doFinal(Base64.decode(body, Base64.NO_WRAP)), StandardCharsets.UTF_8));
    }
    void clear() { prefs.edit().clear().commit(); }
}
