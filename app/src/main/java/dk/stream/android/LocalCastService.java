package dk.stream.android;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** Keeps only the selected Cast queue available while the receiver fetches media. */
@androidx.media3.common.util.UnstableApi
public final class LocalCastService extends Service {
 private static RadioStation pendingRadio;
 private static LocalQueue pending;private static long pendingPosition;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final ExecutorService coverWorker=Executors.newSingleThreadExecutor(task->{Thread thread=new Thread(task,"cast-file-covers");thread.setDaemon(true);return thread;});
 private final LocalCastServers servers=new LocalCastServers();
 private volatile int generation;
 private PowerManager.WakeLock wake;
 private android.net.wifi.WifiManager.WifiLock wifi;
 @Override public void onCreate(){super.onCreate();wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Stream:local-cast");wake.acquire();android.net.wifi.WifiManager manager=(android.net.wifi.WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);if(manager!=null){wifi=manager.createWifiLock(android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF,"Stream:local-cast");wifi.acquire();}}
 public static void cast(Context context,LocalQueue queue,long position){pendingRadio=null;LocalMedia selected=queue.entries.get(queue.start);pending=selected.image()?new LocalQueue(Collections.singletonList(selected),selected):queue;pendingPosition=position;try{androidx.core.content.ContextCompat.startForegroundService(context,new Intent(context,LocalCastService.class).putExtra("selection",CastPlayback.selection()));}catch(RuntimeException denied){pending=null;android.util.Log.e("StreamCast","Could not start Cast service",denied);CastPlayback.failed(context);}}
 public static void castRadio(Context context,RadioStation station){pending=null;pendingRadio=station;try{androidx.core.content.ContextCompat.startForegroundService(context,new Intent(context,LocalCastService.class).putExtra("selection",CastPlayback.selection()));}catch(RuntimeException denied){pendingRadio=null;CastPlayback.failed(context);}}
 @Override public IBinder onBind(Intent intent){return null;}
 @Override public int onStartCommand(Intent intent,int flags,int id){
  if(intent!=null&&"stop".equals(intent.getAction())){CastPlayback.stopCasting(this);stopSelf();return START_NOT_STICKY;}
  final int selectedRequest=intent==null?CastPlayback.selection():intent.getIntExtra("selection",CastPlayback.selection());
  if(Build.VERSION.SDK_INT>=26)((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("local_cast","Casting",NotificationManager.IMPORTANCE_LOW));
  PendingIntent open=PlaybackNotices.open(this,91);
  PendingIntent stop=PendingIntent.getService(this,92,new Intent(this,LocalCastService.class).setAction("stop"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder notification=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"local_cast"):new Notification.Builder(this);
  startForeground(2004,notification.setSmallIcon(R.drawable.brand_icon).setContentTitle(UiText.text(pendingRadio!=null?"Caster radio":"Caster lokale filer")).setContentText("Stream").setContentIntent(open).setOngoing(true).addAction(R.drawable.ic_stop,"Stop",stop).build());
  if(!CastPlayback.selectionCurrent(selectedRequest))return START_NOT_STICKY;
  LocalQueue queue=pending;RadioStation radio=pendingRadio;long position=pendingPosition;pending=null;pendingRadio=null;if(queue==null&&radio==null){stopSelf();return START_NOT_STICKY;}
  com.google.android.gms.cast.framework.CastSession session=CastPlayback.session(this);if(session==null||session.getCastDevice()==null){stopSelf();return START_NOT_STICKY;}
  InetAddress receiver=session.getCastDevice().getInetAddress();String sessionId=session.getSessionId();int request=++generation;
  worker.execute(()->{try {if(generation!=request||!CastPlayback.selectionCurrent(selectedRequest))return;
   InetAddress address;try(DatagramSocket route=new DatagramSocket()){route.connect(receiver,9);address=route.getLocalAddress();}
   LocalCastHttpServer next=new LocalCastHttpServer(address);if(!servers.add(next))return;
   if(radio!=null){
    boolean bundled=PlaybackArtwork.bundled(this,radio.name)||PlaybackArtwork.bundled(this,radio.displayName());
    byte[] artwork=bundled?PlaybackArtwork.local(this,PlaybackArtwork.logoName(this,radio)):PlaybackArtwork.remote(this,radio.logo);
    String logo=artwork==null?(okhttp3.HttpUrl.parse(radio.logo)!=null?radio.logo:null):next.add(()->new ByteArrayInputStream(artwork),artwork.length,"image/png");
    new Handler(Looper.getMainLooper()).post(()->{com.google.android.gms.cast.framework.CastSession current=CastPlayback.session(this);if(generation!=request||!CastPlayback.selectionCurrent(selectedRequest))return;if(current!=null&&Objects.equals(sessionId,current.getSessionId())){servers.activate(next);CastPlayback.loadRadio(this,radio,logo);}else stopSelf();});return;
   }
   PcloudAccount account=null;for(LocalMedia file:queue.entries)if(PcloudDocument.is(file.uri)){account=new LibraryStore(this).load().pcloud;break;}
   final PcloudAccount cloudAccount=account;final WebDavClient cloud=account==null?null:new WebDavClient(account);
   List<String> urls=new ArrayList<>(),imageTypes=new ArrayList<>();for(LocalMedia file:queue.entries){if(generation!=request||!CastPlayback.selectionCurrent(selectedRequest)){next.close();return;}if(file.image()){
    com.bumptech.glide.request.FutureTarget<android.graphics.Bitmap> target=com.bumptech.glide.Glide.with(getApplicationContext()).asBitmap().load(Uri.parse(file.uri)).fitCenter().diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE).skipMemoryCache(true).submit(1920,1080);
    try{android.graphics.Bitmap bitmap=target.get();boolean alpha=bitmap.hasAlpha();ByteArrayOutputStream bytes=new ByteArrayOutputStream();if(!bitmap.compress(alpha?android.graphics.Bitmap.CompressFormat.PNG:android.graphics.Bitmap.CompressFormat.JPEG,90,bytes))throw new IOException("Could not prepare image");byte[] data=bytes.toByteArray();String type=alpha?"image/png":"image/jpeg";urls.add(next.add(()->new ByteArrayInputStream(data),data.length,type));imageTypes.add(type);}finally{com.bumptech.glide.Glide.with(getApplicationContext()).clear(target);}continue;
   }if(SaavnMusic.is(file.uri)){urls.add(next.addDeferred(()->{SaavnSong song=SaavnMusic.resolve(this,SaavnMusic.id(file.uri),false);okhttp3.HttpUrl musicUrl=okhttp3.HttpUrl.get(song.stream);long musicSize=HttpMediaFile.size(SaavnApi.STREAM,musicUrl);return new LocalCastHttpServer.Media(offset->CastMusicStream.open(SaavnApi.STREAM,musicUrl,offset),musicSize,SaavnMusic.media(song).mime);}));continue;}if(PcloudDocument.is(file.uri)){okhttp3.HttpUrl remote=PcloudDocument.url(cloudAccount,file.uri);urls.add(next.addRange(offset->cloud.open(remote,offset),PcloudDocument.size(file.uri),CastPlayback.mime(file)));continue;}Uri uri=Uri.parse(file.uri);long size=-1;
    try(Cursor cursor=getContentResolver().query(uri,new String[]{OpenableColumns.SIZE},null,null,null)){if(cursor!=null&&cursor.moveToFirst()&&!cursor.isNull(0))size=cursor.getLong(0);}
    urls.add(next.add(()->{InputStream input=getContentResolver().openInputStream(uri);if(input==null)throw new FileNotFoundException();return input;},size,CastPlayback.mime(file)));
   }
   List<String> logos=new ArrayList<>();Map<String,String> namedLogos=new HashMap<>();for(int index=0;index<queue.entries.size();index++){if(generation!=request||!CastPlayback.selectionCurrent(selectedRequest))return;LocalMedia file=queue.entries.get(index);String asset=LocalFileArtwork.asset(file);String logo;if(asset!=null&&namedLogos.containsKey(asset))logo=namedLogos.get(asset);else{logo=CastFileArtwork.publish(next,CastFileArtwork.available(this,file));if(asset!=null)namedLogos.put(asset,logo);}if(logo==null&&SaavnMusic.is(file.uri)){SaavnSong song=SaavnMusic.peek(SaavnMusic.id(file.uri));if(song!=null){okhttp3.HttpUrl cover=okhttp3.HttpUrl.parse(song.image);if(cover!=null)logo=cover.toString();}}logos.add(logo);}
   new Handler(Looper.getMainLooper()).post(()->{com.google.android.gms.cast.framework.CastSession current=CastPlayback.session(this);if(generation!=request||!CastPlayback.selectionCurrent(selectedRequest))return;if(current!=null&&Objects.equals(sessionId,current.getSessionId())){LocalMedia currentFile=queue.entries.get(queue.start);if(currentFile.image()){servers.activate(next);CastPlayback.loadImage(this,currentFile,urls.get(queue.start),imageTypes.get(queue.start));}else CastPlayback.loadLocal(this,queue,urls,logos,position,()->generation==request&&CastPlayback.selectionCurrent(selectedRequest)&&!coverWorker.isShutdown(),()->{servers.activate(next);coverWorker.execute(()->{for(int offset=0;offset<queue.entries.size();offset++){if(generation!=request||!CastPlayback.selectionCurrent(selectedRequest))return;int index=(queue.start+offset)%queue.entries.size();LocalMedia file=queue.entries.get(index);if(!file.audio()||logos.get(index)!=null)continue;byte[] data=CastFileArtwork.read(this,file,urls.get(index));if(generation!=request||!CastPlayback.selectionCurrent(selectedRequest))return;String logo=CastFileArtwork.publish(next,data);if(logo==null)continue;new Handler(Looper.getMainLooper()).post(()->{com.google.android.gms.cast.framework.CastSession active=CastPlayback.session(this);if(generation==request&&CastPlayback.selectionCurrent(selectedRequest)&&active!=null&&Objects.equals(sessionId,active.getSessionId())){CastPlayback.fileArtwork(file,data);CastPlayback.updateFileArtwork(this,urls.get(index),logo);}});}});});}else stopSelf();});
  }catch(Exception error){android.util.Log.e("StreamCast","Could not serve local queue",error);new Handler(Looper.getMainLooper()).post(()->{if(generation==request){CastPlayback.failed(this);stopSelf();}});}});
  return START_NOT_STICKY;
 }
 @Override public void onTaskRemoved(Intent root){/* The receiver still needs the media relay after the UI closes. */}
 @Override public void onDestroy(){androidx.core.app.ServiceCompat.stopForeground(this,androidx.core.app.ServiceCompat.STOP_FOREGROUND_REMOVE);generation++;servers.close();worker.shutdownNow();coverWorker.shutdownNow();if(wifi!=null&&wifi.isHeld())wifi.release();if(wake!=null&&wake.isHeld())wake.release();super.onDestroy();}
}
