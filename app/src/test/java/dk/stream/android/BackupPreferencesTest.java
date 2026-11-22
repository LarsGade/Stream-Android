package dk.stream.android;
import java.util.*;
import java.io.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class BackupPreferencesTest {
 private Map<String,Map<String,Object>> state(){Map<String,Map<String,Object>> state=BackupPreferences.empty();state.get("playback_history").put("a".repeat(64),"123:999:false");state.get("radio_widgets").put("station_42","radio-id");Map<String,Object> audio=state.get("audio_output");audio.put("device",42);audio.put("type",8);audio.put("name","Headphones");audio.put("long",Long.MAX_VALUE);audio.put("float",0.25f);audio.put("boolean",true);audio.put("strings",new HashSet<>(Arrays.asList("a","ø")));return state;}
 @Test public void completeSnapshotPreservesPreferencesAndTheirTypes()throws Exception{Library library=new Library();library.moveFavoriteHome("Musik",-1);Map<String,Map<String,Object>> state=state();SettingsBackup.Snapshot restored=SettingsBackup.decodeSnapshot(new ByteArrayInputStream(SettingsBackup.encode(library,state)));assertEquals(state,restored.preferences);assertEquals(library.toJson(),restored.library.toJson());assertTrue(restored.preferences.get("audio_output").get("device") instanceof Integer);assertTrue(restored.preferences.get("audio_output").get("long") instanceof Long);}
 @Test public void oldBackupsReplaceAuxiliaryStateWithDefaults()throws Exception{JSONObject root=new JSONObject().put("format","dk.stream.android.settings").put("backupVersion",1).put("settings",new JSONObject(new Library().toJson()));SettingsBackup.Snapshot restored=SettingsBackup.decodeSnapshot(new ByteArrayInputStream(root.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));assertEquals(BackupPreferences.empty(),restored.preferences);}
 @Test(expected=JSONException.class)public void missingPreferenceStoreIsRejected()throws Exception{JSONObject root=BackupPreferences.encode(state());root.remove("radio_widgets");BackupPreferences.decode(root);}
 @Test(expected=JSONException.class)public void corruptProgressIsRejected()throws Exception{Map<String,Map<String,Object>> state=state();state.get("playback_history").put("a".repeat(64),"1000:999:false");BackupPreferences.encode(state);}
 @Test(expected=JSONException.class)public void wrongDeviceTypeIsRejected()throws Exception{Map<String,Map<String,Object>> state=state();state.get("audio_output").put("device","42");BackupPreferences.encode(state);}
 @Test(expected=JSONException.class)public void unknownPreferenceStoreIsRejected()throws Exception{JSONObject root=BackupPreferences.encode(state());root.put("other",new JSONObject());BackupPreferences.decode(root);}
}
