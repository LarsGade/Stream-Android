package dk.stream.android;

import android.content.Context;
import android.net.Uri;
import android.util.AtomicFile;
import androidx.media3.common.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.json.*;

/** Audio-only catalog for car controllers. User TV sources and credentials are never exposed. */
@androidx.media3.common.util.UnstableApi
public final class CarRadioCatalog {
 public static final String ROOT="radio-root",FAVORITES="radio-favorites",DANISH="radio-danish",PREFIX="radio:";
 private final Context context;
 private final LibraryPersistence store;
 private final RadioBrowser browser;
 private final Map<String,RadioStation> known=new ConcurrentHashMap<>();
 public CarRadioCatalog(Context context) {this(context,new LibraryStore(context),new RadioBrowser());}
 CarRadioCatalog(Context context,LibraryPersistence store,RadioBrowser browser) {this.context=context.getApplicationContext();this.store=store;this.browser=browser;}
 static synchronized void writeCache(Context context,List<RadioStation> stations) {
  AtomicFile file=new AtomicFile(new File(context.getCacheDir(),"car_radio.json"));FileOutputStream out=null;
  try {JSONArray data=new JSONArray();for(RadioStation station:stations) data.put(station.toJson());out=file.startWrite();out.write(data.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));file.finishWrite(out);} catch(Exception e) {if(out!=null) file.failWrite(out);}
 }
 private List<RadioStation> danish() throws Exception {
  File path=new File(context.getCacheDir(),"car_radio.json");List<RadioStation> cached=new ArrayList<>();
  try(InputStream in=new AtomicFile(path).openRead();ByteArrayOutputStream bytes=new ByteArrayOutputStream()) {
   byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1) bytes.write(buffer,0,n);
   JSONArray values=new JSONArray(bytes.toString("UTF-8"));for(int i=0;i<values.length();i++) cached.add(RadioStation.fromJson(values.getJSONObject(i)));
  } catch(Exception ignored) { }
  if(!cached.isEmpty() && System.currentTimeMillis()-path.lastModified()<30*60*1000) return remember(cached);
  try {List<RadioStation> fresh=browser.loadDanish();writeCache(context,fresh);return remember(fresh);} catch(Exception e) {if(!cached.isEmpty()) return remember(cached);throw e;}
 }
 private List<RadioStation> remember(List<RadioStation> stations) {for(RadioStation station:stations) known.put(PREFIX+station.id,station);return stations;}
 public List<RadioStation> favorites() throws Exception {return remember(new ArrayList<>(store.load().radioFavorites));}
 public RadioStation known(String id) {return known.get(id);}
 public RadioStation resolve(String id) throws Exception {
  if(!id.startsWith(PREFIX)) throw new IllegalArgumentException("Unknown radio id");
  for(RadioStation station:favorites()) if((PREFIX+station.id).equals(id)) return station;
  RadioStation cached=known.get(id);if(cached!=null) return cached;
  for(RadioStation station:danish()) if((PREFIX+station.id).equals(id)) return station;
  throw new IllegalArgumentException("Station unavailable");
 }
 public List<MediaItem> children(String parent,int page,int pageSize) throws Exception {
  List<MediaItem> items=new ArrayList<>();
  if(ROOT.equals(parent)) {items.add(folder(FAVORITES,context.getString(R.string.favorites)));items.add(folder(DANISH,context.getString(R.string.car_danish_radio)));}
  else if(FAVORITES.equals(parent)||DANISH.equals(parent)) for(RadioStation station:FAVORITES.equals(parent)?favorites():danish()) items.add(item(station,false));
  else throw new IllegalArgumentException("Unknown radio folder");
  if(page<0||pageSize<1) throw new IllegalArgumentException("Invalid page");long start=(long)page*pageSize;
  if(start>=items.size()) return Collections.emptyList();return new ArrayList<>(items.subList((int)start,(int)Math.min(items.size(),start+pageSize)));
 }
 public List<MediaItem> search(String query) throws Exception {
  String key=LocalChannelLogos.key(query.trim());if(key.isEmpty()) return Collections.emptyList();
  LinkedHashMap<String,RadioStation> matches=new LinkedHashMap<>();
  for(RadioStation station:favorites()) if(LocalChannelLogos.key(station.displayName()).contains(key)) matches.put(station.id,station);
  try {for(RadioStation station:danish()) if(!matches.containsKey(station.id) && LocalChannelLogos.key(station.displayName()).contains(key)) matches.put(station.id,station);} catch(Exception e) {if(matches.isEmpty()) throw e;}
  List<MediaItem> result=new ArrayList<>();for(RadioStation station:matches.values()) result.add(item(station,false));return result;
 }
 public static MediaItem folder(String id,String title) {return new MediaItem.Builder().setMediaId(id).setMediaMetadata(new MediaMetadata.Builder().setTitle(title).setIsBrowsable(true).setIsPlayable(false).build()).build();}
 public MediaItem item(RadioStation station,boolean playable) {
  Uri artwork=PlaybackArtwork.bundled(context,station.displayName())?CarArtworkProvider.uri(context,station.displayName()):Uri.parse(station.logo.isEmpty()?CarArtworkProvider.uri(context,"Stream").toString():station.logo);
  MediaMetadata.Builder metadata=new MediaMetadata.Builder().setTitle(station.displayName()).setArtist("Stream").setIsBrowsable(false).setIsPlayable(true).setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION).setArtworkUri(artwork);
  MediaItem.Builder item=new MediaItem.Builder().setMediaId(PREFIX+station.id);
  if(playable) {item.setUri(Playlist.httpUrl(station.url).toString()).setMimeType(station.hls?MimeTypes.APPLICATION_M3U8:null);metadata.setArtworkData(PlaybackArtwork.local(context,station.displayName()),MediaMetadata.PICTURE_TYPE_FRONT_COVER);}
  return item.setMediaMetadata(metadata.build()).build();
 }
}
