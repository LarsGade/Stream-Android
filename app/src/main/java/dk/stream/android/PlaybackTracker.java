package dk.stream.android;
import android.content.Context;
import android.os.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import java.util.*;

@androidx.media3.common.util.UnstableApi
final class PlaybackTracker implements Player.Listener {
 private static final Map<ExoPlayer,PlaybackTracker> trackers=new HashMap<>();
 private final Context context;private final ExoPlayer player;private final Handler handler=new Handler(Looper.getMainLooper());
 private String lastKey="";private long lastPosition,lastDuration,ignoreOldUntil;private boolean closed;
 private final Runnable tick=new Runnable(){public void run(){if(!closed){capture(false);handler.postDelayed(this,1000);}}};
 private PlaybackTracker(Context context,ExoPlayer player){this.context=context.getApplicationContext();this.player=player;player.addListener(this);handler.post(tick);}
 static void attach(Context context,ExoPlayer player){if(!trackers.containsKey(player))trackers.put(player,new PlaybackTracker(context,player));}
 static void detach(ExoPlayer player){PlaybackTracker tracker=trackers.remove(player);if(tracker!=null){tracker.capture(true);tracker.closed=true;tracker.handler.removeCallbacksAndMessages(null);player.removeListener(tracker);}}
 static String key(MediaItem item){if(item==null)return "";return item.localConfiguration==null?item.mediaId:item.localConfiguration.uri.toString();}
 private boolean live(){MediaItem item=player.getCurrentMediaItem();return player.isCurrentMediaItemLive()||item!=null&&item.mediaMetadata.extras!=null&&item.mediaMetadata.extras.getBoolean("history_live",false);}
 private void capture(boolean force){if(closed||live())return;String key=key(player.getCurrentMediaItem());long duration=player.getDuration(),position=player.getCurrentPosition();if(key.isEmpty()||duration<=0)return;lastKey=key;lastPosition=position;lastDuration=duration;PlaybackHistory.record(context,key,position,duration,player.getPlaybackState()==Player.STATE_ENDED,force);}
 @Override public void onEvents(Player source,Player.Events events){capture(!source.getPlayWhenReady()||source.getPlaybackState()==Player.STATE_ENDED);}
 @Override public void onPositionDiscontinuity(Player.PositionInfo old,Player.PositionInfo next,int reason){if(SystemClock.elapsedRealtime()<ignoreOldUntil)return;String key=key(old.mediaItem);boolean wasLive=old.mediaItem!=null&&old.mediaItem.mediaMetadata.extras!=null&&old.mediaItem.mediaMetadata.extras.getBoolean("history_live",false);if(wasLive)return;long duration=key.equals(lastKey)?lastDuration:PlaybackHistory.get(context,key).duration;PlaybackHistory.record(context,key,old.positionMs,duration,reason==Player.DISCONTINUITY_REASON_AUTO_TRANSITION,true);}
 @Override public void onMediaItemTransition(MediaItem item,int reason){if(reason==Player.MEDIA_ITEM_TRANSITION_REASON_AUTO&&!lastKey.isEmpty())PlaybackHistory.record(context,lastKey,lastDuration,lastDuration,true,true);lastKey="";lastDuration=0;if(reason==Player.MEDIA_ITEM_TRANSITION_REASON_AUTO||reason==Player.MEDIA_ITEM_TRANSITION_REASON_SEEK){String key=key(item);long position=PlaybackHistory.get(context,key).resume();if(position>0)handler.post(()->{if(!closed&&key.equals(key(player.getCurrentMediaItem())))player.seekTo(position);});}}
 static void flushAll(){for(PlaybackTracker tracker:new ArrayList<>(trackers.values()))tracker.capture(true);}
 static void detachAll(){for(ExoPlayer player:new ArrayList<>(trackers.keySet()))detach(player);}
 static void resetPlayers(){for(PlaybackTracker tracker:new ArrayList<>(trackers.values())){tracker.handler.removeCallbacksAndMessages(null);tracker.handler.post(tracker.tick);tracker.lastKey="";tracker.lastDuration=0;tracker.ignoreOldUntil=SystemClock.elapsedRealtime()+2000;if(!tracker.live()&&tracker.player.getCurrentMediaItem()!=null)tracker.player.seekTo(0);}}
 private PlaybackTracker(){throw new AssertionError();}
}
