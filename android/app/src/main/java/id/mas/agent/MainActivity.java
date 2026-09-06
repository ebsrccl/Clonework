package id.mas.agent;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(8, 15, 31), CARD = Color.rgb(19, 34, 57),
        TEXT = Color.rgb(232, 240, 248), MUTED = Color.rgb(145, 162, 184), ACCENT = Color.rgb(73, 218, 197);
    private static final ExecutorService worker = Executors.newSingleThreadExecutor();
    private static LocalAgent sharedAgent;
    private LocalAgent agent;
    private SecureSession storage;
    private JSONObject session;
    private JSONArray messages = new JSONArray(), plans = new JSONArray();
    private boolean demo = false, busy = false;
    private volatile boolean destroyed = false;
    private int tab = 0;
    private LinearLayout root;
    private TextView status;
    private String statusText = "";
    interface Work { JSONObject run() throws Exception; }
    interface Result { void receive(JSONObject result) throws Exception; }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storage = new SecureSession(getApplicationContext());
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        try {
            if (sharedAgent == null) sharedAgent = new LocalAgent(storage);
            agent = sharedAgent;
        } catch (Exception error) { statusText = "Data lokal tidak dapat dibaca. Gunakan Hapus data lokal untuk mengatur ulang."; }
        if (agent == null) { showSetup(); return; }
        base();
        perform(() -> {
            JSONObject profile = agent.profileSummary();
            return new JSONObject().put("profile", profile == null ? JSONObject.NULL : profile).put("snapshot", agent.snapshot());
        }, result -> {
            session = result.optJSONObject("profile"); loadSnapshot(result.getJSONObject("snapshot"));
            if (session == null) showSetup(); else showMain();
        });
    }
    private void loadSnapshot(JSONObject snapshot) {
        try { messages = snapshot.getJSONArray("messages"); plans = snapshot.getJSONArray("plans"); }
        catch (Exception error) { setStatus("Riwayat lokal tidak dapat dibaca."); }
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout row() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private GradientDrawable background(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        v.setLineSpacing(dp(3), 1); if (bold) v.setTypeface(null, Typeface.BOLD); return v;
    }
    private void space(LinearLayout parent, int height) { View v = new View(this); parent.addView(v, new LinearLayout.LayoutParams(1, dp(height))); }
    private LinearLayout card(LinearLayout parent) {
        LinearLayout v = column(); v.setPadding(dp(18), dp(18), dp(18), dp(18)); v.setBackground(background(CARD, 18));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.bottomMargin = dp(14); parent.addView(v, p); return v;
    }
    private Button button(String label, boolean primary, Runnable action) {
        Button v = new Button(this); v.setText(label); v.setAllCaps(false); v.setTextSize(14);
        v.setTextColor(primary ? BG : TEXT); v.setBackground(background(primary ? ACCENT : CARD, 12));
        v.setPadding(dp(12), dp(8), dp(12), dp(8)); v.setMinHeight(dp(48)); v.setOnClickListener(view -> action.run()); return v;
    }
    private void base() {
        root = column(); root.setBackgroundColor(BG); root.setPadding(dp(20), dp(20), dp(20), dp(12));
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top, bottom;
            if (Build.VERSION.SDK_INT >= 30) { Insets i = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime()); top = i.top; bottom = i.bottom; }
            else { top = insets.getSystemWindowInsetTop(); bottom = insets.getSystemWindowInsetBottom(); }
            v.setPadding(dp(20), top + dp(16), dp(20), bottom + dp(10)); return insets;
        });
        setContentView(root);
        root.addView(text("MIKROTIK AUTOMATION SYSTEM", 10, ACCENT, true)); space(root, 7);
        root.addView(text("MikroTik Agent Lokal", 26, TEXT, true)); space(root, 5);
        status = text(statusText, 12, MUTED, false); root.addView(status); space(root, 16);
    }
    private LinearLayout scroll(LinearLayout parent) {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        LinearLayout content = column(); scroll.addView(content); parent.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1)); return content;
    }
    private EditText field(LinearLayout parent, String label, String hint, boolean secret, String value) {
        parent.addView(text(label, 12, MUTED, false));
        EditText edit = new EditText(this); edit.setTextColor(TEXT); edit.setHintTextColor(MUTED); edit.setTextSize(15);
        edit.setHint(hint); edit.setText(value); edit.setSingleLine(true); edit.setPadding(dp(10), dp(8), dp(10), dp(8));
        edit.setBackground(background(BG, 8)); edit.setSaveEnabled(false);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | (secret ? InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS));
        if (secret) edit.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(49)); p.topMargin = dp(5); p.bottomMargin = dp(14); parent.addView(edit, p); return edit;
    }
    private String value(EditText field) { return field.getText().toString().trim(); }
    private void showSetup() {
        base();
        LinearLayout content = scroll(root), intro = card(content);
        intro.addView(text("Agen di HP. Tanpa VPS.", 20, TEXT, true)); space(intro, 9);
        intro.addView(text("HP terhubung langsung ke MikroTik. OpenAI memproses pesan dan data router yang diminta. Profil disimpan terenkripsi di HP.", 14, MUTED, false));
        content.addView(button("Lihat demo tanpa koneksi", false, () -> {
            if (busy) return; demo = true; tab = 0; plans = new JSONArray(); messages = new JSONArray();
            addMessage("assistant", "MODE DEMO: data contoh. Tidak tersambung ke MikroTik atau OpenAI."); showMain();
        })); space(content, 16);
        LinearLayout router = card(content); router.addView(text("Koneksi MikroTik", 18, TEXT, true)); space(router, 10);
        EditText name = field(router, "Nama router", "Router utama", false, "Router utama");
        EditText host = field(router, "IP / hostname MikroTik", "192.168.88.1 atau host remote", false, "");
        CheckBox tls = new CheckBox(this); tls.setText("Gunakan API-SSL (TLS)"); tls.setTextColor(TEXT); tls.setChecked(true); router.addView(tls);
        EditText port = field(router, "Port API MikroTik", "8729 untuk SSL, 8728 untuk API biasa", false, "8729"); port.setInputType(InputType.TYPE_CLASS_NUMBER);
        tls.setOnCheckedChangeListener((v, checked) -> { if (value(port).equals("8728") || value(port).equals("8729")) port.setText(checked ? "8729" : "8728"); });
        router.addView(text("API biasa mengirim password tanpa enkripsi dan hanya diizinkan ke alamat LAN/VPN. API-SSL diperlukan untuk alamat publik.", 12, MUTED, false)); space(router, 12);
        EditText username = field(router, "Username MikroTik", "Akun router dengan izin API", false, "");
        EditText password = field(router, "Password MikroTik", "Password router", true, "");
        EditText fingerprint = field(router, "SHA-256 sertifikat (opsional)", "Untuk sertifikat router yang ditentukan sendiri", false, "");
        router.addView(text("Kosongkan jika sertifikat dipercaya Android dan sesuai hostname. Untuk sertifikat sendiri, masukkan sidik jari SHA-256 yang diperoleh dari router secara tepercaya.", 12, MUTED, false));
        LinearLayout brain = card(content); brain.addView(text("OpenAI", 18, TEXT, true)); space(brain, 10);
        EditText key = field(brain, "API key OpenAI", "Key milik Boss", true, "");
        EditText model = field(brain, "Model", "ID model sesuai akses API", false, "gpt-5-mini");
        brain.addView(text("Key dipakai langsung ke api.openai.com. Password router tetap di HP. Uji koneksi membuat permintaan OpenAI menggunakan kuota API.", 12, MUTED, false));
        content.addView(button("Uji koneksi & simpan di HP", true, () -> {
            if (busy) return;
            try {
                if (agent == null) throw new Exception("Data lokal tidak dapat dibaca. Hapus data lokal untuk mengatur ulang.");
                JSONObject r = new JSONObject().put("name", value(name)).put("host", value(host)).put("port", Integer.parseInt(value(port)))
                    .put("tls", tls.isChecked()).put("certificate_sha256", value(fingerprint)).put("username", value(username)).put("password", password.getText().toString());
                JSONObject data = new JSONObject().put("router", r).put("openai_key", value(key)).put("model", value(model));
                LocalAgent.validateConfig(data);
                perform(() -> agent.setup(data), saved -> {
                    session = saved; demo = false; tab = 0; messages = new JSONArray(); plans = new JSONArray(); key.setText(""); password.setText(""); showMain();
                    setStatus("Koneksi MikroTik dan OpenAI diuji. Profil tersimpan di HP.");
                });
            } catch (NumberFormatException error) { setStatus("Isi port API dengan angka 1–65535."); }
            catch (Exception error) { setStatus(error.getMessage()); }
        })); space(content, 14);
        if (agent == null) content.addView(button("Hapus data lokal yang tidak terbaca", false, this::confirmClear));
    }
    private void confirmClear() {
        if (busy) return;
        new android.app.AlertDialog.Builder(this).setTitle("Hapus profil dan riwayat di HP?")
            .setMessage("Kredensial serta riwayat lokal dihapus. Perubahan yang sudah diterapkan pada MikroTik tetap berlaku.")
            .setNegativeButton("Batal", null).setPositiveButton("Hapus data lokal", (dialog, which) -> perform(() -> {
                if (agent != null) agent.clear(); else storage.clear();
                sharedAgent = new LocalAgent(storage); agent = sharedAgent; return new JSONObject();
            }, result -> { session = null; demo = false; messages = new JSONArray(); plans = new JSONArray(); statusText = "Data lokal dihapus."; showSetup(); })).show();
    }
    private void setStatus(String value) { statusText = value; if (status != null) status.setText(value); }
    private void showMain() {
        statusText = demo ? "MODE DEMO · DATA CONTOH" : "LOKAL DI HP · " + session.optString("router_name");
        if (busy) statusText = "Tugas sedang berjalan…";
        base();
        LinearLayout tabs = row();
        String[] labels = {"Chat", "Skill", "Aktivitas"};
        for (int i = 0; i < labels.length; i++) {
            final int next = i;
            Button b = button(labels[i], tab == i, () -> { tab = next; showMain(); });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(46), 1); if (i > 0) p.leftMargin = dp(7); tabs.addView(b, p);
        }
        root.addView(tabs); space(root, 16);
        if (tab == 0) showChat(); else if (tab == 1) showSkills(); else showActivity();
    }
    private void addMessage(String role, String message) {
        try { messages.put(new JSONObject().put("role", role).put("text", message)); } catch (Exception ignored) { }
    }
    private void showChat() {
        LinearLayout content = scroll(root);
        if (messages.length() == 0) {
            LinearLayout welcome = card(content); welcome.addView(text("Apa yang ingin diperiksa?", 20, TEXT, true)); space(welcome, 10);
            welcome.addView(text("Cek status router, daftar interface, profil hotspot, atau batas bandwidth.", 14, MUTED, false));
        }
        for (int i = 0; i < messages.length(); i++) {
            JSONObject message = messages.optJSONObject(i); if (message == null) continue;
            LinearLayout bubble = card(content);
            bubble.addView(text("user".equals(message.optString("role")) ? "BOSS" : "AGEN MIKROTIK", 10, ACCENT, true)); space(bubble, 8);
            TextView body = text(message.optString("text"), 15, TEXT, false); body.setTextIsSelectable(true); bubble.addView(body);
        }
        if (plans.length() > 0) content.addView(button("Lihat hasil & usulan perubahan", false, () -> { tab = 2; showMain(); }));
        View scrollView = (View) content.getParent(); scrollView.post(() -> ((ScrollView) scrollView).fullScroll(View.FOCUS_DOWN));
        space(root, 8);
        LinearLayout composer = row(); EditText input = new EditText(this); input.setTextColor(TEXT); input.setHintTextColor(MUTED);
        input.setTextSize(15); input.setHint("Perintah untuk MikroTik…"); input.setMaxLines(4); input.setBackground(background(CARD, 12));
        input.setPadding(dp(12), dp(10), dp(12), dp(10)); composer.addView(input, new LinearLayout.LayoutParams(0, -2, 1));
        Button send = button("Kirim", true, () -> {
            String message = value(input); if (message.isEmpty() || busy) return;
            if (message.length() > 3000) { setStatus("Pesan maksimal 3000 karakter."); return; }
            addMessage("user", message);
            if (demo) {
                addMessage("assistant", message.toLowerCase(java.util.Locale.ROOT).contains("mikhmon") ? "DEMO: Mikhmon belum dipasang. Konektor akan dikembangkan setelah layanannya disiapkan." :
                    "DATA CONTOH: Router utama · uptime 2 hari · CPU 8%. Ini simulasi tampilan, bukan pembacaan router atau respons OpenAI. Mode nyata dapat membaca data router dan menyiapkan perubahan bandwidth."); showMain(); return;
            }
            try {
                perform(() -> agent.chat(message), result -> {
                    messages = result.getJSONArray("messages"); plans = result.getJSONArray("plans"); showMain();
                }); showMain();
            } catch (Exception error) { setStatus("Pesan tidak dapat disiapkan."); }
        });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(76), dp(50)); p.leftMargin = dp(8); composer.addView(send, p); root.addView(composer);
    }
    private void showSkills() {
        LinearLayout content = scroll(root);
        String[][] skills = {{"Monitoring router", "Identitas, sumber daya, interface, dan profil hotspot."},
            {"Bandwidth", "Baca queue, tinjau perubahan, terapkan, lalu periksa hasil."},
            {"Mikhmon · belum tersedia", "Aplikasi ini memakai RouterOS API langsung. Konektor Mikhmon belum dibuat."},
            {"Modul lanjutan · belum tersedia", "Voucher, PPPoE, firewall, NAT, backup, dan penjadwalan belum diimplementasikan."}};
        for (String[] skill : skills) { LinearLayout v = card(content); v.addView(text(skill[0], 17, TEXT, true)); space(v, 8); v.addView(text(skill[1], 14, MUTED, false)); }
        if (!demo) {
            content.addView(button("Cek router langsung · tanpa OpenAI", false, () -> {
                if (busy) return;
                perform(() -> agent.execute("router_summary", new JSONObject()), result -> {
                    tab = 0; addMessage("assistant", "Pembacaan langsung dari MikroTik:\n" + result.toString(2)); showMain();
                });
            })); space(content, 12);
        }
        content.addView(button(demo ? "Keluar dari demo" : "Hapus profil lokal / ganti koneksi", false, () -> {
            if (busy) return;
            if (demo) { demo = false; messages = new JSONArray(); statusText = ""; showSetup(); return; }
            confirmClear();
        }));
    }
    private String planStatus(String s) {
        switch (s) {
            case "pending": return "Usulan · belum diterapkan";
            case "verified": return "Berhasil · sudah diperiksa ulang";
            case "stale": return "Konfigurasi berubah · buat usulan baru";
            case "expired": return "Usulan kedaluwarsa";
            case "applying": case "unknown": return "Hasil belum dapat dipastikan · periksa router";
            default: return "Belum terverifikasi · periksa router";
        }
    }
    private void showActivity() {
        LinearLayout content = scroll(root);
        if (plans.length() == 0) { LinearLayout v = card(content); v.addView(text("Belum ada perubahan", 19, TEXT, true)); space(v, 8);
            v.addView(text("Usulan perubahan bandwidth beserta hasil pemeriksaannya akan tampil di sini.", 14, MUTED, false)); }
        for (int i = plans.length() - 1; i >= 0; i--) {
            JSONObject plan = plans.optJSONObject(i); if (plan == null) continue;
            JSONObject before = plan.optJSONObject("before"); if (before == null) continue;
            LinearLayout v = card(content); v.addView(text(before.optString("name"), 18, TEXT, true)); space(v, 8);
            v.addView(text(planStatus(plan.optString("status")), 12, ACCENT, true)); space(v, 10);
            v.addView(text("Target: " + before.optString("target") + "\nSebelum: " + before.optString("max-limit") +
                "\nUsulan: " + plan.optString("after") + " bps\nUrutan: upload / download", 14, TEXT, false));
            if ("pending".equals(plan.optString("status")) && !demo) {
                space(v, 12); v.addView(button("Terapkan batas kecepatan ini", true, () -> {
                    if (busy) return;
                    perform(() -> { agent.apply(plan.getString("id")); return agent.snapshot(); }, result -> { loadSnapshot(result); showMain(); });
                }));
            }
        }
        if (!demo) content.addView(button("Muat ulang aktivitas", false, this::refresh));
    }
    private void refresh() {
        if (busy || demo || session == null) return;
        perform(() -> agent.snapshot(), result -> { loadSnapshot(result); showMain(); });
    }
    private void perform(Work work, Result then) {
        if (busy) return; busy = true; setStatus("Menghubungkan dan menjalankan tugas…");
        worker.execute(() -> {
            try {
                JSONObject result = work.run();
                runOnUiThread(() -> { if (destroyed) return; busy = false; try { then.receive(result); } catch (Exception ignored) { setStatus("Hasil tidak dapat ditampilkan. Muat ulang aktivitas."); } });
            } catch (Exception error) {
                JSONObject latest = null;
                try { if (agent != null) latest = agent.snapshot(); } catch (Exception ignored) { }
                final JSONObject refreshed = latest;
                runOnUiThread(() -> { if (destroyed) return; busy = false;
                    if (!demo && session != null && refreshed != null) { loadSnapshot(refreshed); showMain(); }
                    setStatus(error.getMessage() == null ? "Tugas gagal. Periksa jaringan HP, MikroTik, dan OpenAI." : error.getMessage());
                });
            }
        });
    }
    @Override protected void onDestroy() { destroyed = true; super.onDestroy(); }
}
