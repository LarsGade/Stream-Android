package dk.stream.android;

import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import java.io.*;
import java.util.*;

/** Android 9-compatible ACTION_VIEW handoff; no private Android Auto APIs or installer spoofing. */
public final class CarPlayerBridge {
 public static Intent viewIntent(CarVideo video,long position) {
  Uri uri=Uri.parse(video.uri);Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,video.mime).putExtra(Intent.EXTRA_TITLE,video.title).putExtra("title",video.title).putExtra("position",Math.max(0,position));
  if("content".equals(uri.getScheme()))intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).setClipData(ClipData.newRawUri(video.title,uri));return intent;
 }
 public static CarVideo exportFavorites(Context context,List<CarVideo> videos) throws IOException {
  if(videos.isEmpty())throw new IllegalArgumentException("No video favorites");
  File directory=new File(context.getCacheDir(),"car-playlists");if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Could not create playlist directory");
  File[] previous=directory.listFiles();if(previous!=null)for(File file:previous)if(file.isFile()&&file.getName().endsWith(".m3u")&&System.currentTimeMillis()-file.lastModified()>24*60*60*1000L)file.delete();
  File file=File.createTempFile("stream-",".m3u",directory);
  try(Writer writer=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),java.nio.charset.StandardCharsets.UTF_8))){CarVideo.writePlaylist(writer,videos);}catch(IOException|RuntimeException error){file.delete();throw error;}
  Uri uri=FileProvider.getUriForFile(context,context.getPackageName()+".carvideo",file);
  return new CarVideo("Stream – TV- og filmfavoritter",uri.toString(),"application/x-mpegurl");
 }
 public static void open(Activity activity,CarVideo video,long position,Runnable launched) {
  Intent intent=viewIntent(video,position);PackageManager manager=activity.getPackageManager();List<ResolveInfo> candidates=new ArrayList<>();Set<String> seen=new HashSet<>();
  Set<String> carPackages=new HashSet<>();
  Intent projection=new Intent(Intent.ACTION_MAIN).addCategory("com.google.android.gms.car.category.CATEGORY_PROJECTION");for(ResolveInfo service:manager.queryIntentServices(projection,0))if(service.serviceInfo!=null)carPackages.add(service.serviceInfo.packageName);
  Intent parked=new Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.CAR_LAUNCHER");for(ResolveInfo player:manager.queryIntentActivities(parked,0))if(player.activityInfo!=null)carPackages.add(player.activityInfo.packageName);
  for(ResolveInfo candidate:manager.queryIntentActivities(intent,PackageManager.MATCH_DEFAULT_ONLY))if(candidate.activityInfo!=null&&candidate.activityInfo.exported&&(candidate.activityInfo.packageName.startsWith("me.aap.fermata.auto")||carPackages.contains(candidate.activityInfo.packageName))&&!candidate.activityInfo.packageName.equals(activity.getPackageName())&&seen.add(candidate.activityInfo.packageName+"/"+candidate.activityInfo.name))candidates.add(candidate);
  Collections.sort(candidates,(a,b)->{boolean first=a.activityInfo.packageName.startsWith("me.aap.fermata"),second=b.activityInfo.packageName.startsWith("me.aap.fermata");return first!=second?(first?-1:1):a.loadLabel(manager).toString().compareToIgnoreCase(b.loadLabel(manager).toString());});
  if(candidates.isEmpty()){new AlertDialog.Builder(activity).setTitle("Bilafspiller mangler").setMessage("Installer en videoafspiller, der kan vises i din Android Auto-opsætning, fx Fermata Auto. En almindelig videoafspiller kan kun vise indholdet på telefonen.").setPositiveButton(android.R.string.ok,null).show();return;}
  String[] names=new String[candidates.size()];for(int i=0;i<names.length;i++)names[i]=candidates.get(i).loadLabel(manager).toString();
  new AlertDialog.Builder(activity).setTitle("Vælg bilafspiller").setItems(names,(dialog,index)->{
   ActivityInfo target=candidates.get(index).activityInfo;
   try {activity.startActivity(new Intent(intent).setComponent(new ComponentName(target.packageName,target.name)));launched.run();}
   catch(ActivityNotFoundException|SecurityException error){Toast.makeText(activity,"Afspilleren kunne ikke åbnes. Vælg en anden videoafspiller.",Toast.LENGTH_LONG).show();}
  }).setNegativeButton(android.R.string.cancel,null).show();
 }
 public static void info(Activity activity){
  new AlertDialog.Builder(activity).setTitle("Video i bil").setMessage("Stream kan åbne TV, film og lokale videoer i en ekstern bilafspiller, fx Fermata Auto.\n\nBilafspilleren skal være installeret og kunne vises i Android Auto på din telefon. En almindelig videoafspiller er ikke nok. Brug video, mens bilen er parkeret.\n\nVed åbning af favoritter får den valgte afspiller de gemte streamlinks. Video på bilens skærm er ikke testet eller garanteret på alle biler.").setPositiveButton(android.R.string.ok,null).setNeutralButton("Fermata-projektet",(dialog,which)->{try{activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/AndreyPavlenko/Fermata/releases")));}catch(ActivityNotFoundException ignored){}}).show();
 }
 private CarPlayerBridge(){}
}
