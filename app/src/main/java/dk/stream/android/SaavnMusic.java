package dk.stream.android;
import android.content.Context;
import android.net.Uri;
import android.util.AtomicFile;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.json.*;
/** Stable song references survive expired stream URLs, favorites, exports and Android Auto. */
final class SaavnMusic {
 static final String AUTHORITY=BuildConfig.APPLICATION_ID+".music";
 private static final Map<String,SaavnSong> songs=new ConcurrentHashMap<>();private static final Map<String,Long> refreshed=new ConcurrentHashMap<>();private static boolean loaded;
 static int favoriteVersion;
 static boolean is(String uri){try{java.net.URI value=java.net.URI.create(uri);return "content".equals(value.getScheme())&&AUTHORITY.equals(value.getAuthority())&&value.getPath().startsWith("/song/");}catch(Exception invalid){return false;}}
 static String id(String uri){Uri value=Uri.parse(uri);String id=value.getLastPathSegment();if(!AUTHORITY.equals(value.getAuthority())||value.getPathSegments().size()!=2||id==null||!id.matches("[A-Za-z0-9_-]{1,128}"))throw new IllegalArgumentException("Ugyldig musikreference.");return id;}
 static LocalMedia media(SaavnSong song){String type=song.stream.toLowerCase(Locale.ROOT).contains(".mp3")?"audio/mpeg":"audio/mp4";return new LocalMedia(new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath("song").appendPath(song.id).build().toString(),song.title+(type.equals("audio/mpeg")?".mp3":".m4a"),type,false);}
 static Uri cover(String songUri){return new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath("cover").appendPath(id(songUri)).build();}
 static SaavnSong peek(String id){return songs.get(id);}
 static synchronized void load(Context context){if(loaded)return;try(InputStream input=new AtomicFile(new File(context.getFilesDir(),"saavn_songs.json")).openRead();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1){if(bytes.size()+count>8*1024*1024)throw new IOException();bytes.write(buffer,0,count);}JSONArray array=new JSONArray(bytes.toString("UTF-8"));for(int i=0;i<array.length();i++){try{SaavnSong song=SaavnSong.parse(array.getJSONObject(i));songs.put(song.id,song);}catch(JSONException ignored){}}}catch(Exception ignored){}loaded=true;}
 static synchronized void remember(Context context,List<SaavnSong> list){load(context);for(SaavnSong song:list){songs.put(song.id,song);refreshed.put(song.id,System.currentTimeMillis());}AtomicFile index=new AtomicFile(new File(context.getFilesDir(),"saavn_songs.json"));FileOutputStream output=null;try{JSONArray array=new JSONArray();for(SaavnSong song:songs.values())array.put(song.json());byte[] data=array.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);if(data.length>8*1024*1024)return;output=index.startWrite();output.write(data);index.finishWrite(output);}catch(Exception failure){if(output!=null)index.failWrite(output);}}
 static SaavnSong resolve(Context context,String id,boolean force)throws IOException{load(context);SaavnSong cached=songs.get(id);Long time=refreshed.get(id);if(!force&&cached!=null&&!cached.stream.isEmpty()&&time!=null&&System.currentTimeMillis()-time<10*60*1000)return cached;String address;try{address=new LibraryStore(context).load().saavnApiUrl;}catch(Exception invalid){throw new IOException("Musikopsætningen kunne ikke indlæses.",invalid);}try{SaavnSong song=new SaavnApi(address).song(id);if(song.stream.isEmpty())throw new IOException("Musikkilden har ingen afspilningsadresse til denne sang.");remember(context,Collections.singletonList(song));return song;}catch(IOException failure){if(!force&&cached!=null&&!cached.stream.isEmpty())return cached;throw failure;}}
 static byte[] artwork(Context context,LocalMedia file){try{SaavnSong song=resolve(context,id(file.uri),false);return PlaybackArtwork.remote(context,song.image);}catch(Exception unavailable){return null;}}
 private SaavnMusic(){}
}
