package dk.stream.android;
import java.util.*;
import org.json.*;

/** All user-controlled preference stores outside Library, with lossless value types. */
final class BackupPreferences {
 static final String[] STORES={"playback_history","audio_output","radio_widgets"};
 static Map<String,Map<String,Object>> empty(){Map<String,Map<String,Object>> result=new LinkedHashMap<>();for(String store:STORES)result.put(store,new LinkedHashMap<>());return result;}
 static JSONObject encode(Map<String,Map<String,Object>> stores)throws Exception{
  JSONObject root=new JSONObject();for(String name:STORES){Map<String,Object> values=stores.get(name);if(values==null)throw new JSONException("Missing preference store");JSONObject entries=new JSONObject();for(Map.Entry<String,Object> entry:values.entrySet()){Object value=entry.getValue();String type;Object encoded=value;if(value instanceof String)type="string";else if(value instanceof Boolean)type="boolean";else if(value instanceof Integer)type="int";else if(value instanceof Long)type="long";else if(value instanceof Float){type="float";if(Float.isNaN((Float)value)||Float.isInfinite((Float)value))throw new JSONException("Invalid float");}else if(value instanceof Set){type="strings";TreeSet<String> sorted=new TreeSet<>();for(Object item:(Set<?>)value){if(!(item instanceof String))throw new JSONException("Invalid string set");sorted.add((String)item);}encoded=new JSONArray(sorted);}else throw new JSONException("Unsupported preference type");entries.put(entry.getKey(),new JSONObject().put("type",type).put("value",encoded));}root.put(name,entries);}decode(root);return root;
 }
 static Map<String,Map<String,Object>> decode(JSONObject root)throws Exception{
  Map<String,Map<String,Object>> result=empty();Iterator<String> stores=root.keys();while(stores.hasNext())if(!result.containsKey(stores.next()))throw new JSONException("Unsupported preference store");
  for(String name:STORES){JSONObject entries=root.getJSONObject(name);Iterator<String> keys=entries.keys();while(keys.hasNext()){String key=keys.next();JSONObject entry=entries.getJSONObject(key);String type=entry.getString("type");Object raw=entry.get("value"),value;
    switch(type){case "string":if(!(raw instanceof String))throw new JSONException("Invalid string");value=raw;break;case "boolean":if(!(raw instanceof Boolean))throw new JSONException("Invalid boolean");value=raw;break;case "int":case "long":if(!(raw instanceof Number))throw new JSONException("Invalid integer");long number;try{number=Long.parseLong(raw.toString());}catch(NumberFormatException bad){throw new JSONException("Invalid integer");}if(type.equals("int")){if(number<Integer.MIN_VALUE||number>Integer.MAX_VALUE)throw new JSONException("Integer overflow");value=(int)number;}else value=number;break;case "float":if(!(raw instanceof Number))throw new JSONException("Invalid float");float f=((Number)raw).floatValue();if(Float.isInfinite(f)||Float.isNaN(f))throw new JSONException("Invalid float");value=f;break;case "strings":if(!(raw instanceof JSONArray))throw new JSONException("Invalid string set");Set<String> set=new LinkedHashSet<>();for(int i=0;i<((JSONArray)raw).length();i++){Object item=((JSONArray)raw).get(i);if(!(item instanceof String))throw new JSONException("Invalid string set");set.add((String)item);}value=set;break;default:throw new JSONException("Unsupported preference type");}
    validate(name,key,value);result.get(name).put(key,value);
  }}return result;
 }
 private static void validate(String store,String key,Object value)throws Exception{
  if(store.equals("playback_history")){if(!key.matches("[0-9a-f]{64}")||!(value instanceof String)||!((String)value).matches("[0-9]+:[0-9]+:(true|false)"))throw new JSONException("Invalid playback history");String[] fields=((String)value).split(":");long position=Long.parseLong(fields[0]),duration=Long.parseLong(fields[1]);if(duration<=0||position<0||position>duration)throw new JSONException("Invalid playback position");}
  else if(store.equals("radio_widgets")&&(!key.matches("station_[0-9]+")||!(value instanceof String)))throw new JSONException("Invalid widget binding");
  else if(store.equals("audio_output")){if((key.equals("device")||key.equals("type"))&&!(value instanceof Integer))throw new JSONException("Invalid audio device");if(key.equals("name")&&!(value instanceof String))throw new JSONException("Invalid device name");}
 }
 private BackupPreferences(){}
}
