package dk.stream.android;

import android.content.Context;
import android.net.Uri;
import android.util.AtomicFile;
import androidx.media3.common.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.json.*;

/** Audio documents and folders, shared by the car browser and artwork provider. */
@androidx.media3.common.util.UnstableApi
final class CarAudioCatalog {
 static final String PODCAST="car-podcast",MUSIC="car-music",FILE="car-audio:",FOLDER="car-folder:";
 private static final Map<String,LocalMedia> known=new ConcurrentHashMap<>();private static boolean loaded;private static final Map<String,List<LocalMedia>> queues=new ConcurrentHashMap<>();
 private final Context context;private final LibraryPersistence store;
 CarAudioCatalog(Context context,LibraryPersistence store){this.context=context.getApplicationContext();this.store=store;}
 static String id(LocalMedia file){return (file.folder?FOLDER:FILE)+UUID.nameUUIDFromBytes(file.uri.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 static LocalMedia child(LocalMedia folder,LocalMedia file){return new LocalMedia(file.uri,folder.name+" / "+file.name,file.mime,file.folder);}
 static boolean hasFavorites(Library library,String list){for(FavoriteLists.Item item:FavoriteLists.items(library,list))if(item.radio!=null||item.file!=null&&accepts(item.file))return true;return false;}
 static boolean accepts(LocalMedia file){return file.folder||file.audio();}
 private static synchronized void load(Context context){if(loaded)return;try(InputStream input=new AtomicFile(new File(context.getFilesDir(),"car_audio_index.json")).openRead();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1){if(bytes.size()+count>32*1024*1024)throw new IOException();bytes.write(buffer,0,count);}JSONArray array=new JSONArray(bytes.toString("UTF-8"));for(int i=0;i<array.length();i++){LocalMedia file=LocalMedia.from(array.getJSONObject(i));if(accepts(file))known.put(id(file),file);}}catch(Exception ignored){}loaded=true;}
 private static synchronized void save(Context context){AtomicFile index=new AtomicFile(new File(context.getFilesDir(),"car_audio_index.json"));FileOutputStream output=null;try{JSONArray array=new JSONArray();for(LocalMedia file:known.values())array.put(file.json());byte[] bytes=array.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);if(bytes.length>32*1024*1024)return;output=index.startWrite();output.write(bytes);index.finishWrite(output);}catch(Exception failure){if(output!=null)index.failWrite(output);}}
 static LocalMedia resolve(Context context,String id){load(context);LocalMedia file=known.get(id);if(file==null)throw new IllegalArgumentException("Lydfilen findes ikke længere. Åbn dens mappe igen.");return file;}
 List<MediaItem> children(String parent)throws Exception{load(context);Library library=store.load();List<LocalMedia> files=new ArrayList<>();
  if(PODCAST.equals(parent)||MUSIC.equals(parent)||AndroidAutoPages.FAVORITES.equals(parent)){for(FavoriteLists.Item item:FavoriteLists.items(library,AndroidAutoPages.FAVORITES.equals(parent)?"Radio":MUSIC.equals(parent)?"Musik":"Podcast"))if(item.file!=null&&accepts(item.file))files.add(item.file);}
  else{LocalMedia folder=resolve(context,parent);if(!folder.folder)throw new IllegalArgumentException("Not a folder");if(PcloudDocument.is(folder.uri)){if(library.pcloud==null)throw new IOException("Log på Pcloud igen.");WebDavClient cloud=new WebDavClient(library.pcloud);try{for(WebDavClient.Entry entry:cloud.list(PcloudDocument.url(library.pcloud,folder.uri)))if(accepts(entry.media(library.pcloud)))files.add(child(folder,entry.media(library.pcloud)));}finally{cloud.cancel();}}else for(LocalMedia file:LocalMedia.children(context,folder))if(accepts(file))files.add(child(folder,file));Collections.sort(files,NaturalNameOrder.MEDIA);}
  List<LocalMedia> queue=new ArrayList<>();for(LocalMedia file:files)if(!file.folder)queue.add(file);for(LocalMedia file:queue)queues.put(id(file),queue);
  List<MediaItem> result=new ArrayList<>();for(LocalMedia file:files){known.put(id(file),file);result.add(item(context,file,false));}save(context);
  if(PODCAST.equals(parent)){for(FavoriteLists.Item favorite:FavoriteLists.items(library,"Musik"))if(favorite.file!=null&&accepts(favorite.file)){result.add(CarRadioCatalog.folder(MUSIC,"Musik"));break;}}
  return result;
 }
 List<MediaItem> search(String query)throws Exception{load(context);Library library=store.load();for(String list:new String[]{"Radio","Podcast","Musik"})for(FavoriteLists.Item favorite:FavoriteLists.items(library,list))if(favorite.file!=null&&accepts(favorite.file))known.put(id(favorite.file),favorite.file);String key=LocalChannelLogos.key(query.trim());List<LocalMedia> matches=new ArrayList<>();for(LocalMedia file:known.values())if(!file.folder&&LocalChannelLogos.key(file.displayName()).contains(key))matches.add(file);Collections.sort(matches,NaturalNameOrder.MEDIA);List<MediaItem> result=new ArrayList<>();for(LocalMedia file:matches)result.add(item(context,file,false));save(context);return result;}
 List<MediaItem> queue(String id){List<MediaItem> result=new ArrayList<>();List<LocalMedia> files=queues.get(id);if(files==null)files=Collections.singletonList(resolve(context,id));for(LocalMedia file:files)result.add(item(context,file,true));return result;}
 static MediaItem item(Context context,LocalMedia file,boolean playable){if(!accepts(file)||playable&&file.folder)throw new IllegalArgumentException("Not an audio file");android.os.Bundle extras=new android.os.Bundle();try{extras.putString("car_file",file.json().toString());}catch(JSONException impossible){throw new IllegalArgumentException(impossible);}extras.putBoolean("history_live",false);
  MediaMetadata.Builder metadata=PlaybackText.metadata(playable?file.displayName():file.displayFileName()).setIsBrowsable(file.folder).setIsPlayable(!file.folder).setMediaType(file.folder?MediaMetadata.MEDIA_TYPE_FOLDER_MIXED:MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE).setArtworkUri(CarArtworkProvider.audio(id(file))).setExtras(extras);
  MediaItem.Builder result=new MediaItem.Builder().setMediaId(id(file)).setMediaMetadata(metadata.build());if(playable)result.setUri(file.uri).setMimeType(CastPlayback.mime(file));return result.build();
 }
 static LocalMedia file(MediaItem item){try{return item!=null&&item.mediaMetadata.extras!=null&&item.mediaMetadata.extras.containsKey("car_file")?LocalMedia.from(new JSONObject(item.mediaMetadata.extras.getString("car_file"))):null;}catch(Exception error){return null;}}
}
