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
 private static LocalQueue pending;private static long pendingPosition;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private volatile LocalCastHttpServer server;
 private volatile int generation;
 private PowerManager.WakeLock wake;
 private android.net.wifi.WifiManager.WifiLock wifi;
 @Override public void onCreate(){super.onCreate();wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Stream:local-cast");wake.acquire();android.net.wifi.WifiManager manager=(android.net.wifi.WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);if(manager!=null){wifi=manager.createWifiLock(android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF,"Stream:local-cast");wifi.acquire();}}
 public static void cast(Context context,LocalQueue queue,long position){LocalMedia selected=queue.entries.get(queue.start);pending=selected.image()?new LocalQueue(Collections.singletonList(selected),selected):queue;pendingPosition=position;try{androidx.core.content.ContextCompat.startForegroundService(context,new Intent(context,LocalCastService.class));}catch(RuntimeException denied){pending=null;android.util.Log.e("StreamCast","Could not start Cast service",denied);CastPlayback.failed(context);}}
 @Override public IBinder onBind(Intent intent){return null;}
 @Override public int onStartCommand(Intent intent,int flags,int id){
  if(intent!=null&&"stop".equals(intent.getAction())){CastPlayback.stop(this);stopSelf();return START_NOT_STICKY;}
  if(Build.VERSION.SDK_INT>=26)((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("local_cast","Casting",NotificationManager.IMPORTANCE_LOW));
  PendingIntent open=PendingIntent.getActivity(this,91,(pending!=null&&pending.entries.get(pending.start).image()?ImageViewerActivity.reopenIntent(this):new Intent(this,AudioPlayerActivity.class).putExtra("cast",true)),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  PendingIntent stop=PendingIntent.getService(this,92,new Intent(this,LocalCastService.class).setAction("stop"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder notification=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"local_cast"):new Notification.Builder(this);
  startForeground(2004,notification.setSmallIcon(R.drawable.ic_play).setContentTitle("Caster lokale filer").setContentText("Stream").setContentIntent(open).setOngoing(true).addAction(R.drawable.ic_stop,"Stop",stop).build());
  LocalQueue queue=pending;long position=pendingPosition;pending=null;if(queue==null){stopSelf();return START_NOT_STICKY;}
  com.google.android.gms.cast.framework.CastSession session=CastPlayback.session(this);if(session==null||session.getCastDevice()==null){stopSelf();return START_NOT_STICKY;}
  InetAddress receiver=session.getCastDevice().getInetAddress();String sessionId=session.getSessionId();int request=++generation;
  worker.execute(()->{try {
   InetAddress address;try(DatagramSocket route=new DatagramSocket()){route.connect(receiver,9);address=route.getLocalAddress();}
   LocalCastHttpServer next=new LocalCastHttpServer(address);if(server!=null)server.close();server=next;
   List<String> urls=new ArrayList<>(),imageTypes=new ArrayList<>();for(LocalMedia file:queue.entries){if(generation!=request){next.close();return;}if(file.image()){
    com.bumptech.glide.request.FutureTarget<android.graphics.Bitmap> target=com.bumptech.glide.Glide.with(getApplicationContext()).asBitmap().load(Uri.parse(file.uri)).fitCenter().diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE).skipMemoryCache(true).submit(1920,1080);
    try{android.graphics.Bitmap bitmap=target.get();boolean alpha=bitmap.hasAlpha();ByteArrayOutputStream bytes=new ByteArrayOutputStream();if(!bitmap.compress(alpha?android.graphics.Bitmap.CompressFormat.PNG:android.graphics.Bitmap.CompressFormat.JPEG,90,bytes))throw new IOException("Could not prepare image");byte[] data=bytes.toByteArray();String type=alpha?"image/png":"image/jpeg";urls.add(next.add(()->new ByteArrayInputStream(data),data.length,type));imageTypes.add(type);}finally{com.bumptech.glide.Glide.with(getApplicationContext()).clear(target);}continue;
   }if(PcloudDocument.is(file.uri)){PcloudAccount account=new LibraryStore(this).load().pcloud;okhttp3.HttpUrl remote=PcloudDocument.url(account,file.uri);WebDavClient webdav=new WebDavClient(account);urls.add(next.addRange(offset->webdav.open(remote,offset),PcloudDocument.size(file.uri),CastPlayback.mime(file)));continue;}Uri uri=Uri.parse(file.uri);long size=-1;
    try(Cursor cursor=getContentResolver().query(uri,new String[]{OpenableColumns.SIZE},null,null,null)){if(cursor!=null&&cursor.moveToFirst()&&!cursor.isNull(0))size=cursor.getLong(0);}
    urls.add(next.add(()->{InputStream input=getContentResolver().openInputStream(uri);if(input==null)throw new FileNotFoundException();return input;},size,CastPlayback.mime(file)));
   }
   new Handler(Looper.getMainLooper()).post(()->{com.google.android.gms.cast.framework.CastSession current=CastPlayback.session(this);if(generation!=request)return;if(current!=null&&Objects.equals(sessionId,current.getSessionId())){LocalMedia currentFile=queue.entries.get(queue.start);if(currentFile.image())CastPlayback.loadImage(this,currentFile,urls.get(queue.start),imageTypes.get(queue.start));else CastPlayback.loadLocal(this,queue,urls,position);}else stopSelf();});
  }catch(Exception error){android.util.Log.e("StreamCast","Could not serve local queue",error);new Handler(Looper.getMainLooper()).post(()->{if(generation==request){CastPlayback.failed(this);stopSelf();}});}});
  return START_NOT_STICKY;
 }
 @Override public void onDestroy(){generation++;if(server!=null)server.close();worker.shutdownNow();if(wifi!=null&&wifi.isHeld())wifi.release();if(wake!=null&&wake.isHeld())wake.release();super.onDestroy();}
}
