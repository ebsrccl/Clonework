package id.mas.agent;

import org.json.*;
import java.io.IOException;
import java.util.UUID;

/** Encrypted backing store owner. Stores are bound to an immutable router id. */
final class RouterVault {
    private final LocalAgent.Store disk;
    private JSONObject data;
    RouterVault(LocalAgent.Store disk) throws Exception {
        this.disk = disk;
        JSONObject old = disk.read();
        if (old != null && old.optInt("schema") == 4) data = copy(old);
        else {
            data = new JSONObject().put("schema",4).put("routers",new JSONObject()).put("settings",new JSONObject());
            if (old != null && old.has("router")) {
                String id=UUID.randomUUID().toString(); old.getJSONObject("router").put("id",id);
                data.getJSONObject("routers").put(id,old); data.put("active",id);
            }
            disk.save(copy(data));
        }
    }
    static java.util.List<String> keys(JSONObject o) { java.util.List<String> k=new java.util.ArrayList<>(); o.keys().forEachRemaining(k::add); return k; }
    static JSONObject copy(JSONObject o) throws Exception { return new JSONObject(o.toString()); }
    private void commit(JSONObject next) throws Exception { disk.save(copy(next)); data=next; }
    synchronized String active() { return data.optString("active",""); }
    synchronized JSONObject settings() throws Exception { return copy(data.getJSONObject("settings")); }
    synchronized void settings(JSONObject settings) throws Exception {
        int minutes=settings.optInt("proposal_minutes",10);
        if(minutes<1||minutes>60) throw new IOException("Masa usulan harus 1–60 menit.");
        JSONObject next=copy(data); next.put("settings",new JSONObject().put("proposal_minutes",minutes).put("writes_enabled",settings.optBoolean("writes_enabled",true)));
        commit(next);
    }
    synchronized JSONArray list() throws Exception {
        JSONArray result=new JSONArray(); JSONObject routers=data.getJSONObject("routers");
        for(String id:keys(routers)) { JSONObject r=routers.getJSONObject(id).getJSONObject("router"); result.put(new JSONObject().put("id",id).put("name",r.optString("name")).put("host",r.optString("host")).put("port",r.optInt("port")).put("tls",r.optBoolean("tls")).put("active",id.equals(active()))); }
        return result;
    }
    synchronized JSONObject config(String id) throws Exception { return copy(entry(id).getJSONObject("router")); }
    private JSONObject entry(String id) throws Exception {
        JSONObject entry=data.getJSONObject("routers").optJSONObject(id);
        if(entry==null) throw new IOException("Profil router tidak ditemukan."); return entry;
    }
    synchronized String saveConfig(String id, JSONObject config) throws Exception {
        JSONObject r=copy(config); LocalAgent.validateConfig(new JSONObject().put("router",r));
        JSONObject next=copy(data); JSONObject routers=next.getJSONObject("routers");
        if(id==null||id.isEmpty()) { if(routers.length()>=50) throw new IOException("Maksimal 50 router."); id=UUID.randomUUID().toString(); }
        else entry(id);
        r.put("id",id); JSONObject state=routers.optJSONObject(id);
        if(state==null) state=new JSONObject().put("messages",new JSONArray()).put("plans",new JSONArray());
        // Changing connection/credentials invalidates pending proposals for this identity.
        JSONArray plans=state.optJSONArray("plans");
        if(plans!=null) for(int i=0;i<plans.length();i++) if("pending".equals(plans.getJSONObject(i).optString("status"))) plans.getJSONObject(i).put("status","stale");
        state.put("router",r); routers.put(id,state); next.put("active",id); commit(next); return id;
    }
    synchronized void select(String id) throws Exception { entry(id); JSONObject next=copy(data); next.put("active",id); commit(next); }
    synchronized void remove(String id) throws Exception {
        entry(id); JSONObject next=copy(data); JSONObject routers=next.getJSONObject("routers"); routers.remove(id);
        if(id.equals(active())) next.put("active",routers.length()==0?"":routers.keys().next()); commit(next);
    }
    synchronized JSONObject exportData() throws Exception { return copy(data); }
    synchronized void importData(JSONObject imported) throws Exception {
        if(imported.optInt("schema")!=4) throw new IOException("Format cadangan tidak didukung.");
        JSONObject routers=imported.getJSONObject("routers"); if(routers.length()>50) throw new IOException("Terlalu banyak router.");
        for(String id:keys(routers)) {
            if(!id.matches("[a-zA-Z0-9-]{1,64}")) throw new IOException("ID router tidak valid.");
            JSONObject state=routers.getJSONObject(id); LocalAgent.validateConfig(state); state.getJSONObject("router").put("id",id);
            JSONArray plans=state.optJSONArray("plans"); if(plans!=null) for(int i=0;i<plans.length();i++) if("pending".equals(plans.getJSONObject(i).optString("status"))) plans.getJSONObject(i).put("status","stale");
        }
        if(!imported.optString("active").isEmpty()&&!routers.has(imported.getString("active"))) throw new IOException("Router aktif tidak valid.");
        if(imported.optJSONObject("settings")==null) imported.put("settings",new JSONObject()); commit(copy(imported));
    }
    LocalAgent.Store forRouter(final String id) throws Exception {
        synchronized(this) { entry(id); }
        return new LocalAgent.Store() {
            public JSONObject read() throws Exception { synchronized(RouterVault.this){ return copy(entry(id)); } }
            public void save(JSONObject state) throws Exception { synchronized(RouterVault.this){
                JSONObject current=entry(id);
                if(!current.getJSONObject("router").toString().equals(state.getJSONObject("router").toString())) throw new IOException("Koneksi berubah; buka ulang router.");
                JSONObject next=copy(data); next.getJSONObject("routers").put(id,copy(state)); commit(next);
            } }
            public void clear() throws Exception { RouterVault.this.remove(id); }
        };
    }
}
