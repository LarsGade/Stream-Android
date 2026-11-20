package dk.stream.android;
import android.content.*;
import android.os.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

@androidx.media3.common.util.UnstableApi
final class MediaDownloads {
 private static final ExecutorService worker=Executors.newSingleThreadExecutor();
 static boolean eligible(FavoriteLists.Item item){return item!=null&&item.radio==null&&(item.file!=null||item.stream!=null&&item.stream.channel.kind!=Playlist.Kind.TV);}
 static void add(PopupMenu menu,Context context,FavoriteLists.Item item){if(eligible(item))menu.getMenu().add("Download").setOnMenuItemClickListener(choice->{start(context,Collections.singletonList(item),item.name());return true;});}
 static void addGroup(PopupMenu menu,Context context,List<Library.Entry> entries,String name){List<FavoriteLists.Item> items=new ArrayList<>();for(Library.Entry entry:entries){FavoriteLists.Item item=FavoriteLists.of(entry);if(eligible(item))items.add(item);}if(!items.isEmpty())menu.getMenu().add("Download").setOnMenuItemClickListener(choice->{start(context,items,name);return true;});}
 static void show(android.view.View anchor,Context context,FavoriteLists.Item item){if(!eligible(item))return;PopupMenu menu=new StablePopupMenu(context,anchor);add(menu,context,item);menu.show();}
 static FavoriteLists.Item current(Context context){if(CastPlayback.hasMedia(context))return CastPlayback.downloadItem(context);LocalMedia file=LocalAudioService.current();if(file==null)file=RadioPlaybackService.selectedFile();return file==null?null:FavoriteLists.of(file);}
 private static void start(Context context,List<FavoriteLists.Item> items,String name){Context app=context.getApplicationContext();worker.execute(()->{try{JSONArray targets=new JSONArray();for(FavoriteLists.Item item:items){if(!eligible(item))continue;JSONObject target=new JSONObject();if(item.file!=null)target.put("file",item.file.json());else{Playlist.Channel channel=item.stream.channel;target.put("source",item.stream.sourceId).put("name",channel.name).put("url",channel.url).put("kind",channel.kind.name()).put("series",channel.seriesId==null?JSONObject.NULL:channel.seriesId).put("group",channel.group);}targets.put(target);}String request=save(app,new JSONObject().put("name",name).put("items",targets));new Handler(Looper.getMainLooper()).post(()->{if(context instanceof android.app.Activity&&(((android.app.Activity)context).isFinishing()||((android.app.Activity)context).isDestroyed())){try{file(app,request).delete();}catch(Exception ignored){}return;}try{context.startActivity(new Intent(context,MediaDownloadActivity.class).putExtra("request",request));}catch(RuntimeException failure){Toast.makeText(app,"Download kunne ikke startes.",Toast.LENGTH_LONG).show();}});}catch(Exception failure){new Handler(Looper.getMainLooper()).post(()->Toast.makeText(app,"Download kunne ikke klargøres.",Toast.LENGTH_LONG).show());}});}
 static File file(Context context,String name)throws IOException{if(name==null||!name.matches("[0-9a-f-]{36}\\.json"))throw new IOException("Invalid download plan");File directory=new File(context.getCacheDir(),"download-plans");if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot create download plan");return new File(directory,name);}
 static String save(Context context,JSONObject data)throws Exception{String name=UUID.randomUUID()+".json";try(OutputStream output=new FileOutputStream(file(context,name))){output.write(data.toString().getBytes(StandardCharsets.UTF_8));}return name;}
 static JSONObject read(Context context,String name)throws Exception{try(InputStream input=new FileInputStream(file(context,name));ByteArrayOutputStream output=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))!=-1){if(output.size()+n>32*1024*1024)throw new IOException("Download plan too large");output.write(buffer,0,n);}return new JSONObject(output.toString("UTF-8"));}}
 private MediaDownloads(){}
}
