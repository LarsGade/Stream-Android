package dk.stream.android;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import org.json.*;

/** Portable app settings, including credentials; Android document grants stay on the device. */
public final class SettingsBackup {
 public static final int MAX_BYTES=16*1024*1024;
 private static final String FORMAT="dk.stream.android.settings";
 public static final class Snapshot {
  public final Library library;final Map<String,Map<String,Object>> preferences;
  Snapshot(Library library,Map<String,Map<String,Object>> preferences){this.library=library;this.preferences=preferences;}
 }
 public static byte[] encode(Library library)throws Exception{return encode(library,BackupPreferences.empty());}
 static byte[] encode(Library library,Map<String,Map<String,Object>> preferences)throws Exception{
  JSONObject root=new JSONObject().put("format",FORMAT).put("backupVersion",2).put("settings",new JSONObject(library.toJson())).put("preferences",BackupPreferences.encode(preferences));
  byte[] bytes=(root.toString(2)+"\n").getBytes(StandardCharsets.UTF_8);if(bytes.length>MAX_BYTES)throw new IOException("Settings too large");return bytes;
 }
 public static Library decode(InputStream input)throws Exception{return decodeSnapshot(input).library;}
 static Snapshot decodeSnapshot(InputStream input) throws Exception {
  if(input==null)throw new IOException("File unavailable");ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] chunk=new byte[8192];int count;
  while((count=input.read(chunk))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();if(count>MAX_BYTES-bytes.size())throw new IOException("Settings too large");bytes.write(chunk,0,count);}
  String json=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes.toByteArray())).toString();if(json.startsWith("\ufeff"))json=json.substring(1);
  JSONTokener tokens=new JSONTokener(json);Object value=tokens.nextValue();if(!(value instanceof JSONObject)||tokens.nextClean()!=0)throw new JSONException("Invalid settings JSON");JSONObject root=(JSONObject)value;
  Map<String,Map<String,Object>> preferences=BackupPreferences.empty();
  if(root.has("format")){int version=root.getInt("backupVersion");if(!FORMAT.equals(root.getString("format"))||(version!=1&&version!=2))throw new JSONException("Unsupported backup format");if(version==2)preferences=BackupPreferences.decode(root.getJSONObject("preferences"));root=root.getJSONObject("settings");}
  if(root.getInt("version")!=1)throw new JSONException("Unsupported settings version");
  if(root.has("pcloud")&&!root.isNull("pcloud")&&!(root.get("pcloud") instanceof JSONObject))throw new JSONException("Invalid account");
  for(String name:new String[]{"guides","localFiles","localFavorites","imageFavorites","radioFavorites","favoriteOrder","favoriteHomeOrder"})if(root.has(name)&&!(root.get(name) instanceof JSONArray))throw new JSONException("Invalid settings list");
  if(root.has("favoriteAssignments")&&!(root.get("favoriteAssignments") instanceof JSONObject))throw new JSONException("Invalid favorite lists");
  if(root.has("tvFavoriteLists")&&!(root.get("tvFavoriteLists") instanceof JSONObject))throw new JSONException("Invalid TV favorite lists");
  if(root.has("favoriteKinds")&&!(root.get("favoriteKinds") instanceof JSONObject))throw new JSONException("Invalid streaming favorites");
  JSONArray sources=root.getJSONArray("sources"),favorites=root.getJSONArray("favorites");Set<String> ids=new HashSet<>();
  for(int i=0;i<sources.length();i++){String id=sources.getJSONObject(i).getString("id");if(id.isEmpty()||!ids.add(id))throw new JSONException("Duplicate source");}
  for(int i=0;i<favorites.length();i++)if(!ids.contains(favorites.getJSONObject(i).getString("sourceId")))throw new JSONException("Favorite source missing");
  Library library=Library.fromJson(root.toString());for(Library.Source source:library.sources)Playlist.httpUrl(source.server);return new Snapshot(library,preferences);
 }
 private SettingsBackup(){}
}
