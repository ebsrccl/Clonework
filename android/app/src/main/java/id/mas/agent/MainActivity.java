package id.mas.agent;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private final int BG=Color.rgb(8,15,31),CARD=Color.rgb(19,34,57),TEXT=Color.rgb(232,240,248),MUTED=Color.rgb(145,162,184),ACCENT=Color.rgb(73,218,197);
    private static final ExecutorService worker=Executors.newSingleThreadExecutor();
    private static CodexBrain sharedBrain;
    private CodexBrain brain; private RouterVault vault; private LocalAgent agent;
    private JSONObject account=new JSONObject(),snapshot=new JSONObject(); private JSONArray routers=new JSONArray();
    private LinearLayout root,body; private TextView status; private boolean busy,destroyed,keepLoginAlive; private int tab=0;
    private String statusText="Memuat agen…",activeName="Belum ada router";
    private byte[] pendingExport; private String exportMime,exportName; private char[] importPassword;
    interface Work { JSONObject run() throws Exception; } interface Result { void receive(JSONObject result) throws Exception; }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
        if(sharedBrain==null)sharedBrain=new CodexBrain(getApplicationContext());brain=sharedBrain;
        base();perform(()->{
            vault=new RouterVault(new SecureSession(getApplicationContext()));reload();
            try{account=brain.account();}catch(Exception e){account=new JSONObject().put("error",e.getMessage());}
            return new JSONObject();
        },r->{if(routers.length()==0&&!account.optBoolean("signed_in"))getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);statusText="Siap. Pilih router lalu jalankan fungsi.";if(routers.length()==0)tab=1;show();});
    }
    private void reload() throws Exception {
        routers=vault.list();if(vault.active().isEmpty()){agent=null;snapshot=new JSONObject();activeName="Belum ada router";}
        else {agent=new LocalAgent(vault.forRouter(vault.active()),brain);snapshot=agent.snapshot();activeName=vault.config(vault.active()).optString("name");}
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout col(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String s,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(dp(3),1);if(bold)v.setTypeface(null,Typeface.BOLD);return v;}
    private void space(LinearLayout p,int n){p.addView(new View(this),new LinearLayout.LayoutParams(1,dp(n)));}
    private LinearLayout card(LinearLayout p){LinearLayout v=col();v.setPadding(dp(16),dp(16),dp(16),dp(16));v.setBackground(bg(CARD,16));LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(-1,-2);q.bottomMargin=dp(14);p.addView(v,q);return v;}
    private Button button(String label,boolean primary,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(primary?BG:TEXT);b.setTextSize(14);b.setBackground(bg(primary?ACCENT:CARD,12));b.setMinHeight(dp(48));b.setOnClickListener(v->{if(!busy)action.run();});return b;}
    private void addButton(LinearLayout p,String label,boolean primary,Runnable action){p.addView(button(label,primary,action));space(p,10);}
    private EditText field(LinearLayout p,String label,String value,boolean secret){p.addView(text(label,12,MUTED,false));EditText e=new EditText(this);e.setText(value);e.setTextSize(15);e.setTextColor(TEXT);e.setSingleLine(true);e.setSaveEnabled(false);e.setInputType(InputType.TYPE_CLASS_TEXT|(secret?InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS));e.setBackground(bg(BG,8));e.setPadding(dp(10),dp(9),dp(10),dp(9));if(secret)e.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);p.addView(e,new LinearLayout.LayoutParams(-1,dp(48)));space(p,12);return e;}
    private String value(EditText e){return e.getText().toString().trim();}
    private void base(){
        root=col();root.setBackgroundColor(BG);root.setPadding(dp(18),dp(18),dp(18),dp(10));
        root.setOnApplyWindowInsetsListener((v,insets)->{int t,b;if(Build.VERSION.SDK_INT>=30){Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime());t=i.top;b=i.bottom;}else{t=insets.getSystemWindowInsetTop();b=insets.getSystemWindowInsetBottom();}v.setPadding(dp(18),t+dp(12),dp(18),b+dp(8));return insets;});setContentView(root);
        root.addView(text("MIKROTIK AGENT · HOST LOKAL",11,ACCENT,true));space(root,6);root.addView(text(activeName,22,TEXT,true));status=text(statusText,12,MUTED,false);root.addView(status);space(root,12);
        HorizontalScrollView nav=new HorizontalScrollView(this);nav.setHorizontalScrollBarEnabled(false);LinearLayout buttons=row();String[] labels={"Chat","Router","Hotspot","Aktivitas","Pengaturan"};
        for(int i=0;i<labels.length;i++){final int next=i;Button b=button(labels[i],tab==i,()->{tab=next;show();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(46));p.rightMargin=dp(6);buttons.addView(b,p);}nav.addView(buttons);root.addView(nav);space(root,12);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);body=col();scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    }
    private void show(){if(routers.length()>0||account.optBoolean("signed_in"))getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);base();if(vault==null){body.addView(text("Memuat penyimpanan…",16,TEXT,false));return;}switch(tab){case 0:chat();break;case 1:routerList();break;case 2:hotspot();break;case 3:activity();break;default:settings();}}
    private void setStatus(String s){statusText=s;if(status!=null)status.setText(s);}
    private void perform(Work work,Result then){
        if(busy)return;busy=true;try{startForegroundService(new Intent(this,AgentService.class));}catch(Exception e){busy=false;setStatus("Layanan tugas tidak dapat dimulai.");return;}setStatus("Menjalankan tugas di HP…");
        worker.execute(()->{try{JSONObject result=work.run();if(!keepLoginAlive)stopService(new Intent(this,AgentService.class));runOnUiThread(()->{if(destroyed)return;busy=false;try{then.receive(result);}catch(Exception e){setStatus("Hasil gagal ditampilkan. Buka ulang halaman.");}});}
            catch(Exception e){if(!keepLoginAlive)stopService(new Intent(this,AgentService.class));try{if(agent!=null)snapshot=agent.snapshot();}catch(Exception ignored){}runOnUiThread(()->{if(destroyed)return;busy=false;setStatus(e.getMessage()==null?"Tugas gagal. Periksa koneksi dan aktivitas.":e.getMessage());});}});
    }
    private void refreshAfter(JSONObject r){snapshot=r;statusText="Selesai. Periksa hasil di halaman ini.";show();}
    private boolean requireRouter(){if(agent==null){setStatus("Tambahkan atau pilih router dahulu.");return false;}return true;}
    private void accountCard(LinearLayout p){
        LinearLayout c=card(p);c.addView(text("Akun ChatGPT",18,TEXT,true));space(c,8);
        c.addView(text(account.optBoolean("signed_in")?"Terhubung: "+account.optString("email"):account.optString("error","Belum login ChatGPT."),13,ACCENT,false));space(c,10);
        c.addView(text("Login melalui browser OpenAI, lalu kembali dan periksa login. Kuota mengikuti akses Codex akun Boss.",13,MUTED,false));space(c,10);
        if(!account.optBoolean("signed_in")){
            addButton(c,"Login ChatGPT",true,()->{keepLoginAlive=true;startForegroundService(new Intent(this,AgentService.class));perform(()->{try{return brain.login();}catch(Exception e){keepLoginAlive=false;stopService(new Intent(this,AgentService.class));throw e;}},r->{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(r.getString("auth_url"))));setStatus("Selesaikan login browser lalu tekan Periksa login.");});});
            addButton(c,"Periksa login",false,()->perform(()->brain.account(),r->{account=r;if(r.optBoolean("signed_in")){keepLoginAlive=false;stopService(new Intent(this,AgentService.class));}statusText=r.optBoolean("signed_in")?"Login berhasil.":"Login belum selesai.";show();}));
        }else addButton(c,"Keluar akun ChatGPT",false,()->perform(()->{brain.logout();return new JSONObject();},r->{account=r;statusText="Akun ChatGPT keluar; profil router tetap tersimpan.";show();}));
    }
    private void chat(){
        if(!account.optBoolean("signed_in"))accountCard(body);
        LinearLayout welcome=card(body);welcome.addView(text("Host fungsi Android aktif",18,TEXT,true));space(welcome,8);welcome.addView(text("Agen membaca dan menyiapkan perubahan untuk router yang dipilih. Terapkan usulan melalui Aktivitas. Contoh: cek izin API, tampilkan PPPoE, buat 10 voucher profil harian, atau tinjau NAT.",14,MUTED,false));
        JSONArray messages=snapshot.optJSONArray("messages");if(messages!=null)for(int i=0;i<messages.length();i++){JSONObject m=messages.optJSONObject(i);if(m==null)continue;LinearLayout c=card(body);c.addView(text("user".equals(m.optString("role"))?"BOSS":"AGEN MIKROTIK",11,ACCENT,true));TextView t=text(m.optString("text"),15,TEXT,false);t.setTextIsSelectable(true);c.addView(t);}
        EditText input=field(body,"Perintah untuk "+activeName,"",false);input.setSingleLine(false);input.setMinLines(2);
        addButton(body,"Kirim ke agen",true,()->{if(!requireRouter())return;String message=value(input);if(message.isEmpty())return;startForegroundService(new Intent(this,AgentService.class));perform(()->{try{return agent.chat(message);}finally{stopService(new Intent(this,AgentService.class));}},this::refreshAfter);});
    }
    private void routerList(){
        LinearLayout intro=card(body);intro.addView(text("Router tersimpan · "+routers.length()+" / 50",19,TEXT,true));intro.addView(text("Profil, kredensial, percakapan, voucher, dan usulan dipisahkan per router. Fungsi langsung tetap bisa dipakai tanpa permintaan AI.",14,MUTED,false));
        addButton(body,"Tambah router",true,()->routerForm(null,new JSONObject()));
        for(int i=0;i<routers.length();i++){JSONObject r=routers.optJSONObject(i);if(r==null)continue;String id=r.optString("id");LinearLayout c=card(body);c.addView(text(r.optString("name")+(r.optBoolean("active")?" · AKTIF":""),18,TEXT,true));c.addView(text(r.optString("host")+":"+r.optInt("port")+(r.optBoolean("tls")?" · API-SSL":" · LAN/VPN"),13,MUTED,false));space(c,10);
            addButton(c,"Pilih router ini",true,()->perform(()->{vault.select(id);reload();return new JSONObject();},x->{statusText="Router aktif: "+activeName;show();}));
            addButton(c,"Edit profil",false,()->perform(()->vault.config(id),x->routerForm(id,x)));
            addButton(c,"Hapus profil",false,()->new AlertDialog.Builder(this).setTitle("Hapus profil "+r.optString("name")+"?").setMessage("Riwayat lokal router ini ikut dihapus. Konfigurasi MikroTik tidak berubah.").setNegativeButton("Batal",null).setPositiveButton("Hapus",(d,w)->perform(()->{vault.remove(id);reload();return new JSONObject();},x->{statusText="Profil dihapus.";show();})).show());
        }
        if(agent!=null){addButton(body,"Uji koneksi dan izin API",true,this::diagnostics);addButton(body,"Jelajahi & kelola sumber daya",false,()->resourcePage("hotspot_users"));}
    }
    private void routerForm(String id,JSONObject config){
        base();addButton(body,"Kembali ke daftar router",false,this::show);LinearLayout c=card(body);c.addView(text(id==null?"Router baru":"Edit router",20,TEXT,true));
        EditText name=field(c,"Nama router",config.optString("name","Router utama"),false),host=field(c,"IP / hostname (tanpa URL)",config.optString("host"),false);
        CheckBox tls=new CheckBox(this);tls.setText("API-SSL / TLS");tls.setTextColor(TEXT);tls.setChecked(config.optBoolean("tls",true));c.addView(tls);
        EditText port=field(c,"Port API",Integer.toString(config.optInt("port",8729)),false);port.setInputType(InputType.TYPE_CLASS_NUMBER);tls.setOnCheckedChangeListener((b,on)->{if(value(port).equals("8728")||value(port).equals("8729"))port.setText(on?"8729":"8728");});
        EditText user=field(c,"Username API",config.optString("username"),false),pass=field(c,id==null?"Password API":"Password baru (kosong = tetap)","",true),pin=field(c,"SHA-256 sertifikat (opsional)",config.optString("certificate_sha256"),false);
        CheckBox writes=new CheckBox(this);writes.setText("Izinkan usulan dan penerapan perubahan");writes.setTextColor(TEXT);writes.setChecked(config.optBoolean("writes_enabled",true));c.addView(writes);
        EditText minutes=field(c,"Masa berlaku usulan (menit 1–60)",Integer.toString(config.optInt("proposal_minutes",10)),false);
        EditText mikhmon=field(c,"URL Mikhmon milik Boss (opsional)",config.optString("mikhmon_url"),false);
        c.addView(text("API biasa hanya untuk LAN/VPN. Alamat publik wajib TLS. API dan read diperlukan untuk monitoring; write untuk perubahan. APK tidak menaikkan izin akun. URL Mikhmon tidak diperlukan untuk hotspot/voucher lokal.",13,MUTED,false));space(c,12);
        addButton(c,"Uji koneksi & simpan router",true,()->{
            try{JSONObject r=new JSONObject().put("name",value(name)).put("host",value(host)).put("port",Integer.parseInt(value(port))).put("username",value(user)).put("password",pass.getText().length()==0&&id!=null?config.optString("password"):pass.getText().toString()).put("tls",tls.isChecked()).put("certificate_sha256",value(pin)).put("writes_enabled",writes.isChecked()).put("proposal_minutes",Integer.parseInt(value(minutes))).put("mikhmon_url",value(mikhmon));
                if(r.getInt("proposal_minutes")<1||r.getInt("proposal_minutes")>60)throw new Exception("Masa usulan 1–60 menit.");validateMikhmon(r.optString("mikhmon_url"));LocalAgent.validateConfig(new JSONObject().put("router",r));
                perform(()->{JSONObject access=HostFunctions.access(new RouterApi(r),r);vault.saveConfig(id,r);reload();return access;},result->{pass.setText("");tab=1;statusText="Profil tersimpan. Login API dan baca terverifikasi; izin write lihat diagnostik.";show();showJson("Hasil koneksi",result);});
            }catch(Exception e){setStatus(e.getMessage());}
        });
    }
    private static void validateMikhmon(String url) throws Exception {if(url.isEmpty())return;java.net.URI u=new java.net.URI(url);if(!Arrays.asList("https","http").contains(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null)throw new Exception("URL Mikhmon harus http/https tanpa kredensial di URL.");}
    private void diagnostics(){if(!requireRouter())return;perform(()->agent.execute("check_router_access",new JSONObject()),r->{setStatus("Diagnostik selesai.");showJson("Koneksi dan izin MikroTik",r);});}
    private void showJson(String title,JSONObject result){TextView t=text(result.toString(),14,TEXT,false);try{t.setText(result.toString(2));}catch(Exception ignored){}t.setTextIsSelectable(true);ScrollView scroll=new ScrollView(this);scroll.setPadding(dp(16),dp(10),dp(16),dp(10));scroll.setBackgroundColor(BG);scroll.addView(t);new AlertDialog.Builder(this).setTitle(title).setView(scroll).setPositiveButton("Tutup",null).show();}
    private void resourcePage(String resource){
        if(!requireRouter())return;base();addButton(body,"Kembali",false,this::show);
        Spinner spinner=new Spinner(this);java.util.List<String> labels=new ArrayList<>(HostFunctions.CATALOG.keySet());ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels);spinner.setAdapter(adapter);spinner.setSelection(labels.indexOf(resource));body.addView(spinner);
        EditText filter=field(body,"Nama tepat (kosong = semua)","",false);
        addButton(body,"Baca data router",true,()->{String selected=(String)spinner.getSelectedItem();perform(()->agent.execute("read_resource",new JSONObject().put("resource",selected).put("filter_name",value(filter))),r->{setStatus("Data dibaca dari "+activeName);resourceRows(selected,r);});});
        addButton(body,"Tambah objek",false,()->changeForm((String)spinner.getSelectedItem(),"add",new JSONObject()));
        body.addView(text("Pembacaan dibatasi 100 baris tampilan. Gunakan nama tepat untuk akun tertentu. Perubahan firewall, alamat IP, rute, atau interface dapat memutus koneksi; periksa usulannya sebelum menerapkan.",13,MUTED,false));
    }
    private void resourceRows(String resource,JSONObject result) throws Exception {
        base();addButton(body,"Kembali ke sumber daya",false,()->resourcePage(resource));body.addView(text(resource+" · "+result.optInt("total")+" objek",18,TEXT,true));JSONArray rows=result.getJSONArray("rows");
        if(rows.length()==0)body.addView(text("Tidak ada data.",15,MUTED,false));
        for(int i=0;i<rows.length();i++){JSONObject item=rows.getJSONObject(i);LinearLayout c=card(body);TextView t=text(item.toString(2),13,TEXT,false);t.setTextIsSelectable(true);c.addView(t);
            HostFunctions.Resource spec=HostFunctions.get(resource);if(!spec.writable.isEmpty())addButton(c,"Edit objek",false,()->changeForm(resource,"set",item));
            if(spec.remove)addButton(c,resource.endsWith("active")?"Putus sesi (usulan)":"Hapus (usulan)",false,()->propose(resource,"remove",item.optString(".id"),new JSONObject()));
        }
    }
    private void changeForm(String resource,String action,JSONObject before){
        try{HostFunctions.Resource spec=HostFunctions.get(resource);if(action.equals("add")&&!spec.add){setStatus("Tambah tidak tersedia untuk sumber daya ini.");return;}
            base();addButton(body,"Kembali",false,()->resourcePage(resource));LinearLayout c=card(body);c.addView(text(action+" · "+resource,20,TEXT,true));c.addView(text("Isi field yang akan ditulis. Kolom kosong tidak dikirim. Untuk akun hotspot/PPP baru, password dibuat acak di HP dan ditampilkan setelah berhasil.",13,MUTED,false));
            Map<String,EditText> fields=new LinkedHashMap<>();for(String key:new TreeSet<>(spec.writable))fields.put(key,field(c,key,"set".equals(action)?before.optString(key):"",false));
            addButton(c,"Siapkan usulan",true,()->{try{JSONObject values=new JSONObject();for(String key:fields.keySet()){String val=value(fields.get(key));if(!val.isEmpty())values.put(key,val);}propose(resource,action,before.optString(".id"),values);}catch(Exception e){setStatus(e.getMessage());}});
        }catch(Exception e){setStatus(e.getMessage());}
    }
    private void propose(String resource,String action,String id,JSONObject values){perform(()->agent.execute("prepare_resource_change",new JSONObject().put("resource",resource).put("action",action).put("id",id).put("values",values)),r->{snapshot=agent.snapshot();tab=3;statusText="Usulan siap; belum diterapkan.";show();});}
    private void hotspot(){
        if(!requireRouter()){body.addView(text("Pilih router di menu Router.",16,TEXT,false));return;}
        LinearLayout intro=card(body);intro.addView(text("Hotspot & voucher lokal",20,TEXT,true));intro.addView(text("Fungsi pengelolaan hotspot berjalan langsung dari HP lewat API RouterOS. Mikhmon PHP tidak dibundel. Harga dan status terjual adalah catatan lokal, bukan laporan pembayaran otomatis.",14,MUTED,false));
        addButton(body,"Pengguna hotspot",false,()->resourcePage("hotspot_users"));addButton(body,"Profil & kecepatan hotspot",false,()->resourcePage("hotspot_profiles"));addButton(body,"Sesi aktif / putus sesi",false,()->resourcePage("hotspot_active"));
        LinearLayout form=card(body);form.addView(text("Buat voucher",18,TEXT,true));EditText profile=field(form,"Nama profil yang sudah ada","default",false),prefix=field(form,"Prefix","HS",false),count=field(form,"Jumlah (1–50)","5",false),minutes=field(form,"Durasi pemakaian (menit)","60",false),price=field(form,"Harga per voucher (Rp)","0",false);
        form.addView(text("Durasi membatasi uptime kumulatif. Ini bukan tanggal kedaluwarsa kalender. Nama dan password voucher dibuat acak, disimpan terenkripsi di HP, dan tidak dikirim ke ChatGPT.",13,MUTED,false));
        addButton(form,"Siapkan batch voucher",true,()->{try{JSONObject args=new JSONObject().put("profile",value(profile)).put("prefix",value(prefix)).put("count",Long.parseLong(value(count))).put("uptime_minutes",Long.parseLong(value(minutes))).put("price_rupiah",Long.parseLong(value(price)));perform(()->agent.execute("prepare_vouchers",args),r->{snapshot=agent.snapshot();tab=3;statusText="Voucher menunggu penerapan.";show();});}catch(Exception e){setStatus("Isi jumlah, durasi, dan harga dengan angka yang valid.");}});
        JSONArray vouchers=snapshot.optJSONArray("vouchers");if(vouchers==null)vouchers=new JSONArray();final JSONArray records=vouchers;
        long revenue=0;int sold=0;for(int i=0;i<records.length();i++)if(records.optJSONObject(i).optBoolean("sold")){sold++;revenue+=records.optJSONObject(i).optLong("price_rupiah");}
        LinearLayout summary=card(body);summary.addView(text("Kartu akses tersimpan: "+records.length(),18,TEXT,true));summary.addView(text("Ditandai terjual: "+sold+" · Nilai catatan: Rp "+revenue,14,ACCENT,false));
        if(records.length()>0){addButton(summary,"Cetak / simpan PDF kartu",true,()->VoucherPrinter.print(this,activeName,records));addButton(summary,"Ekspor kartu CSV (termasuk password)",false,()->new AlertDialog.Builder(this).setTitle("Ekspor kredensial kartu?").setMessage("Berkas CSV memuat password. Simpan hanya di lokasi yang Boss percayai.").setNegativeButton("Batal",null).setPositiveButton("Ekspor",(d,w)->{try{exportFile(voucherCsv(records).getBytes(StandardCharsets.UTF_8),"text/csv","Voucher-Mikrotik.csv");}catch(Exception e){setStatus(e.getMessage());}}).show());}
        for(int i=records.length()-1;i>=Math.max(0,records.length()-100);i--){JSONObject v=records.optJSONObject(i);LinearLayout c=card(body);c.addView(text(v.optString("name"),18,TEXT,true));c.addView(text(v.optString("kind","hotspot_users")+" · "+v.optString("profile")+" · "+(v.optBoolean("sold")?"Terjual":"Belum ditandai terjual"),13,MUTED,false));addButton(c,"Lihat kartu / password",false,()->showJson("Kartu akses · "+activeName,v));addButton(c,v.optBoolean("sold")?"Batalkan tanda terjual":"Tandai terjual",false,()->perform(()->agent.markSold(v.optString("id"),!v.optBoolean("sold")),this::refreshAfter));}
    }
    private static String csv(String s){if(s.matches("(?s)^[\\x00-\\x20]*[=+@-].*"))s="'"+s;return "\""+s.replace("\"","\"\"")+"\"";}
    private static String voucherCsv(JSONArray rows) throws Exception {StringBuilder s=new StringBuilder("name,password,profile,uptime,price_rupiah,sold\r\n");for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);for(String k:new String[]{"name","password","profile","uptime","price_rupiah","sold"})s.append(csv(r.optString(k))).append(k.equals("sold")?"\r\n":",");}return s.toString();}
    private void activity(){
        if(!requireRouter())return;JSONArray plans=snapshot.optJSONArray("plans");if(plans==null||plans.length()==0)body.addView(text("Belum ada usulan perubahan.",16,TEXT,false));
        if(plans!=null)for(int i=plans.length()-1;i>=0;i--){JSONObject p=plans.optJSONObject(i);LinearLayout c=card(body);c.addView(text(p.optString("type")+" · "+p.optString("status"),18,TEXT,true));c.addView(text("Router: "+activeName,13,ACCENT,true));
            try{JSONObject view=RouterVault.copy(p);view.remove("entries");view.remove("profile_before");view=(JSONObject)agent.redact(view);c.addView(text(view.toString(2),13,TEXT,false));}catch(Exception ignored){}
            if("pending".equals(p.optString("status"))){addButton(c,"Terapkan ke "+activeName,true,()->new AlertDialog.Builder(this).setTitle("Terapkan perubahan ke "+activeName+"?").setMessage("Perubahan konfigurasi dapat memengaruhi layanan. Pastikan target dan nilai usulan sudah benar.").setNegativeButton("Batal",null).setPositiveButton("Terapkan",(d,w)->perform(()->{agent.apply(p.optString("id"));return agent.snapshot();},this::refreshAfter)).show());addButton(c,"Batalkan usulan",false,()->perform(()->agent.cancelPlan(p.optString("id")),this::refreshAfter));}
        }
        addButton(body,"Muat ulang aktivitas",false,()->perform(()->agent.snapshot(),this::refreshAfter));addButton(body,"Bersihkan riwayat selesai",false,()->perform(()->agent.archiveFinished(),this::refreshAfter));
    }
    private void settings(){
        accountCard(body);LinearLayout c=card(body);c.addView(text("Pengaturan router & penyimpanan",19,TEXT,true));c.addView(text("Mode baca saja, masa usulan, TLS, dan URL Mikhmon diatur per profil melalui Edit router. Semua profil dan kartu akses disimpan terenkripsi. Backup tidak menyertakan sesi ChatGPT; login AI dilakukan terpisah.",14,MUTED,false));
        if(agent!=null){addButton(c,"Pengaturan router aktif",true,()->perform(()->vault.config(vault.active()),r->routerForm(vault.active(),r)));addButton(c,"Diagnostik koneksi & izin",false,this::diagnostics);addButton(c,"Buka Mikhmon yang dikonfigurasi",false,()->perform(()->vault.config(vault.active()),r->{String url=r.optString("mikhmon_url");if(url.isEmpty()){setStatus("URL Mikhmon belum diisi. Fungsi hotspot lokal tersedia tanpa Mikhmon PHP.");return;}validateMikhmon(url);startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}));}
        addButton(c,"Cadangkan semua router (terenkripsi)",true,()->passwordDialog(false));addButton(c,"Pulihkan cadangan router",false,()->passwordDialog(true));
        LinearLayout limits=card(body);limits.addView(text("Kemampuan & batas versi 0.4",18,TEXT,true));limits.addView(text("Host menyediakan monitoring, hotspot/voucher, PPP, queue, IP/DHCP/DNS, rute, firewall dan NAT sesuai daftar field. Semua penulisan perlu penerapan. Shell umum, reset/reboot, skrip RouterOS bebas, backup biner router, serta Mikhmon PHP belum diaktifkan. API router tetap menjadi penentu izin akhir.",14,MUTED,false));
        addButton(limits,"Lihat kemampuan host",false,()->{if(requireRouter())perform(()->agent.execute("host_status",new JSONObject()),r->showJson("Host fungsi APK",r));});
    }
    private void passwordDialog(boolean importing){EditText input=new EditText(this);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);input.setHint("Frasa sandi minimal 12 karakter");input.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        new AlertDialog.Builder(this).setTitle(importing?"Pulihkan cadangan":"Enkripsi cadangan").setMessage(importing?"Cadangan akan mengganti semua profil lokal. Usulan lama dinonaktifkan. Sesi ChatGPT tetap.":"Simpan frasa sandi. Tanpanya cadangan tidak dapat dibuka.").setView(input).setNegativeButton("Batal",null).setPositiveButton(importing?"Pilih berkas":"Buat cadangan",(d,w)->{
            char[] password=input.getText().toString().toCharArray();input.setText("");if(password.length<12){Arrays.fill(password,'\0');setStatus("Frasa sandi minimal 12 karakter.");return;}
            if(importing){importPassword=password;startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),42);}
            else perform(()->{try{pendingExport=VaultBackup.encode(vault.exportData(),password);return new JSONObject();}finally{Arrays.fill(password,'\0');}},r->exportFile(pendingExport,"application/octet-stream","Mikrotik-Router-Backup.masbak"));
        }).show();
    }
    private void exportFile(byte[] bytes,String mime,String name){pendingExport=bytes;exportMime=mime;exportName=name;startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType(mime).addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,name),41);}
    @Override protected void onActivityResult(int request,int result,Intent intent){super.onActivityResult(request,result,intent);
        if(result!=RESULT_OK||intent==null||intent.getData()==null){pendingExport=null;if(importPassword!=null)Arrays.fill(importPassword,'\0');importPassword=null;return;}
        Uri uri=intent.getData();if(request==41){byte[] bytes=pendingExport;pendingExport=null;if(bytes==null){setStatus("Ekspor terputus; ulangi dari pengaturan.");return;}perform(()->{try(OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new IOException("Berkas tidak dapat ditulis.");out.write(bytes);}return new JSONObject();},r->setStatus("Berkas berhasil disimpan."));}
        if(request==42){char[] password=importPassword;importPassword=null;if(password==null){setStatus("Pemulihan terputus; ulangi dari pengaturan.");return;}perform(()->{try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Berkas tidak dapat dibaca.");ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] block=new byte[8192];int n;while((n=in.read(block))!=-1){if(bytes.size()+n>12000000)throw new IOException("Cadangan terlalu besar.");bytes.write(block,0,n);}JSONObject data;try{data=VaultBackup.decode(bytes.toByteArray(),password);}catch(Exception e){throw new IOException("Cadangan atau frasa sandi tidak valid.");}vault.importData(data);reload();return new JSONObject();}finally{Arrays.fill(password,'\0');}},r->{statusText="Cadangan dipulihkan. Periksa router aktif sebelum bekerja.";show();});}
    }
    @Override protected void onDestroy(){destroyed=true;pendingExport=null;if(importPassword!=null)Arrays.fill(importPassword,'\0');super.onDestroy();}
}
