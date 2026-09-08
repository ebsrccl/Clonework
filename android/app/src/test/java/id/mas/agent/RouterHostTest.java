package id.mas.agent;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import java.io.*;
import java.util.*;

public class RouterHostTest {
    static class Memory implements LocalAgent.Store {
        JSONObject data; boolean fail;
        public JSONObject read() throws Exception {return data==null?null:RouterVault.copy(data);}
        public void save(JSONObject next) throws Exception {if(fail)throw new IOException("disk full");data=RouterVault.copy(next);}
        public void clear(){data=null;}
    }
    static JSONObject config(String name) throws Exception {JSONObject c=LocalAgentTest.profile().getJSONObject("router");c.put("name",name);return c;}
    static LocalAgent.Brain noBrain=(m,h,t)->"test";
    @Test public void migratesLegacyAndBindsEachStoreToItsRouter() throws Exception {
        Memory disk=new Memory();disk.data=LocalAgentTest.profile().put("messages",new JSONArray().put("old message"));RouterVault v=new RouterVault(disk);
        String first=v.active();assertEquals(1,v.list().length());assertEquals("old message",v.forRouter(first).read().getJSONArray("messages").getString(0));
        String second=v.saveConfig(null,config("Branch B"));LocalAgent.Store bound=v.forRouter(first);
        JSONObject a=bound.read();a.put("plans",new JSONArray().put(new JSONObject().put("status","pending")));bound.save(a);
        assertEquals(second,v.active());assertEquals(0,v.forRouter(second).read().getJSONArray("plans").length());
        v.select(first);assertEquals(1,v.forRouter(first).read().getJSONArray("plans").length());
        v.remove(first);assertThrows(IOException.class,bound::read);assertEquals(second,v.active());
    }
    @Test public void editedProfileRejectsStaleWorkerAndInvalidatesProposals() throws Exception {
        RouterVault v=new RouterVault(new Memory());String id=v.saveConfig(null,config("A"));LocalAgent.Store bound=v.forRouter(id);JSONObject old=bound.read();old.put("plans",new JSONArray().put(new JSONObject().put("status","pending")));bound.save(old);
        JSONObject changed=config("A");changed.put("host","192.168.88.2");v.saveConfig(id,changed);
        assertEquals("stale",bound.read().getJSONArray("plans").getJSONObject(0).getString("status"));assertThrows(IOException.class,()->bound.save(old));
    }
    @Test public void failedVaultWriteDoesNotChangeSelection() throws Exception {
        Memory disk=new Memory();RouterVault v=new RouterVault(disk);String first=v.saveConfig(null,config("A"));String second=v.saveConfig(null,config("B"));disk.fail=true;
        assertThrows(IOException.class,()->v.select(first));assertEquals(second,v.active());
    }
    @Test public void encryptedBackupRoundTripsAndRejectsWrongPasswordOrTampering() throws Exception {
        char[] key="long-secret-passphrase".toCharArray();JSONObject original=new JSONObject().put("secret","router password");byte[] bytes=VaultBackup.encode(original,key);
        assertFalse(new String(bytes,java.nio.charset.StandardCharsets.UTF_8).contains("router password"));assertEquals("router password",VaultBackup.decode(bytes,key).getString("secret"));
        assertThrows(Exception.class,()->VaultBackup.decode(bytes,"wrong-secret-passphrase".toCharArray()));
        JSONObject envelope=new JSONObject(new String(bytes,java.nio.charset.StandardCharsets.UTF_8));byte[] payload=java.util.Base64.getDecoder().decode(envelope.getString("data"));payload[0]^=1;envelope.put("data",java.util.Base64.getEncoder().encodeToString(payload));
        assertThrows(Exception.class,()->VaultBackup.decode(envelope.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8),key));
    }
    static class Router implements LocalAgent.Router {
        final Map<String,JSONArray> table=new HashMap<>();int mutations;int failAt=-1;
        Router() throws Exception {table.put("/ip/hotspot/user/profile",new JSONArray().put(new JSONObject().put(".id","*1").put("name","daily").put("rate-limit","1M/2M")));table.put("/ip/hotspot/user",new JSONArray());}
        public JSONArray read(String path,String[] fields) throws Exception {return table.containsKey(path)?new JSONArray(table.get(path).toString()):new JSONArray();}
        public void setQueue(String id,String limit){throw new AssertionError();}
        public void mutate(String path,String action,JSONObject args) throws Exception {
            mutations++;if(mutations==failAt)throw new IOException("connection lost");JSONArray data=table.computeIfAbsent(path,k->new JSONArray());
            if(action.equals("add")){JSONObject r=RouterVault.copy(args);r.remove("password");r.put(".id","*"+Integer.toHexString(mutations+16));data.put(r);}
            else if(action.equals("set")){JSONObject r=LocalAgent.find(data,args.getString(".id"));for(String k:RouterVault.keys(args))r.put(k,args.get(k));}
            else if(action.equals("remove")){JSONArray next=new JSONArray();for(int i=0;i<data.length();i++)if(!args.getString(".id").equals(data.getJSONObject(i).optString(".id")))next.put(data.get(i));table.put(path,next);}
        }
    }
    static Memory profile() throws Exception {Memory m=new Memory();m.data=LocalAgentTest.profile().put("plans",new JSONArray());m.data.getJSONObject("router").put("id","router-A");return m;}
    static JSONObject voucherArgs(int count) throws Exception {return new JSONObject().put("profile","daily").put("prefix","HS").put("count",count).put("uptime_minutes",60).put("price_rupiah",5000);}
    @Test public void voucherSecretsStayLocalAndBatchRequiresApplicationApply() throws Exception {
        Memory m=profile();Router r=new Router();LocalAgent a=new LocalAgent(m,r,noBrain);
        JSONObject proposal=a.execute("prepare_vouchers",voucherArgs(3));assertEquals(0,r.mutations);assertFalse(proposal.toString().contains("entries"));
        String id=proposal.getJSONObject("plan").getString("id");assertEquals("verified",a.apply(id).getString("status"));assertEquals(3,r.mutations);a.apply(id);assertEquals(3,r.mutations);
        JSONArray cards=a.snapshot().getJSONArray("vouchers");assertEquals(3,cards.length());assertEquals(12,cards.getJSONObject(0).getString("password").length());assertFalse(cards.getJSONObject(0).getBoolean("sold"));
        a.markSold(cards.getJSONObject(0).getString("id"),true);assertTrue(a.snapshot().getJSONArray("vouchers").getJSONObject(0).getBoolean("sold"));
    }
    @Test public void partialVoucherBatchNeverAutomaticallyRetries() throws Exception {
        Memory m=profile();Router r=new Router();r.failAt=2;LocalAgent a=new LocalAgent(m,r,noBrain);String id=a.execute("prepare_vouchers",voucherArgs(4)).getJSONObject("plan").getString("id");
        assertEquals("partial",a.apply(id).getString("status"));assertEquals(2,r.mutations);assertEquals(1,a.snapshot().getJSONArray("vouchers").length());
        new LocalAgent(m,r,noBrain).apply(id);assertEquals(2,r.mutations);
    }
    @Test public void profileChangeAndReadOnlyModePreventVoucherWrites() throws Exception {
        Memory m=profile();Router r=new Router();LocalAgent a=new LocalAgent(m,r,noBrain);String id=a.execute("prepare_vouchers",voucherArgs(2)).getJSONObject("plan").getString("id");
        r.table.get("/ip/hotspot/user/profile").getJSONObject(0).put("rate-limit","3M/4M");assertEquals("stale",a.apply(id).getString("status"));assertEquals(0,r.mutations);
        m.data.getJSONObject("router").put("writes_enabled",false);LocalAgent locked=new LocalAgent(m,r,noBrain);assertThrows(IOException.class,()->locked.execute("prepare_vouchers",voucherArgs(1)));
    }
    @Test public void routerMismatchAndRejectedPersistenceBlockMutation() throws Exception {
        Memory m=profile();Router r=new Router();LocalAgent a=new LocalAgent(m,r,noBrain);String id=a.execute("prepare_vouchers",voucherArgs(2)).getJSONObject("plan").getString("id");
        a.state.getJSONArray("plans").getJSONObject(0).put("router_id","router-B");assertThrows(IOException.class,()->a.apply(id));assertEquals(0,r.mutations);
        a.state.getJSONArray("plans").getJSONObject(0).put("router_id","router-A");m.fail=true;assertThrows(IOException.class,()->a.apply(id));assertEquals(0,r.mutations);
    }
    @Test public void disallowsScriptFieldsAndGenericShellCommands() throws Exception {
        Memory m=profile();Router r=new Router();LocalAgent a=new LocalAgent(m,r,noBrain);
        JSONObject args=new JSONObject().put("resource","hotspot_profiles").put("action","set").put("id","*1").put("values",new JSONObject().put("on-login","/system reboot"));
        assertThrows(IOException.class,()->a.execute("prepare_resource_change",args));assertThrows(IOException.class,()->a.execute("shell",new JSONObject()));assertEquals(0,r.mutations);
    }
    @Test public void genericProfileUpdateChecksForStaleDataAndVerifiesResult() throws Exception {
        Memory m=profile();Router r=new Router();LocalAgent a=new LocalAgent(m,r,noBrain);
        JSONObject args=new JSONObject().put("resource","hotspot_profiles").put("action","set").put("id","*1").put("values",new JSONObject().put("rate-limit","2M/5M"));
        String id=a.execute("prepare_resource_change",args).getJSONObject("plan").getString("id");assertEquals(0,r.mutations);assertEquals("verified",a.apply(id).getString("status"));assertEquals("2M/5M",r.table.get("/ip/hotspot/user/profile").getJSONObject(0).getString("rate-limit"));
    }
    @Test public void permissionInspectionNeverEquatesSuccessfulLoginWithWriteAccess() throws Exception {
        Router r=new Router();JSONObject result=HostFunctions.access(r,config("Test"));assertEquals("unknown",result.getString("write"));assertEquals(0,r.mutations);
        r.table.put("/user",new JSONArray().put(new JSONObject().put("name","test-user").put("group","reader")));r.table.put("/user/group",new JSONArray().put(new JSONObject().put("name","reader").put("policy","read,api")));
        assertEquals("denied_by_policy",HostFunctions.access(r,config("Test")).getString("write"));
    }
}
