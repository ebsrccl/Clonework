package id.mas.agent;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.net.ssl.*;

/** Direct RouterOS API connection. No proxy, listener, or command supplied by the model. */
final class RouterApi implements LocalAgent.Router {
    private final JSONObject config;
    RouterApi(JSONObject config) { this.config = config; }
    static void writeLength(OutputStream out, long n) throws IOException {
        if (n < 0 || n > 0xffffffffL) throw new IOException("Panjang kata tidak valid.");
        if (n < 0x80) out.write((int)n);
        else if (n < 0x4000) { out.write((int)(n >> 8) | 0x80); out.write((int)n); }
        else if (n < 0x200000) { out.write((int)(n >> 16) | 0xc0); out.write((int)(n >> 8)); out.write((int)n); }
        else if (n < 0x10000000) { out.write((int)(n >> 24) | 0xe0); out.write((int)(n >> 16)); out.write((int)(n >> 8)); out.write((int)n); }
        else { out.write(0xf0); for (int s = 24; s >= 0; s -= 8) out.write((int)(n >> s)); }
    }
    static int byteIn(InputStream in) throws IOException { int b = in.read(); if (b < 0) throw new EOFException("Router menutup koneksi."); return b; }
    static long readLength(InputStream in) throws IOException {
        int b = byteIn(in), count; long n;
        if (b < 0x80) return b;
        if (b < 0xc0) { count = 1; n = b & 0x3f; }
        else if (b < 0xe0) { count = 2; n = b & 0x1f; }
        else if (b < 0xf0) { count = 3; n = b & 0x0f; }
        else if (b == 0xf0) { count = 4; n = 0; }
        else throw new IOException("Prefix API tidak didukung.");
        while (count-- > 0) n = n * 256 + byteIn(in);
        return n;
    }
    static void writeSentence(OutputStream out, String... words) throws IOException {
        for (String word : words) { byte[] b = word.getBytes(StandardCharsets.UTF_8); if (b.length > 1048576) throw new IOException("Kata terlalu besar."); writeLength(out, b.length); out.write(b); }
        out.write(0); out.flush();
    }
    static List<String> readSentence(InputStream in) throws IOException {
        List<String> words = new ArrayList<>(); long total = 0;
        while (true) {
            long length = readLength(in); if (length == 0) return words;
            total += length;
            if (length > 1048576 || total > 2097152 || words.size() >= 4096) throw new IOException("Balasan router terlalu besar.");
            byte[] data = new byte[(int)length]; new DataInputStream(in).readFully(data);
            words.add(new String(data, StandardCharsets.UTF_8));
        }
    }
    static boolean localAddress(InetAddress address) {
        byte[] b = address.getAddress();
        return address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()
            || (b.length == 16 && (b[0] & 0xfe) == 0xfc)
            || (b.length == 4 && (b[0] & 255) == 100 && (b[1] & 0xc0) == 64);
    }
    private Socket connect() throws Exception {
        String host = config.getString("host"); int port = config.getInt("port"); boolean tls = config.getBoolean("tls");
        InetAddress[] addresses = InetAddress.getAllByName(host);
        if (!tls) for (InetAddress a : addresses) if (!localAddress(a)) throw new IOException("API biasa hanya untuk alamat LAN/VPN. Gunakan API-SSL untuk alamat publik.");
        Socket raw = new Socket();
        try {
            raw.connect(new InetSocketAddress(addresses[0], port), 12000); raw.setSoTimeout(12000);
            if (!tls) return raw;
            String pin = config.optString("certificate_sha256");
            SSLSocketFactory factory = (SSLSocketFactory)SSLSocketFactory.getDefault();
            if (!pin.isEmpty()) {
                final String expected = pin;
                TrustManager[] managers = {new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] c, String a) throws java.security.cert.CertificateException { throw new java.security.cert.CertificateException("Client certificate not supported"); }
                    public void checkServerTrusted(X509Certificate[] chain, String auth) throws java.security.cert.CertificateException {
                        try {
                            if (chain == null || chain.length == 0) throw new Exception();
                            chain[0].checkValidity();
                            byte[] hash = MessageDigest.getInstance("SHA-256").digest(chain[0].getEncoded());
                            StringBuilder hex = new StringBuilder(); for (byte b : hash) hex.append(String.format(Locale.ROOT, "%02x", b & 255));
                            if (!MessageDigest.isEqual(hex.toString().getBytes(StandardCharsets.US_ASCII), expected.getBytes(StandardCharsets.US_ASCII))) throw new Exception();
                        } catch (Exception e) { throw new java.security.cert.CertificateException("Sidik jari sertifikat router tidak cocok atau kedaluwarsa."); }
                    }
                }};
                SSLContext context = SSLContext.getInstance("TLS"); context.init(null, managers, null); factory = context.getSocketFactory();
            }
            SSLSocket socket = (SSLSocket)factory.createSocket(raw, host, port, true);
            List<String> protocols = new ArrayList<>(); for (String p : socket.getSupportedProtocols()) if (p.equals("TLSv1.2") || p.equals("TLSv1.3")) protocols.add(p);
            socket.setEnabledProtocols(protocols.toArray(new String[0]));
            if (pin.isEmpty()) { SSLParameters parameters = socket.getSSLParameters(); parameters.setEndpointIdentificationAlgorithm("HTTPS"); socket.setSSLParameters(parameters); }
            socket.startHandshake(); return socket;
        } catch (Exception e) { raw.close(); throw e; }
    }
    private JSONArray command(Socket socket, String... words) throws Exception {
        writeSentence(socket.getOutputStream(), words);
        JSONArray rows = new JSONArray(); long bytes = 0, end = System.nanoTime() + 30_000_000_000L;
        for (int i = 0; i < 5000; i++) {
            if (System.nanoTime() > end) throw new IOException("Router melewati batas waktu.");
            List<String> sentence = readSentence(socket.getInputStream()); if (sentence.isEmpty()) continue;
            JSONObject row = new JSONObject();
            for (String word : sentence) { bytes += word.length(); if (bytes > 2097152) throw new IOException("Data router melebihi batas.");
                if (word.startsWith("=")) { int split = word.indexOf('=', 1); if (split > 1) row.put(word.substring(1, split), word.substring(split + 1)); } }
            String kind = sentence.get(0);
            if (kind.equals("!fatal") || kind.equals("!trap")) throw new IOException("MikroTik menolak login/perintah. Periksa akun, izin API, dan parameter.");
            if (kind.equals("!done")) { if (words[0].equals("/login") && row.has("ret")) throw new IOException("Gunakan RouterOS 6.43 atau lebih baru."); return rows; }
            if (kind.equals("!re")) { if (rows.length() >= 2000) throw new IOException("Data melebihi 2000 baris; persempit konfigurasi yang dibaca."); rows.put(row); }
        }
        throw new IOException("Balasan router melebihi batas.");
    }
    private JSONArray run(String... words) throws Exception {
        try (Socket socket = connect()) {
            command(socket, "/login", "=name=" + config.getString("username"), "=password=" + config.getString("password"));
            return command(socket, words);
        } catch (SSLException e) { throw new IOException("API-SSL gagal diverifikasi. Periksa sertifikat, hostname, atau sidik jari SHA-256 router."); }
        catch (SocketTimeoutException e) { throw new IOException("MikroTik tidak merespons. Periksa jaringan HP, alamat, port API, dan firewall."); }
        catch (UnknownHostException e) { throw new IOException("Alamat MikroTik tidak ditemukan. Periksa host dan DNS HP."); }
        catch (ConnectException e) { throw new IOException("Koneksi MikroTik gagal. Pastikan API aktif dan alamat/port dapat dijangkau HP."); }
    }
    public JSONArray read(String path, String[] fields) throws Exception {
        JSONArray raw = run(path + "/print", "=.proplist=" + String.join(",", fields)), clean = new JSONArray();
        for (int i = 0; i < raw.length(); i++) { JSONObject row = raw.getJSONObject(i), selected = new JSONObject(); for (String field : fields) if (row.has(field)) selected.put(field, row.get(field)); clean.put(selected); }
        return clean;
    }
    @Override public JSONArray lookup(String path,String[] fields,String key,String value) throws Exception {
        JSONArray raw=run(path+"/print", "=.proplist="+String.join(",",fields), "?"+key+"="+value),clean=new JSONArray();
        for(int i=0;i<raw.length();i++){JSONObject selected=new JSONObject(),row=raw.getJSONObject(i);for(String field:fields)if(row.has(field))selected.put(field,row.get(field));clean.put(selected);}return clean;
    }
    @Override public void mutate(String path,String action,JSONObject args) throws Exception {
        java.util.List<String> words=new java.util.ArrayList<>(); words.add(path+"/"+action);
        java.util.Iterator<String> keys=args.keys(); while(keys.hasNext()) { String key=keys.next(); words.add("="+key+"="+args.getString(key)); }
        run(words.toArray(new String[0]));
    }
    public void setQueue(String id, String limit) throws Exception { run("/queue/simple/set", "=.id=" + id, "=max-limit=" + limit); }
}
