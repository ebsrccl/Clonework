package id.mas.agent;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.*;

/** User-owned portable backup; ChatGPT OAuth files are deliberately outside this store. */
final class VaultBackup {
    static byte[] crypt(int mode, byte[] value, char[] password, byte[] salt, byte[] iv) throws Exception {
        if(password.length<12) throw new Exception("Frasa sandi minimal 12 karakter.");
        PBEKeySpec spec=new PBEKeySpec(password,salt,180000,256);
        byte[] key=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); spec.clearPassword();
        try { Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(mode,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,iv)); cipher.updateAAD("MAS4".getBytes(StandardCharsets.US_ASCII)); return cipher.doFinal(value); }
        finally { java.util.Arrays.fill(key,(byte)0); }
    }
    static byte[] encode(JSONObject data,char[] password) throws Exception {
        SecureRandom random=new SecureRandom(); byte[] salt=new byte[16],iv=new byte[12]; random.nextBytes(salt); random.nextBytes(iv);
        Base64.Encoder b=Base64.getEncoder(); return new JSONObject().put("format","MAS4").put("salt",b.encodeToString(salt)).put("iv",b.encodeToString(iv)).put("data",b.encodeToString(crypt(Cipher.ENCRYPT_MODE,data.toString().getBytes(StandardCharsets.UTF_8),password,salt,iv))).toString().getBytes(StandardCharsets.UTF_8);
    }
    static JSONObject decode(byte[] bytes,char[] password) throws Exception {
        if(bytes.length>12000000) throw new Exception("Cadangan terlalu besar.");
        JSONObject o=new JSONObject(new String(bytes,StandardCharsets.UTF_8)); if(!"MAS4".equals(o.getString("format"))) throw new Exception("Format cadangan tidak valid.");
        Base64.Decoder b=Base64.getDecoder(); byte[] salt=b.decode(o.getString("salt")),iv=b.decode(o.getString("iv"));
        if(salt.length!=16||iv.length!=12) throw new Exception("Cadangan tidak valid.");
        return new JSONObject(new String(crypt(Cipher.DECRYPT_MODE,b.decode(o.getString("data")),password,salt,iv),StandardCharsets.UTF_8));
    }
}
