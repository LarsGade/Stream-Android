package dk.stream.android;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.*;
import org.json.*;

@androidx.media3.common.util.UnstableApi
public final class MediaDownloadService extends Service {
 private static final String PAUSE="pause",RESUME="resume",CANCEL="cancel";
 private static final int NOTICE=2010;
 private static final class Task {
  final String plan;volatile Uri destination;final boolean automatic;final DownloadControl control=new DownloadControl();
  volatile boolean finished;volatile String name="Starter …",file="";volatile int total=1,done;volatile long copied,expected=-1;
  Task(String plan,Uri destination){this.plan=plan;this.destination=destination;this.automatic=destination==null;}
 }
 private final ExecutorService worker=Executors.newSingleThreadExecutor(),closer=Executors.newSingleThreadExecutor();
 private final AtomicInteger jobs=new AtomicInteger();private volatile Task active;private volatile Call activeCall;private volatile InputStream activeInput;
 private volatile boolean stopping;private volatile WebDavClient cloud;private PowerManager.WakeLock wake;
 private final OkHttpClient http=new OkHttpClient.Builder().connectTimeout(20,TimeUnit.SECONDS).readTimeout(60,TimeUnit.SECONDS).build();private long lastUpdate;private int lastStartId;
 @Override public void onCreate(){super.onCreate();if(Build.VERSION.SDK_INT>=26)((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("downloads","Download",NotificationManager.IMPORTANCE_LOW));wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Stream:downloads");wake.acquire(6*60*60*1000L);}
 @Override public IBinder onBind(Intent intent){return null;}
 private PendingIntent action(Task task,String action,int id){return PendingIntent.getService(this,id,new Intent(this,MediaDownloadService.class).setAction(action).putExtra("job",task.plan),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 private Notification notice(Task task){boolean paused=task.control.paused();boolean cancelled=task.control.cancelled();int progress=DownloadProgress.basisPoints(task.done,task.total,task.copied,task.expected);Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"downloads"):new Notification.Builder(this);builder.setSmallIcon(R.drawable.brand_icon).setContentTitle("Download").setContentText(UiText.text((progress/100)+" % hentet · "+(cancelled?"Annullerer … · ":paused?"Sat på pause · ":"")+task.name)).setSubText((Math.min(task.total,task.done+1))+" / "+task.total+(task.file.isEmpty()?"":" · "+task.file)).setOngoing(true).setOnlyAlertOnce(true).setProgress(10000,progress,!paused&&!cancelled&&task.total==1&&task.expected<0);if(!cancelled){builder.addAction(paused?R.drawable.ic_play:R.drawable.ic_pause,paused?"Fortsæt":"Pause",action(task,paused?RESUME:PAUSE,2012));builder.addAction(R.drawable.ic_stop,"Annuller",action(task,CANCEL,2013));}return builder.build();}
 @android.annotation.SuppressLint("MissingPermission") private void update(Task task){if(active==task)((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTICE,notice(task));}
 @Override public int onStartCommand(Intent intent,int flags,int startId){
  lastStartId=startId;
  if(intent==null){stopSelf(startId);return START_NOT_STICKY;}
  String command=intent.getAction();if(PAUSE.equals(command)||RESUME.equals(command)||CANCEL.equals(command)){Task task=active;if(task==null){if(jobs.get()==0)stopSelf();return START_NOT_STICKY;}if(task.finished||!Objects.equals(task.plan,intent.getStringExtra("job")))return START_NOT_STICKY;
   if(PAUSE.equals(command)){task.control.pause();Call call=activeCall;if(call!=null)call.cancel();if(wake.isHeld())wake.release();}
   else if(RESUME.equals(command)){if(!wake.isHeld())wake.acquire(6*60*60*1000L);task.control.resume();}
   else{task.control.cancel();Call call=activeCall;if(call!=null)call.cancel();InputStream input=activeInput;if(input!=null)closer.execute(()->{try{input.close();}catch(IOException ignored){}});}
   update(task);return START_NOT_STICKY;
  }
  if(!intent.hasExtra("plan")){if(jobs.get()==0)stopSelf();return START_NOT_STICKY;}Task task=new Task(intent.getStringExtra("plan"),intent.getData());jobs.incrementAndGet();if(active==null)active=task;startForeground(NOTICE,notice(active));worker.execute(()->download(task));return START_NOT_STICKY;
 }
 private void download(Task task){active=task;boolean success=false,writing=false;try{
  task.control.await();if(!wake.isHeld())wake.acquire(6*60*60*1000L);JSONObject plan=MediaDownloads.read(this,task.plan);task.name=plan.optString("name","Klargør …");update(task);if(!plan.has("files"))plan=DownloadPlanner.plan(this,plan,task.control);checkpoint(task);task.name=plan.getString("name");JSONArray files=plan.getJSONArray("files");task.total=files.length();update(task);if(task.destination==null)task.destination=DownloadDestination.create(this,plan);
  for(int i=0;i<files.length();i++)if(task.destination.toString().equals(files.getJSONObject(i).getString("uri")))throw new IOException("Source and destination are identical");
  Library library=new LibraryStore(this).load();cloud=library.pcloud==null?null:new WebDavClient(library.pcloud);lastUpdate=0;
  try(OutputStream raw=getContentResolver().openOutputStream(task.destination,"wt")){if(raw==null)throw new IOException("Cannot write destination");writing=true;List<String> names=new ArrayList<>();for(int i=0;i<files.length();i++)names.add(files.getJSONObject(i).getString("name"));DownloadArchive.write(names,raw,(index,output)->copy(files.getJSONObject(index),output,library,task,index));}DownloadDestination.publish(this,task.destination);success=true;
 }catch(Exception failure){if(!task.control.cancelled()&&!stopping)new Handler(Looper.getMainLooper()).post(()->UiText.toast(this,"Download mislykkedes. Kontrollér netværk og adgang til filerne.",android.widget.Toast.LENGTH_LONG).show());}
 finally{success=success&&!task.control.cancelled();if(!success&&task.destination!=null&&(writing||task.automatic))DownloadDestination.remove(this,task.destination);if(cloud!=null)cloud.cancel();cloud=null;activeCall=null;activeInput=null;try{MediaDownloads.file(this,task.plan).delete();}catch(Exception ignored){}task.finished=true;boolean completed=success;new Handler(Looper.getMainLooper()).post(()->{if(active==task)active=null;boolean last=jobs.decrementAndGet()==0;if(last)androidx.core.app.ServiceCompat.stopForeground(this,androidx.core.app.ServiceCompat.STOP_FOREGROUND_REMOVE);if(!task.control.cancelled()&&!stopping)complete(task,completed);if(last)stopSelf(lastStartId);});}
 }
 private void checkpoint(Task task)throws IOException{if(stopping)throw new InterruptedIOException();task.control.await();}
 private void copy(JSONObject file,OutputStream output,Library library,Task task,int index)throws Exception{
  String uri=file.getString("uri");task.file=file.getString("name");task.done=index;task.copied=0;task.expected=-1;long offset=0;InputStream input=null;int revision=task.control.revision();byte[] buffer=new byte[65536];
  try{while(true){checkpoint(task);try{
   if(input==null){revision=task.control.revision();input=open(uri,library,task,offset);activeInput=input;if(input==null)throw new IOException("Cannot read file");update(task);}
   checkpoint(task);int n=input.read(buffer);if(n<0)break;checkpoint(task);output.write(buffer,0,n);offset+=n;task.copied=offset;long now=SystemClock.elapsedRealtime();if(now-lastUpdate>=500){lastUpdate=now;update(task);}
  }catch(IOException failure){if(input!=null)try{input.close();}catch(IOException ignored){}input=null;activeInput=null;activeCall=null;if(stopping||task.control.cancelled()||revision==task.control.revision())throw failure;checkpoint(task);}}
   task.done=index+1;task.copied=0;task.expected=-1;update(task);
  }finally{if(input!=null)input.close();activeInput=null;activeCall=null;}
 }
 private InputStream open(String value,Library library,Task task,long offset)throws IOException{
  if(PcloudDocument.is(value)){if(cloud==null)throw new IOException("pCloud account missing");task.expected=PcloudDocument.size(value);return cloud.open(PcloudDocument.url(library.pcloud,value),offset,call->activeCall=call);}
  if(value.startsWith("content:")){Uri uri=Uri.parse(value);try(android.database.Cursor cursor=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.SIZE},null,null,null)){if(cursor!=null&&cursor.moveToFirst()&&!cursor.isNull(0))task.expected=cursor.getLong(0);}InputStream input=getContentResolver().openInputStream(uri);if(input==null)throw new IOException("Cannot read source");try{DownloadRange.skip(input,offset);return input;}catch(IOException error){input.close();throw error;}}
  Request.Builder request=new Request.Builder().url(Playlist.httpUrl(value));if(offset>0)request.header("Range","bytes="+offset+"-");Call call=http.newCall(request.build());activeCall=call;Response response=call.execute();
  try{if(!response.isSuccessful()||response.body()==null)throw new IOException("HTTP download failed");if(response.code()==206)DownloadRange.validate(response.header("Content-Range"),offset);task.expected=response.code()==206?DownloadRange.total(response.header("Content-Range"),response.body().contentLength()<0?-1:offset+response.body().contentLength()):response.body().contentLength();InputStream input=response.body().byteStream();if(offset>0&&response.code()==200)DownloadRange.skip(input,offset);return new FilterInputStream(input){@Override public void close()throws IOException{try{super.close();}finally{response.close();}}};}catch(IOException error){response.close();throw error;}
 }
 @android.annotation.SuppressLint("MissingPermission") private void complete(Task task,boolean success){if(success)new Handler(Looper.getMainLooper()).post(()->UiText.toast(this,"Download færdig: "+task.name,android.widget.Toast.LENGTH_LONG).show());Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"downloads"):new Notification.Builder(this);builder.setSmallIcon(R.drawable.brand_icon).setContentTitle(UiText.text(success?"Download færdig":"Download mislykkedes")).setContentText(task.name).setAutoCancel(true);if(success){Intent open=new Intent(Intent.ACTION_VIEW).setDataAndType(task.destination,"*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);builder.setContentIntent(PendingIntent.getActivity(this,2011,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));}((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(2011,builder.build());}
 @Override public void onTimeout(int startId,int type){stopping=true;Task task=active;if(task!=null)task.control.cancel();Call call=activeCall;if(call!=null)call.cancel();http.dispatcher().cancelAll();if(cloud!=null)cloud.cancel();stopSelf();}
 @Override public void onDestroy(){stopping=true;Task task=active;if(task!=null)task.control.cancel();Call call=activeCall;if(call!=null)call.cancel();http.dispatcher().cancelAll();if(cloud!=null)cloud.cancel();worker.shutdownNow();closer.shutdownNow();if(wake!=null&&wake.isHeld())wake.release();super.onDestroy();}
}
