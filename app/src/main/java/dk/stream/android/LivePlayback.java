package dk.stream.android;

import com.google.android.gms.cast.framework.media.RemoteMediaClient;

/** Live controls apply only to live channels, never to ordinary files. */
public final class LivePlayback {
 public static boolean behind(boolean live,boolean paused,long position,Long liveEdge){return live&&(paused||liveEdge!=null&&position<liveEdge-5000);}
 public static boolean behind(RemoteMediaClient remote){
  if(remote==null||!remote.isLiveStream())return false;
  if(remote.isPaused())return true;
  com.google.android.gms.cast.MediaStatus status=remote.getMediaStatus();
  com.google.android.gms.cast.MediaLiveSeekableRange range=status==null?null:status.getLiveSeekableRange();
  return behind(true,false,remote.getApproximateStreamPosition(),range==null?null:range.getEndTime());
 }
 public static void goLive(RemoteMediaClient remote){if(remote!=null&&remote.isLiveStream()){remote.seek(new com.google.android.gms.cast.MediaSeekOptions.Builder().setIsSeekToInfinite(true).build());remote.play();}}
 private LivePlayback(){}
}
