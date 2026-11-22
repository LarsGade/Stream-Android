package dk.stream.android;
import android.content.*;
import java.io.IOException;
import java.util.*;

/** Central registry for portable settings outside the encrypted Library. */
final class AppSettingsState {
 static Map<String,Map<String,Object>> capture(Context context){Map<String,Map<String,Object>> result=BackupPreferences.empty();for(String name:BackupPreferences.STORES){Map<String,Object> snapshot=result.get(name);for(Map.Entry<String,?> entry:context.getSharedPreferences(name,0).getAll().entrySet()){Object value=entry.getValue();if(value instanceof Set)value=new LinkedHashSet<>((Set<?>)value);snapshot.put(entry.getKey(),value);}}
  android.media.AudioManager audio=(android.media.AudioManager)context.getSystemService(Context.AUDIO_SERVICE);Map<String,Object> output=result.get("audio_output");Object id=output.get("device");if(id instanceof Integer&&!output.containsKey("name"))for(android.media.AudioDeviceInfo device:audio.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS))if(device.getId()==(Integer)id)output.put("name",device.getProductName().toString());return result;
 }
 static void restore(Context context,Map<String,Map<String,Object>> state)throws IOException{for(String name:BackupPreferences.STORES){SharedPreferences.Editor editor=context.getSharedPreferences(name,0).edit().clear();for(Map.Entry<String,Object> entry:state.get(name).entrySet()){String key=entry.getKey();Object value=entry.getValue();if(value instanceof String)editor.putString(key,(String)value);else if(value instanceof Boolean)editor.putBoolean(key,(Boolean)value);else if(value instanceof Integer)editor.putInt(key,(Integer)value);else if(value instanceof Long)editor.putLong(key,(Long)value);else if(value instanceof Float)editor.putFloat(key,(Float)value);else if(value instanceof Set){Set<String> strings=new LinkedHashSet<>();for(Object item:(Set<?>)value)strings.add((String)item);editor.putStringSet(key,strings);}else throw new IOException("Unsupported preference value");}if(!editor.commit())throw new IOException("Could not save settings");}}
 private AppSettingsState(){}
}
