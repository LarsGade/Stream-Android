package dk.stream.android;
import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import org.json.*;

/** Bundled channel artwork wins over provider artwork when the channel name matches. */
public final class LocalChannelLogos {
 private final Map<String,String> files;
 private LocalChannelLogos(Map<String,String> files) {this.files=files;}
 public static String key(String name) {return Normalizer.normalize(name,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replace('’','\'').replaceAll("\\s+","");}
 public static LocalChannelLogos load(Context context) {
  try(InputStream input=context.getAssets().open("channel_logos_index.json");ByteArrayOutputStream bytes=new ByteArrayOutputStream()) {
   byte[] buffer=new byte[4096];int count;while((count=input.read(buffer))!=-1) bytes.write(buffer,0,count);
   JSONObject index=new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));Map<String,String> files=new HashMap<>();
   Iterator<String> keys=index.keys();while(keys.hasNext()) {String key=keys.next();files.put(key,index.getString(key));}
   return new LocalChannelLogos(files);
  } catch(IOException|JSONException e) {return new LocalChannelLogos(Collections.emptyMap());}
 }
 public String find(String channelName) {String file=files.get(key(channelName));return file==null?null:"file:///android_asset/channel_logos/"+file;}
 public int size() {return files.size();}
}
