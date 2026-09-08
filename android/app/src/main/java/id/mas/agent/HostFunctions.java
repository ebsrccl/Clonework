package id.mas.agent;

import org.json.*;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;

/** Local function host. Resource names and fields map to fixed RouterOS API commands. */
final class HostFunctions {
    static final class Resource {
        final String name,path; final String[] fields; final Set<String> writable; final boolean add,remove;
        Resource(String name,String path,String fields,String writes,boolean add,boolean remove) {
            this.name=name;this.path=path;this.fields=fields.split(",");this.writable=new HashSet<>(Arrays.asList(writes.isEmpty()?new String[0]:writes.split(",")));this.add=add;this.remove=remove;
        }
    }
    static final LinkedHashMap<String,Resource> CATALOG=new LinkedHashMap<>();
    static void resource(String name,String path,String fields,String writes,boolean add,boolean remove) { CATALOG.put(name,new Resource(name,path,fields,writes,add,remove)); }
    static {
        resource("hotspot_users","/ip/hotspot/user",".id,name,profile,server,mac-address,limit-uptime,limit-bytes-total,disabled,comment,uptime,bytes-in,bytes-out","name,profile,server,mac-address,limit-uptime,limit-bytes-total,disabled,comment",true,true);
        resource("hotspot_profiles","/ip/hotspot/user/profile",".id,name,rate-limit,shared-users,session-timeout,idle-timeout,keepalive-timeout,add-mac-cookie","name,rate-limit,shared-users,session-timeout,idle-timeout,keepalive-timeout,add-mac-cookie",true,true);
        resource("hotspot_active","/ip/hotspot/active",".id,user,address,mac-address,server,uptime,session-time-left,bytes-in,bytes-out","",false,true);
        resource("hotspot_hosts","/ip/hotspot/host",".id,address,mac-address,to-address,server,authorized,bypassed","",false,false);
        resource("hotspot_servers","/ip/hotspot",".id,name,interface,address-pool,profile,disabled","disabled",false,false);
        resource("hotspot_bindings","/ip/hotspot/ip-binding",".id,mac-address,address,to-address,server,type,comment,disabled","mac-address,address,to-address,server,type,comment,disabled",true,true);
        resource("queues","/queue/simple",".id,name,target,max-limit,limit-at,parent,disabled,dynamic,comment","name,target,max-limit,limit-at,parent,disabled,comment",true,true);
        resource("interfaces","/interface",".id,name,type,mtu,running,disabled,comment,rx-byte,tx-byte","disabled,comment",false,false);
        resource("ip_addresses","/ip/address",".id,address,network,interface,disabled,dynamic,comment","address,interface,disabled,comment",true,true);
        resource("dhcp_leases","/ip/dhcp-server/lease",".id,address,mac-address,host-name,server,status,dynamic,disabled,comment","comment,disabled",false,false);
        resource("dhcp_servers","/ip/dhcp-server",".id,name,interface,address-pool,lease-time,disabled","lease-time,disabled",false,false);
        resource("dns_static","/ip/dns/static",".id,name,address,type,ttl,disabled,comment","name,address,ttl,disabled,comment",true,true);
        resource("routes","/ip/route",".id,dst-address,gateway,distance,disabled,dynamic,active,comment","dst-address,gateway,distance,disabled,comment",true,true);
        resource("ppp_profiles","/ppp/profile",".id,name,local-address,remote-address,rate-limit,only-one,change-tcp-mss","name,local-address,remote-address,rate-limit,only-one,change-tcp-mss",true,true);
        resource("ppp_users","/ppp/secret",".id,name,service,profile,local-address,remote-address,disabled,comment","name,service,profile,local-address,remote-address,disabled,comment",true,true);
        resource("ppp_active","/ppp/active",".id,name,service,caller-id,address,uptime","",false,true);
        resource("firewall_filter","/ip/firewall/filter",".id,chain,action,src-address,dst-address,protocol,src-port,dst-port,in-interface,out-interface,connection-state,disabled,dynamic,comment","chain,action,src-address,dst-address,protocol,src-port,dst-port,in-interface,out-interface,connection-state,disabled,comment",true,true);
        resource("firewall_nat","/ip/firewall/nat",".id,chain,action,src-address,dst-address,protocol,dst-port,in-interface,out-interface,to-addresses,to-ports,disabled,dynamic,comment","chain,action,src-address,dst-address,protocol,dst-port,in-interface,out-interface,to-addresses,to-ports,disabled,comment",true,true);
        resource("address_lists","/ip/firewall/address-list",".id,list,address,timeout,disabled,dynamic,comment","list,address,timeout,disabled,comment",true,true);
        resource("logs","/log",".id,time,topics,message","",false,false);
    }
    static JSONArray names() { return new JSONArray(CATALOG.keySet()); }
    static JSONObject str() throws Exception { return new JSONObject().put("type","string"); }
    static void addTools(JSONArray tools) throws Exception {
        tools.put(LocalAgent.function("host_status","Kemampuan host Android dan identitas router aktif.",new JSONObject()));
        tools.put(LocalAgent.function("check_router_access","Uji baca API dan inspeksi izin akun bila router mengizinkannya; tidak mengubah izin.",new JSONObject()));
        tools.put(LocalAgent.function("read_resource","Baca sumber daya router aktif. filter_name kosong untuk semua atau nama tepat untuk filter.",new JSONObject().put("resource",str().put("enum",names())).put("filter_name",str())));
        JSONObject values=new JSONObject(); Set<String> keys=new TreeSet<>(); for(Resource r:CATALOG.values()) keys.addAll(r.writable);
        for(String key:keys) values.put(key,str());
        tools.put(LocalAgent.function("prepare_resource_change","Siapkan add/set/remove. ID wajib untuk set/remove; kosong untuk add. Belum menulis, tinjau di Aktivitas. Kredensial akun baru dibuat lokal.",new JSONObject().put("resource",str().put("enum",names())).put("action",str().put("enum",new JSONArray(Arrays.asList("add","set","remove")))).put("id",str()).put("values",new JSONObject().put("type","object").put("properties",values).put("additionalProperties",false))));
        tools.put(LocalAgent.function("prepare_vouchers","Siapkan 1–50 voucher hotspot lokal. Uptime adalah durasi pemakaian kumulatif, bukan tanggal kedaluwarsa. Harga untuk catatan lokal; belum dianggap terjual.",new JSONObject().put("profile",str()).put("prefix",str()).put("count",new JSONObject().put("type","integer").put("minimum",1).put("maximum",50)).put("uptime_minutes",new JSONObject().put("type","integer").put("minimum",1).put("maximum",525600)).put("price_rupiah",new JSONObject().put("type","integer").put("minimum",0).put("maximum",100000000))));
    }
    static boolean handles(String name) { return Arrays.asList("host_status","check_router_access","read_resource","prepare_resource_change","prepare_vouchers").contains(name); }
    static Resource get(String name) throws Exception { Resource r=CATALOG.get(name);if(r==null) throw new IOException("Sumber daya tidak tersedia.");return r; }
    static JSONArray rows(LocalAgent a,Resource r) throws Exception { return a.router.read(r.path,r.fields); }
    static JSONObject execute(LocalAgent a,String name,JSONObject args) throws Exception {
        if(name.equals("host_status")) return new JSONObject().put("host","android_routeros_api").put("router",a.profileSummary()).put("resources",names()).put("writes_require_review",true).put("mikhmon_php_bundled",false);
        if(name.equals("check_router_access")) return access(a.router,a.state.getJSONObject("router"));
        if(name.equals("read_resource")) {
            Resource r=get(args.getString("resource"));String filter=args.getString("filter_name"); if(filter.length()>128) throw new IOException("Filter terlalu panjang.");
            JSONArray data=filter.isEmpty()?rows(a,r):a.router.lookup(r.path,r.fields,"name",filter),shown=new JSONArray();
            for(int i=0;i<Math.min(100,data.length());i++) shown.put(data.get(i));
            return new JSONObject().put("router",a.profileSummary()).put("resource",r.name).put("sampled_at_ms",System.currentTimeMillis()).put("rows",shown).put("total",data.length()).put("truncated",data.length()>100);
        }
        if(!a.state.getJSONObject("router").optBoolean("writes_enabled",true)) throw new IOException("Mode baca saja aktif. Ubah di pengaturan router.");
        if(name.equals("prepare_vouchers")) return vouchers(a,args);
        Resource r=get(args.getString("resource"));String action=args.getString("action"),id=args.getString("id");JSONObject values=args.getJSONObject("values"); validate(r,action,id,values);
        JSONObject before=new JSONObject();
        if(!action.equals("add")) {
            before=LocalAgent.find(rows(a,r),id);if(before==null) throw new IOException("Objek tidak ditemukan.");
            if("true".equals(before.optString("dynamic"))) throw new IOException("Objek dinamis tidak dapat diubah.");
        } else if(values.has("name") && a.router.lookup(r.path,r.fields,"name",values.getString("name")).length()>0) throw new IOException("Nama sudah ada.");
        JSONObject plan=base(a,"resource_change").put("resource",r.name).put("action",action).put("target_id",id).put("before",before).put("values",RouterVault.copy(values));
        if(action.equals("add")&&(r.name.equals("hotspot_users")||r.name.equals("ppp_users"))) plan.getJSONObject("values").put("password",randomCode(14));
        addPlan(a,plan); return publicPlan(a,plan);
    }
    static JSONObject access(LocalAgent.Router router,JSONObject config) throws Exception {
        long start=System.currentTimeMillis(); JSONArray identity=router.read("/system/identity",new String[]{"name"});
        JSONObject result=new JSONObject().put("api_login","ok").put("read","verified").put("transport",config.optBoolean("tls")?"API-SSL":"API LAN/VPN").put("identity",identity).put("latency_ms",System.currentTimeMillis()-start).put("write","unknown");
        try {
            JSONArray users=router.lookup("/user",new String[]{"name","group","disabled"},"name",config.getString("username"));
            if(users.length()!=1) throw new IOException();String group=users.getJSONObject(0).getString("group");
            JSONArray groups=router.lookup("/user/group",new String[]{"name","policy"},"name",group);if(groups.length()!=1) throw new IOException();
            Set<String> policy=new HashSet<>(Arrays.asList(groups.getJSONObject(0).getString("policy").split(",")));
            result.put("group",group).put("policy",new JSONArray(policy)).put("write",policy.contains("write")?"policy_granted_not_operation_tested":"denied_by_policy");
        } catch(Exception e) { result.put("permission_inspection","Tidak dapat membaca grup akun. Izin write belum diketahui; tidak diuji dengan perubahan percobaan."); }
        result.put("required","api + read untuk monitoring; write untuk perubahan. Akun aplikasi tidak menaikkan izin router secara otomatis."); return result;
    }
    static void validate(Resource r,String action,String id,JSONObject values) throws Exception {
        if(!Arrays.asList("add","set","remove").contains(action)) throw new IOException("Aksi tidak valid.");
        if(action.equals("add")) { if(!r.add||!id.isEmpty()) throw new IOException("Tambah tidak tersedia."); }
        else if(!id.matches("\\*[0-9a-fA-F]+")) throw new IOException("ID objek harus ID API RouterOS.");
        if(action.equals("remove")) { if(!r.remove||values.length()!=0) throw new IOException("Hapus tidak tersedia atau parameter berlebih.");return; }
        if(values.length()==0) throw new IOException("Perubahan kosong.");
        for(String key:RouterVault.keys(values)) {
            Object raw=values.get(key);if(!r.writable.contains(key)||!(raw instanceof String)) throw new IOException("Field tidak diizinkan: "+key);
            String value=(String)raw;if(value.length()>256||value.matches("(?s).*[\\x00-\\x1f].*")) throw new IOException("Nilai field tidak valid.");
            if(key.equals("disabled")&&!Arrays.asList("true","false").contains(value)) throw new IOException("disabled harus true/false.");
            if(key.equals("name")&&value.trim().isEmpty()) throw new IOException("Nama wajib diisi.");
            if(key.equals("shared-users")&&(!value.matches("[0-9]{1,3}")||Integer.parseInt(value)<1)) throw new IOException("Shared users 1–999.");
            if(key.equals("action")&&!Arrays.asList("accept","drop","reject","log","masquerade","src-nat","dst-nat","redirect","return").contains(value)) throw new IOException("Aksi firewall ini belum didukung.");
        }
        if(action.equals("add")) {
            if(r.writable.contains("name")&&!values.has("name")) throw new IOException("Nama diperlukan.");
            if(r.name.equals("hotspot_users")&&!values.has("profile")) throw new IOException("Profil hotspot diperlukan.");
            if(r.name.startsWith("firewall_")&&(!values.has("chain")||!values.has("action"))) throw new IOException("Chain dan action diperlukan.");
        }
    }
    static JSONObject base(LocalAgent a,String type) throws Exception {
        int minutes=Math.max(1,Math.min(60,a.state.getJSONObject("router").optInt("proposal_minutes",10)));
        return new JSONObject().put("id",UUID.randomUUID().toString()).put("router_id",a.state.getJSONObject("router").optString("id")).put("router_name",a.state.getJSONObject("router").optString("name")).put("type",type).put("status","pending").put("created_at",System.currentTimeMillis()).put("expires_at",System.currentTimeMillis()+minutes*60000L);
    }
    static void addPlan(LocalAgent a,JSONObject plan) throws Exception {
        JSONArray plans=LocalAgent.copyArray(a.state.optJSONArray("plans")); if(plans.length()>=200) throw new IOException("Arsipkan riwayat usulan dahulu."); plans.put(plan);
        JSONObject old=RouterVault.copy(a.state);a.state.put("plans",plans);try{a.persist();}catch(Exception e){a.state=old;throw e;}
    }
    static JSONObject publicPlan(LocalAgent a,JSONObject p) throws Exception {
        JSONObject visible=(JSONObject)a.redact(p);visible.remove("entries");
        return new JSONObject().put("status","pending").put("plan",visible).put("message","Tinjau router dan perubahan di Aktivitas lalu tekan Terapkan.");
    }
    private static final SecureRandom RANDOM=new SecureRandom();
    static String randomCode(int count) { String alphabet="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";StringBuilder s=new StringBuilder();while(s.length()<count)s.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));return s.toString(); }
    static long integer(JSONObject args,String key,long min,long max) throws Exception {
        Object o=args.get(key);if(!(o instanceof Number))throw new IOException("Angka diperlukan."); double n=((Number)o).doubleValue();if(!Double.isFinite(n)||n!=Math.floor(n)||n<min||n>max)throw new IOException("Nilai "+key+" di luar batas.");return (long)n;
    }
    static JSONObject vouchers(LocalAgent a,JSONObject args) throws Exception {
        int count=(int)integer(args,"count",1,50);long minutes=integer(args,"uptime_minutes",1,525600),price=integer(args,"price_rupiah",0,100000000);
        String prefix=args.getString("prefix"),profile=args.getString("profile");if(!prefix.matches("[A-Za-z0-9_-]{0,16}")||profile.length()>128)throw new IOException("Prefix/profil tidak valid.");
        JSONArray profiles=a.router.lookup(get("hotspot_profiles").path,get("hotspot_profiles").fields,"name",profile);if(profiles.length()!=1)throw new IOException("Profil hotspot harus sudah tersedia.");
        JSONArray entries=new JSONArray(); for(int i=0;i<count;i++) entries.put(new JSONObject().put("name",prefix+randomCode(10)).put("password",randomCode(12)).put("profile",profile).put("limit-uptime",minutes+"m").put("status","pending"));
        JSONObject plan=base(a,"voucher_batch").put("before",new JSONObject()).put("profile",profile).put("profile_before",stable(profiles.getJSONObject(0))).put("count",count).put("price_rupiah",price).put("uptime_minutes",minutes).put("entries",entries);
        addPlan(a,plan);return publicPlan(a,plan);
    }
    static JSONObject stable(JSONObject row) throws Exception {
        JSONObject o=RouterVault.copy(row);for(String k:new String[]{"uptime","bytes-in","bytes-out","rx-byte","tx-byte","running","active","status","session-time-left","time","topics","message"})o.remove(k);return o;
    }
    static boolean same(JSONObject x,JSONObject y) throws Exception {
        JSONObject a=stable(x),b=stable(y);if(a.length()!=b.length())return false;
        for(String k:RouterVault.keys(a))if(!a.optString(k).equals(b.optString(k)))return false;return true;
    }
    static boolean matches(JSONObject row,JSONObject values) throws Exception {
        if(row==null)return false;for(String k:RouterVault.keys(values)) { if(k.equals("password"))continue;String actual=row.optString(k),expected=values.getString(k);
            if(k.equals("max-limit")||k.equals("limit-at")) { if(!Objects.equals(LocalAgent.normalizeLimit(actual),LocalAgent.normalizeLimit(expected)))return false; }
            else if(k.equals("limit-uptime")) { long left=duration(actual),right=duration(expected);if(left<0||right<0){if(!actual.equals(expected))return false;}else if(left!=right)return false; }
            else if(!actual.equals(expected)) return false;
        }return true;
    }
    static long duration(String value) {
        if(value.matches("[0-9]+"))return Long.parseLong(value);
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d+)([wdhms])").matcher(value);long seconds=0;int end=0;
        while(m.find()){if(m.start()!=end)return -1;long unit=m.group(2).equals("w")?604800:m.group(2).equals("d")?86400:m.group(2).equals("h")?3600:m.group(2).equals("m")?60:1;seconds+=Long.parseLong(m.group(1))*unit;end=m.end();}return end==value.length()&&end>0?seconds:-1;
    }
    static JSONObject apply(LocalAgent a,JSONObject p) throws Exception {
        if("voucher_batch".equals(p.getString("type")))return applyVouchers(a,p);
        if(!"resource_change".equals(p.getString("type")))throw new IOException("Jenis usulan tidak dikenal.");
        Resource r=get(p.getString("resource"));String action=p.getString("action");JSONObject values=p.getJSONObject("values"),toValidate=RouterVault.copy(values);toValidate.remove("password");validate(r,action,p.getString("target_id"),toValidate);
        if(!action.equals("add")) {
            JSONObject now=LocalAgent.find(rows(a,r),p.getString("target_id"));if(now==null||!same(now,p.getJSONObject("before"))){p.put("status","stale");a.persist();return p;}
        } else if(values.has("name")&&a.router.lookup(r.path,r.fields,"name",values.getString("name")).length()>0){p.put("status","stale");a.persist();return p;}
        Set<String> existing=new HashSet<>();
        if(action.equals("add")&&!values.has("name")) { JSONArray current=rows(a,r);for(int i=0;i<current.length();i++)existing.add(current.getJSONObject(i).getString(".id")); }
        p.put("status","applying");a.persist();
        try {
            JSONObject send=RouterVault.copy(values);if(!action.equals("add"))send.put(".id",p.getString("target_id"));
            a.router.mutate(r.path,action,send);
            JSONObject observed=null;
            if(action.equals("add")) {
                JSONArray all=values.has("name")?a.router.lookup(r.path,r.fields,"name",values.getString("name")):rows(a,r);
                int matches=0;for(int i=0;i<all.length();i++)if(!existing.contains(all.getJSONObject(i).optString(".id"))&&matches(all.getJSONObject(i),values)){observed=all.getJSONObject(i);matches++;}if(matches!=1)observed=null;
            }else observed=LocalAgent.find(rows(a,r),p.getString("target_id"));
            boolean ok=action.equals("remove")?observed==null:matches(observed,values);
            p.put("status",ok?"verified":"unverified");
            if(ok&&values.has("password"))record(a,values,p.optLong("price_rupiah"),p.getString("id"),r.name);
        }catch(Exception e){p.put("status","unknown");}
        a.persist();return p;
    }
    static void record(LocalAgent a,JSONObject values,long price,String batch,String kind) throws Exception {
        JSONArray records=a.state.optJSONArray("vouchers");if(records==null)records=new JSONArray();
        records.put(new JSONObject().put("id",UUID.randomUUID().toString()).put("name",values.getString("name")).put("password",values.getString("password")).put("profile",values.optString("profile")).put("uptime",values.optString("limit-uptime")).put("price_rupiah",price).put("sold",false).put("created_at",System.currentTimeMillis()).put("batch_id",batch).put("kind",kind));a.state.put("vouchers",records);
    }
    static JSONObject applyVouchers(LocalAgent a,JSONObject p) throws Exception {
        long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.MINUTES.toNanos(5);
        Resource profiles=get("hotspot_profiles"),users=get("hotspot_users");JSONArray profile=a.router.lookup(profiles.path,profiles.fields,"name",p.getString("profile"));
        if(profile.length()!=1||!same(profile.getJSONObject(0),p.getJSONObject("profile_before"))){p.put("status","stale");a.persist();return p;}
        JSONArray entries=p.getJSONArray("entries");
        for(int i=0;i<entries.length();i++){if(System.nanoTime()>deadline)throw new IOException("Waktu pemeriksaan batch habis; belum ada penulisan.");if(a.router.lookup(users.path,users.fields,"name",entries.getJSONObject(i).getString("name")).length()>0){p.put("status","stale");a.persist();return p;}}
        p.put("status","applying");a.persist();int verified=0;
        for(int i=0;i<entries.length();i++) {
            if(System.nanoTime()>deadline){p.put("status","partial").put("verified_count",verified);a.persist();return p;}
            JSONObject item=entries.getJSONObject(i);item.put("status","applying");a.persist();
            try {
                JSONObject values=RouterVault.copy(item);values.remove("status");values.put("comment","MAS4:"+p.getString("id"));
                a.router.mutate(users.path,"add",values);JSONArray observed=a.router.lookup(users.path,users.fields,"name",values.getString("name"));
                if(observed.length()!=1||!matches(observed.getJSONObject(0),values))throw new IOException("Belum terverifikasi");
                item.put("status","verified");record(a,values,p.getLong("price_rupiah"),p.getString("id"),"hotspot_users");verified++;a.persist();
            }catch(Exception e){item.put("status","unknown");p.put("status",verified>0?"partial":"unknown").put("verified_count",verified);a.persist();return p;}
        }
        p.put("status","verified").put("verified_count",verified);a.persist();return p;
    }
}
