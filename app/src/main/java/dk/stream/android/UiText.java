package dk.stream.android;
import android.content.Context;
import java.io.*;
import java.util.*;
import org.json.*;
/** Presentation translations leave stored page IDs, filenames and user content unchanged. */
final class UiText {
 private static final Map<String,String> translations=new LinkedHashMap<>();private static volatile boolean english;
 static void init(Context context,boolean enabled){english=enabled;if(!translations.isEmpty())return;try(InputStream input=context.getAssets().open("ui_en.json");ByteArrayOutputStream bytes=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1)bytes.write(buffer,0,count);JSONObject json=new JSONObject(bytes.toString("UTF-8"));Iterator<String> keys=json.keys();while(keys.hasNext()){String key=keys.next();translations.put(key,json.getString(key));}}catch(IOException|JSONException failure){throw new IllegalStateException("Cannot load UI translations",failure);}}
 static String text(String value){return english?translate(value,translations):value;}
 static CharSequence text(CharSequence value){return value instanceof String?text(value.toString()):value;}
 static String translate(String value,Map<String,String> dictionary){if(value==null||value.isEmpty())return value;String exact=dictionary.get(value);if(exact!=null)return exact;List<String> fragments=new ArrayList<>();for(String key:dictionary.keySet())if((!key.trim().equals(key)||key.startsWith(".")||key.startsWith("?"))&&!key.trim().isEmpty())fragments.add(key);Collections.sort(fragments,(a,b)->Integer.compare(b.length(),a.length()));for(String key:fragments)if(value.startsWith(key)){String tail=value.substring(key.length());for(String suffix:fragments)if((suffix.startsWith(".")||suffix.startsWith("?"))&&tail.endsWith(suffix)){tail=tail.substring(0,tail.length()-suffix.length())+dictionary.get(suffix);break;}return dictionary.get(key)+tail;}for(String key:fragments){if(value.endsWith(key))return value.substring(0,value.length()-key.length())+dictionary.get(key);if(Character.isDigit(value.charAt(0))&&key.startsWith(" ")){int at=value.indexOf(key);if(at>0&&value.substring(0,at).matches("[0-9]+"))return value.substring(0,at)+dictionary.get(key)+value.substring(at+key.length());}}return value;}


 static String[] labels(String[] labels){String[] copy=labels.clone();for(int i=0;i<copy.length;i++)copy[i]=text(copy[i]);return copy;}
 static CharSequence[] labels(CharSequence[] labels){CharSequence[] copy=labels.clone();for(int i=0;i<copy.length;i++)copy[i]=text(copy[i]);return copy;}
 static android.widget.Toast toast(Context context,CharSequence value,int duration){return android.widget.Toast.makeText(context,text(value),duration);}
 static android.widget.Toast toast(Context context,int resource,int duration){return android.widget.Toast.makeText(context,resource,duration);}
 private UiText(){}
}
