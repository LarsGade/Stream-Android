package dk.stream.android;
import android.content.Context;
import android.os.*;
import com.google.android.gms.cast.*;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

@androidx.media3.common.util.UnstableApi
final class CastHistory {
 private static final Handler handler=new Handler(Looper.getMainLooper());private static boolean started;private static Context application;private static String lastKey="";private static long lastPosition,lastDuration,ignoreUntil;
 private static final Runnable tick=new Runnable(){public void run(){if(started){capture(false);if(started)handler.postDelayed(this,1000);}}};
 static void start(Context context){application=context.getApplicationContext();if(!started){started=true;handler.post(tick);}}
 static void capture(boolean force){if(application==null||SystemClock.elapsedRealtime()<ignoreUntil)return;RemoteMediaClient remote=CastPlayback.remote(application);if(remote==null){flush();lastKey="";started=false;return;}if(remote.getMediaInfo()==null||remote.isLiveStream()||CastPlayback.image(application)){flush();lastKey="";return;}String key=CastPlayback.historyKey(application);long duration=remote.getStreamDuration(),position=remote.getApproximateStreamPosition();if(key.isEmpty()||duration<=0)return;
  if(!key.equals(lastKey)){if(lastDuration>0&&lastPosition>=lastDuration-2000)lastPosition=lastDuration;flush();lastKey=key;PlaybackMark saved=PlaybackHistory.get(application,key);long resume=saved.resume();if(position<1000&&resume>1000){lastDuration=duration;lastPosition=resume;remote.seek(new MediaSeekOptions.Builder().setPosition(resume).build());return;}}
  boolean complete=remote.getMediaStatus()!=null&&remote.getPlayerState()==MediaStatus.PLAYER_STATE_IDLE&&remote.getMediaStatus().getIdleReason()==MediaStatus.IDLE_REASON_FINISHED;
  lastDuration=duration;lastPosition=complete?duration:position;PlaybackHistory.record(application,key,lastPosition,duration,complete,force||remote.isPaused());
 }
 private static void flush(){if(application!=null&&!lastKey.isEmpty())PlaybackHistory.record(application,lastKey,lastPosition,lastDuration,lastPosition>=lastDuration&&lastDuration>0,true);}
 static void beforeLoad(){capture(true);flush();lastKey="";lastPosition=0;lastDuration=0;}
 static void stop(){capture(true);flush();lastKey="";started=false;handler.removeCallbacks(tick);}
 static void reset(Context context){lastKey="";lastPosition=0;lastDuration=0;ignoreUntil=SystemClock.elapsedRealtime()+2000;RemoteMediaClient remote=CastPlayback.remote(context);if(remote!=null&&!remote.isLiveStream()&&!CastPlayback.image(context))remote.seek(new MediaSeekOptions.Builder().setPosition(0).build());}
 private CastHistory(){}
}
