package dk.stream.android;

import android.app.PendingIntent;
import android.content.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.*;
import androidx.media3.session.*;
import java.util.*;

/** Owns local playback across screens, including mixed audio/video queues. */
@androidx.media3.common.util.UnstableApi
public final class LocalAudioService extends MediaSessionService {
 public static final String PLAY="local.PLAY",TOGGLE="local.TOGGLE",STOP="local.STOP",CHANGED="dk.stream.android.LOCAL_CHANGED";
 private static LocalAudioService active;
 private static LocalQueue pendingQueue;
 private static String pendingToken;
 private ExoPlayer player;
 private MediaSession session;
 private LocalMedia current;
 private List<LocalMedia> queue=Collections.emptyList();
 public static List<LocalMedia> queue(){return active==null?Collections.emptyList():new ArrayList<>(active.queue);}
 public static ExoPlayer player() {return active==null?null:active.player;}
 public static LocalMedia current() {return active==null?null:active.current;}
 public static boolean playing(String uri) {return current()!=null&&current().uri.equals(uri)&&player()!=null&&player().getPlayWhenReady()&&player().getPlaybackState()!=Player.STATE_ENDED&&player().getPlayerError()==null;}
 public static Intent command(Context context,String action) {return new Intent(context,LocalAudioService.class).setAction(action);}
 public static Intent playIntent(Context context,LocalMedia entry) {return playIntent(context,entry,Collections.singletonList(entry));}
 public static Intent playIntent(Context context,LocalMedia entry,List<LocalMedia> displayed) {
  // Pass only a small token through Binder, even for very large folders. If the process
  // dies before delivery, the selected URI remains a valid single-item fallback.
  pendingQueue=new LocalQueue(displayed,entry);pendingToken=UUID.randomUUID().toString();
  return command(context,PLAY).putExtra("queue",pendingToken).putExtra("uri",entry.uri).putExtra("name",entry.name).putExtra("mime",entry.mime);
 }
 public static MediaItem item(LocalMedia entry) {
  android.os.Bundle extras=new android.os.Bundle();extras.putString("mime",entry.mime);extras.putString("name",entry.name);
  MediaMetadata metadata=new MediaMetadata.Builder().setArtist(PlaybackText.SOURCE).setSubtitle(PlaybackText.SOURCE).setTitle(entry.displayName()).setMediaType(entry.audio()?MediaMetadata.MEDIA_TYPE_MUSIC:MediaMetadata.MEDIA_TYPE_VIDEO).setExtras(extras).build();
  return new MediaItem.Builder().setMediaId(entry.uri).setUri(entry.uri).setMediaMetadata(metadata).build();
 }
 private void changed() {sendBroadcast(new Intent(CHANGED).setPackage(getPackageName()));}
 @Override public void onCreate() {
  super.onCreate();active=this;
  setMediaNotificationProvider(new DismissibleLocalNotifications(this));
  setListener(new MediaSessionService.Listener() {@Override public void onForegroundServiceStartNotAllowedException() {
   if(player!=null) player.pause();android.util.Log.e("StreamLocalPlayback","Android rejected foreground playback");
   android.widget.Toast.makeText(LocalAudioService.this,"Åbn Stream for at genoptage afspilningen.",android.widget.Toast.LENGTH_LONG).show();
  }});
  player=new ExoPlayer.Builder(this,new DefaultRenderersFactory(this).setEnableDecoderFallback(true)).setMediaSourceFactory(new androidx.media3.exoplayer.source.DefaultMediaSourceFactory(()->new PcloudDataSource(this))).build();
  player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),true);player.setHandleAudioBecomingNoisy(true);player.setWakeMode(C.WAKE_MODE_LOCAL);
  session=new MediaSession.Builder(this,player).setId("stream-local-playback").build();addSession(session);
  player.addListener(new Player.Listener() {
   @Override public void onPlayWhenReadyChanged(boolean ready,int reason){if(!ready)refreshPausedNotification();}
   @Override public void onMediaItemTransition(MediaItem item,int reason) {
    if(item==null) {current=null;changed();return;}
    android.os.Bundle extras=item.mediaMetadata.extras;
    current=new LocalMedia(item.mediaId,extras==null?String.valueOf(item.mediaMetadata.title):extras.getString("name",String.valueOf(item.mediaMetadata.title)),extras==null?"":extras.getString("mime",""),false);
    Intent screen=(current.audio()?new Intent(LocalAudioService.this,AudioPlayerActivity.class).putExtra("local",true):new Intent(LocalAudioService.this,MainActivity.class).putExtra("openFullPlayer",true)).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
    session.setSessionActivity(PendingIntent.getActivity(LocalAudioService.this,30,screen,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));changed();
   }
   @Override public void onEvents(Player source,Player.Events events) {changed();}
   @Override public void onPlayerError(PlaybackException error) {
    android.util.Log.e("StreamLocalPlayback","Local playback failed: "+error.getErrorCodeName(),error);
    android.widget.Toast.makeText(LocalAudioService.this,"Filen kan ikke afspilles. Kontrollér filformat og adgang til filen.",android.widget.Toast.LENGTH_LONG).show();
    player.pause();androidx.core.app.ServiceCompat.stopForeground(LocalAudioService.this,androidx.core.app.ServiceCompat.STOP_FOREGROUND_REMOVE);changed();
   }
  });
 }
 private void refreshPausedNotification(){PausedNotificationRefresh.update(this,session,2003,()->{PendingIntent toggle=PendingIntent.getService(this,31,command(this,TOGGLE),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);PendingIntent stop=PendingIntent.getService(this,32,command(this,STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);return PlaybackNotification.create(this,"stream_local",session,toggle,stop);});}
 @Override public void onUpdateNotification(MediaSession mediaSession,boolean foregroundRequired){super.onUpdateNotification(mediaSession,foregroundRequired&&mediaSession.getPlayer().getPlayWhenReady());}
 @Override public MediaSession onGetSession(MediaSession.ControllerInfo controller) {return session;}
 @Override public int onStartCommand(Intent intent,int flags,int startId) {
  super.onStartCommand(intent,flags,startId);if(intent==null) return START_NOT_STICKY;
  if(STOP.equals(intent.getAction())) {current=null;queue=Collections.emptyList();pendingQueue=null;pendingToken=null;player.pause();player.stop();player.clearMediaItems();androidx.core.app.ServiceCompat.stopForeground(LocalAudioService.this,androidx.core.app.ServiceCompat.STOP_FOREGROUND_REMOVE);changed();stopSelf();}
  else if(TOGGLE.equals(intent.getAction())) {if(current!=null) {if(player.getPlaybackState()==Player.STATE_ENDED) {player.seekTo(0);player.play();}else if(player.getPlayWhenReady()) player.pause();else {if(player.getPlaybackState()==Player.STATE_IDLE) player.prepare();player.play();}}}
  else if(PLAY.equals(intent.getAction())) {
   try {
    LocalMedia entry=new LocalMedia(intent.getStringExtra("uri"),intent.getStringExtra("name"),intent.getStringExtra("mime"),false);
    if(entry.image())throw new IllegalArgumentException("Use image viewer");
    LocalQueue queue=pendingQueue!=null&&Objects.equals(pendingToken,intent.getStringExtra("queue"))?pendingQueue:new LocalQueue(Collections.singletonList(entry),entry);
    this.queue=queue.entries;pendingQueue=null;pendingToken=null;stopService(new Intent(this,RadioPlaybackService.class));
    List<MediaItem> items=new ArrayList<>();boolean network=false;for(LocalMedia file:queue.entries){MediaItem media=item(file);byte[] cover=LocalFileArtwork.bytes(this,file);if(cover!=null)media=media.buildUpon().setMediaMetadata(media.mediaMetadata.buildUpon().setArtworkData(cover,MediaMetadata.PICTURE_TYPE_FRONT_COVER).build()).build();items.add(media);network|=PcloudDocument.is(file.uri);}player.setWakeMode(network?C.WAKE_MODE_NETWORK:C.WAKE_MODE_LOCAL);
    player.setRepeatMode(Player.REPEAT_MODE_OFF);player.setShuffleModeEnabled(false);player.setMediaItems(items,queue.start,Math.max(0,intent.getLongExtra("position",0)));
    // Establish foreground immediately, before asynchronous file/decoder preparation.
    PendingIntent toggle=PendingIntent.getService(this,31,command(this,TOGGLE),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    PendingIntent stop=PendingIntent.getService(this,32,command(this,STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    startForeground(2003,PlaybackNotification.create(this,"stream_local",session,toggle,stop));
    player.prepare();player.play();
   } catch(Exception failure) {
    android.util.Log.e("StreamLocalPlayback","Could not start local playback",failure);
    current=null;player.stop();androidx.core.app.ServiceCompat.stopForeground(LocalAudioService.this,androidx.core.app.ServiceCompat.STOP_FOREGROUND_REMOVE);changed();stopSelf();
    android.widget.Toast.makeText(this,"Kan ikke åbne filen. Vælg filen igen, hvis adgangen er fjernet.",android.widget.Toast.LENGTH_LONG).show();
   }
  }
  return START_NOT_STICKY;
 }
 @Override public void onTaskRemoved(Intent root) {PlaybackShutdown.stop(this);androidx.core.app.ServiceCompat.stopForeground(this,androidx.core.app.ServiceCompat.STOP_FOREGROUND_REMOVE);stopSelf();}
 @Override public void onDestroy() {androidx.core.app.ServiceCompat.stopForeground(this,androidx.core.app.ServiceCompat.STOP_FOREGROUND_REMOVE);if(active==this) active=null;current=null;if(session!=null) session.release();if(player!=null) player.release();changed();super.onDestroy();}
}
