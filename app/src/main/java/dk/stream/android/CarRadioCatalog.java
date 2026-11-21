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
 public static final String ROOT="radio-root",FAVORITES="radio-favorites",DANISH="radio-danish",PREFIX="radio:",ENGLAND="radio-england",USA="radio-usa";
 private final Context context;private final CarAudioCatalog audio;
 private final LibraryPersistence store;
 private final RadioBrowser browser;
 private final Map<String,RadioStation> known=new ConcurrentHashMap<>();
 public CarRadioCatalog(Context context) {this(context,new LibraryStore(context),new RadioBrowser());}
 CarRadioCatalog(Context context,LibraryPersistence store,RadioBrowser browser) {this.context=context.getApplicationContext();this.store=store;this.browser=browser;this.audio=new CarAudioCatalog(context,store);}
 static synchronized void writeCache(Context context,List<RadioStation> stations) {
  AtomicFile file=new AtomicFile(new File(context.getFilesDir(),"car_radio.json"));FileOutputStream out=null;
  try {JSONArray data=new JSONArray();for(RadioStation station:stations) data.put(station.toJson());out=file.startWrite();out.write(data.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));file.finishWrite(out);} catch(Exception e) {if(out!=null) file.failWrite(out);}
 }
 static synchronized List<RadioStation> readCache(Context context){
  File path=new File(context.getFilesDir(),"car_radio.json");if(!path.exists())path=new File(context.getCacheDir(),"car_radio.json");List<RadioStation> cached=new ArrayList<>();
  try(InputStream in=new AtomicFile(path).openRead();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);JSONArray values=new JSONArray(bytes.toString("UTF-8"));for(int i=0;i<values.length();i++)cached.add(RadioStation.fromJson(values.getJSONObject(i)));}catch(Exception ignored){}
  return cached;
 }
 private List<RadioStation> danish() throws Exception {return remember(readCache(context));}
 private List<RadioStation> remember(List<RadioStation> stations) {for(RadioStation station:stations) known.put(PREFIX+station.id,station);return stations;}
 Context context(){return context;}
 MediaItem resumeRadio(RadioStation station){remember(Collections.singletonList(station));return item(station,true);}
 List<MediaItem> audioQueue(String id){return audio.queue(id);}
 public List<RadioStation> favorites() throws Exception {List<RadioStation> radios=new ArrayList<>();for(FavoriteLists.Item item:FavoriteLists.items(store.load(),"Radio"))if(item.radio!=null)radios.add(item.radio);return remember(radios);}
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
  if(AndroidAutoPages.START.equals(parent)){Library library=store.load();String chosen=AndroidAutoPages.view(library.androidAutoStartPage,library.androidAutoFavorites);items=AndroidAutoPages.startItems(chosen,children(chosen,0,Integer.MAX_VALUE),folder(ROOT,"Alle sider"));}
  else if(ROOT.equals(parent)){Library settings=store.load();for(String id:AndroidAutoPages.IDS)if(!ROOT.equals(id))items.add(folder(AndroidAutoPages.view(id,settings.androidAutoFavorites),AndroidAutoPages.label(id)));}
  else if(AndroidAutoPages.PODCAST.equals(parent)||AndroidAutoPages.MUSIC.equals(parent)||AndroidAutoPages.LOCAL.equals(parent)||AndroidAutoPages.CLOUD.equals(parent)||parent.endsWith("-all")||parent.startsWith(CarAudioCatalog.FOLDER))items.addAll(audio.children(parent));
  else if(AndroidAutoPages.COUNTRIES.equals(parent)){items.add(folder(DANISH,"Danmark"));items.add(folder(ENGLAND,"England"));items.add(folder(USA,"USA"));}
  else if(FAVORITES.equals(parent)||DANISH.equals(parent)||ENGLAND.equals(parent)||USA.equals(parent)) for(RadioStation station:FAVORITES.equals(parent)?favorites():danish()) {String country=ENGLAND.equals(parent)?"GB":USA.equals(parent)?"US":"DK";if(FAVORITES.equals(parent)||country.equals(station.country))items.add(item(station,false));}
  else throw new IllegalArgumentException("Unknown radio folder");
  if(FAVORITES.equals(parent)){items.addAll(audio.children(parent));items.add(folder(AndroidAutoPages.COUNTRIES,"Alle radiostationer"));}
  if(page<0||pageSize<1) throw new IllegalArgumentException("Invalid page");long start=(long)page*pageSize;
  if(start>=items.size()) return Collections.emptyList();return new ArrayList<>(items.subList((int)start,(int)Math.min(items.size(),start+pageSize)));
 }
 public List<MediaItem> search(String query) throws Exception {
  String key=LocalChannelLogos.key(query.trim());if(key.isEmpty()) return Collections.emptyList();
  Library settings=store.load();if(AndroidAutoPages.MUSIC.equals(settings.androidAutoStartPage)&&!settings.androidAutoFavorites){OpenSpotSaavnApi.Page page=new OpenSpotSaavnApi(settings.openSpotApiUrl,settings.openSpotProxyUrl).searchMusic(query,0);SaavnMusic.remember(context,page.songs);List<MediaItem> songs=new ArrayList<>();for(SaavnSong song:page.songs){LocalMedia file=SaavnMusic.media(song);CarAudioCatalog.register(context,file);songs.add(CarAudioCatalog.item(context,file,false));}return songs;}
  LinkedHashMap<String,RadioStation> matches=new LinkedHashMap<>();
  for(RadioStation station:favorites()) if(LocalChannelLogos.key(station.displayName()).contains(key)) matches.put(station.id,station);
  try {for(RadioStation station:danish()) if(!matches.containsKey(station.id) && LocalChannelLogos.key(station.displayName()).contains(key)) matches.put(station.id,station);} catch(Exception e) {if(matches.isEmpty()) throw e;}
  List<MediaItem> result=new ArrayList<>();for(RadioStation station:matches.values()) result.add(item(station,false));result.addAll(audio.search(query));return result;
 }
 public static MediaItem folder(String id,String title) {return new MediaItem.Builder().setMediaId(id).setMediaMetadata(PlaybackText.metadata(UiText.text(title)).setArtworkUri(CarArtworkProvider.page(id)).setIsBrowsable(true).setIsPlayable(false).build()).build();}
 static android.os.Bundle liveExtras(){android.os.Bundle extras=new android.os.Bundle();extras.putBoolean("history_live",true);return extras;}
 static byte[] artworkData(Context context,RadioStation station){String name=PlaybackArtwork.logoName(context,station);return PlaybackArtwork.bundled(context,name)?PlaybackArtwork.local(context,name):CarPageArtwork.png("radio-placeholder");}
 public MediaItem item(RadioStation station,boolean playable) {
  Uri artwork=CarArtworkProvider.station(context,station.id);
  MediaMetadata.Builder metadata=PlaybackText.metadata(station.displayName()).setExtras(liveExtras()).setDisplayTitle(station.displayName()).setIsBrowsable(false).setIsPlayable(true).setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION).setArtworkUri(artwork);
  MediaItem.Builder item=new MediaItem.Builder().setMediaId(PREFIX+station.id);
  if(playable) {item.setUri(Playlist.httpUrl(station.url).toString()).setMimeType(station.hls?MimeTypes.APPLICATION_M3U8:null);metadata.setArtworkData(artworkData(context,station),MediaMetadata.PICTURE_TYPE_FRONT_COVER);}
  return item.setMediaMetadata(metadata.build()).build();
 }
}
