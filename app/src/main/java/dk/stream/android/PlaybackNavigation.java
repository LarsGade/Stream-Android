package dk.stream.android;
import androidx.media3.common.Player;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
@androidx.media3.common.util.UnstableApi
final class PlaybackNavigation {
 static boolean canMove(Player player,int direction){return player!=null&&(direction<0?player.hasPreviousMediaItem():player.hasNextMediaItem());}
 static void move(Player player,int direction){if(!canMove(player,direction))return;if(direction<0)player.seekToPreviousMediaItem();else player.seekToNextMediaItem();}
 static boolean canMove(RemoteMediaClient remote,int direction){if(remote==null||remote.getMediaStatus()==null)return false;com.google.android.gms.cast.MediaStatus status=remote.getMediaStatus();Integer index=status.getIndexById(status.getCurrentItemId());return index!=null&&(direction<0?index>0:index+1<status.getQueueItemCount());}
 static void move(RemoteMediaClient remote,int direction){if(!canMove(remote,direction))return;CastHistory.beforeLoad();if(direction<0)remote.queuePrev(null);else remote.queueNext(null);}
 private PlaybackNavigation(){}
}
