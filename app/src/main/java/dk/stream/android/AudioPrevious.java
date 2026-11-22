package dk.stream.android;

import androidx.media3.common.Player;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

/** Restart after three seconds; a second quick press skips even while Cast's position is stale. */
@androidx.media3.common.util.UnstableApi
final class AudioPrevious {
 enum Action {NONE,RESTART,PREVIOUS}
 private String restarted;private long restartedAt;
 Action choose(String key,long position,boolean seekable,boolean previous,long now){
  boolean doublePress=restarted!=null&&restarted.equals(key)&&now>=restartedAt&&now-restartedAt<=600;
  if(previous&&(doublePress||!seekable||position<=3000)){restarted=null;return Action.PREVIOUS;}
  if(seekable){restarted=key;restartedAt=now;return Action.RESTART;}
  restarted=null;return Action.NONE;
 }
 private static boolean seekable(Player player){return player!=null&&!player.isCurrentMediaItemLive()&&player.isCurrentMediaItemSeekable();}
 private static boolean seekable(RemoteMediaClient remote){return remote!=null&&!remote.isLiveStream()&&remote.getStreamDuration()>0;}
 static boolean available(Player player){return seekable(player)||PlaybackNavigation.canMove(player,-1);}
 static boolean available(RemoteMediaClient remote){return seekable(remote)||PlaybackNavigation.canMove(remote,-1);}
 void press(Player player){if(player==null)return;androidx.media3.common.MediaItem item=player.getCurrentMediaItem();String key=(item==null?"":item.mediaId)+":"+player.getCurrentMediaItemIndex();Action action=choose(key,player.getCurrentPosition(),seekable(player),PlaybackNavigation.canMove(player,-1),android.os.SystemClock.elapsedRealtime());if(action==Action.RESTART)player.seekTo(0);else if(action==Action.PREVIOUS)PlaybackNavigation.move(player,-1);}
 void press(RemoteMediaClient remote){if(remote==null)return;String key=(remote.getMediaInfo()==null?"":remote.getMediaInfo().getContentId())+":"+(remote.getMediaStatus()==null?0:remote.getMediaStatus().getCurrentItemId());Action action=choose(key,remote.getApproximateStreamPosition(),seekable(remote),PlaybackNavigation.canMove(remote,-1),android.os.SystemClock.elapsedRealtime());if(action==Action.RESTART)remote.seek(new com.google.android.gms.cast.MediaSeekOptions.Builder().setPosition(0).build());else if(action==Action.PREVIOUS)PlaybackNavigation.move(remote,-1);}
}
