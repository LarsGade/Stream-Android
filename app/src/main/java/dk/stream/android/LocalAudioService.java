package dk.stream.android;

import android.app.PendingIntent;
import android.content.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.*;

/** Plays the original document URI in the background with Android media controls. */
@androidx.media3.common.util.UnstableApi
public final class LocalAudioService extends MediaSessionService {
 public static final String PLAY="local.PLAY",TOGGLE="local.TOGGLE",STOP="local.STOP";
 private static LocalAudioService active;
 private ExoPlayer player;
 private MediaSession session;
 private LocalMedia current;
 public static Player player() {return active==null?null:active.player;}
 public static LocalMedia current() {return active!=null && active.player.getPlaybackState()!=Player.STATE_IDLE?active.current:null;}
 public static Intent command(Context context,String action) {return new Intent(context,LocalAudioService.class).setAction(action);}
 public static Intent playIntent(Context context,LocalMedia entry) {return command(context,PLAY).putExtra("uri",entry.uri).putExtra("name",entry.name).putExtra("mime",entry.mime);}
 @Override public void onCreate() {
  super.onCreate();active=this;player=new ExoPlayer.Builder(this).build();player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),true);player.setHandleAudioBecomingNoisy(true);player.setWakeMode(C.WAKE_MODE_LOCAL);
  Intent screen=new Intent(this,AudioPlayerActivity.class).putExtra("local",true);
  session=new MediaSession.Builder(this,player).setId("stream-local-audio").setSessionActivity(PendingIntent.getActivity(this,30,screen,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)).build();
  player.addListener(new Player.Listener() {@Override public void onPlayerError(PlaybackException error) {android.widget.Toast.makeText(LocalAudioService.this,"Filen kan ikke afspilles. Kontrollér filen og adgangen til den.",android.widget.Toast.LENGTH_LONG).show();stopSelf();}});
 }
 @Override public MediaSession onGetSession(MediaSession.ControllerInfo controller) {return session;}
 @Override public int onStartCommand(Intent intent,int flags,int startId) {
  super.onStartCommand(intent,flags,startId);if(intent==null) return START_NOT_STICKY;
  if(STOP.equals(intent.getAction())) {player.stop();current=null;stopSelf();}
  else if(TOGGLE.equals(intent.getAction())) {if(current!=null) {if(player.getPlaybackState()==Player.STATE_ENDED) {player.seekTo(0);player.play();}else if(player.getPlayWhenReady()) player.pause();else player.play();}}
  else if(PLAY.equals(intent.getAction())) {
   try {
    LocalMedia entry=new LocalMedia(intent.getStringExtra("uri"),intent.getStringExtra("name"),intent.getStringExtra("mime"),false);
    if(!entry.audio()) throw new IllegalArgumentException("Audio required");
    // ExoPlayer opens the original document on its loader thread using the saved read grant.
    stopService(new Intent(this,RadioPlaybackService.class));current=entry;
    MediaMetadata metadata=new MediaMetadata.Builder().setTitle(entry.name).setArtist("Afspilles via Stream").setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC).build();
    player.setMediaItem(new MediaItem.Builder().setMediaId(entry.uri).setUri(entry.uri).setMediaMetadata(metadata).build());player.prepare();player.play();
   } catch(Exception failure) {android.widget.Toast.makeText(this,"Kan ikke åbne lydfilen. Tilføj filen igen, hvis adgangen er fjernet.",android.widget.Toast.LENGTH_LONG).show();stopSelf();}
  }
  return START_NOT_STICKY;
 }
 @Override public void onTaskRemoved(Intent root) {if(!player.getPlayWhenReady()) stopSelf();}
 @Override public void onDestroy() {if(active==this) active=null;if(session!=null) session.release();if(player!=null) player.release();super.onDestroy();}
}
