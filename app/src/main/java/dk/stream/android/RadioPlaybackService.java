package dk.stream.android;

import android.app.*;
import android.content.*;
import android.os.Build;

import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.*;
import org.json.JSONObject;

/** Radio owns a foreground media session, so launcher widgets also work after the UI closes. */
@androidx.media3.common.util.UnstableApi
public final class RadioPlaybackService extends MediaSessionService {
 public static final String PLAY="dk.stream.android.RADIO_PLAY",TOGGLE="dk.stream.android.RADIO_TOGGLE",STOP="dk.stream.android.RADIO_STOP",WIDGET_TOGGLE="dk.stream.android.RADIO_WIDGET_TOGGLE",TRANSPORT_TOGGLE="dk.stream.android.RADIO_TRANSPORT_TOGGLE";
 private static RadioPlaybackService active;
 private ExoPlayer player;
 private MediaSession session;
 private RadioStation current;
 private String error="";
 private final java.util.concurrent.ExecutorService artworkWorker=java.util.concurrent.Executors.newSingleThreadExecutor();
 private int artworkGeneration;
 public static boolean playing(String id) {return active!=null && active.current!=null && active.current.id.equals(id) && active.player!=null && active.player.getPlayWhenReady();}
 public static String audibleStationId() {return active!=null && active.current!=null && active.player!=null && active.player.isPlaying()?active.current.id:"";}
 public static String nowPlaying() {return active==null||active.current==null?"":active.current.displayName();}
 public static String error() {return active==null?"":active.error;}
 public static Intent playIntent(Context context,RadioStation station) {
  Intent intent=new Intent(context,RadioPlaybackService.class).setAction(PLAY);
  try {intent.putExtra("station",station.toJson().toString());} catch(Exception ignored) { }
  return intent;
 }
 @Override public void onCreate() {
  super.onCreate();active=this;
  player=new ExoPlayer.Builder(this).build();player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),true);player.setHandleAudioBecomingNoisy(true);player.setWakeMode(C.WAKE_MODE_NETWORK);
  setMediaNotificationProvider(new MediaNotification.Provider() {
   public MediaNotification createNotification(MediaSession mediaSession,com.google.common.collect.ImmutableList<CommandButton> buttons,MediaNotification.ActionFactory factory,MediaNotification.Provider.Callback callback) {return new MediaNotification(2001,notification(mediaSession));}
   public boolean handleCustomCommand(MediaSession mediaSession,String action,android.os.Bundle extras) {return false;}
  });
  session=new MediaSession.Builder(this,player).setId("stream-radio").setSessionActivity(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class).putExtra("openRadio",true).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)).build();
  player.addListener(new Player.Listener() {
   @Override public void onEvents(Player source,Player.Events events) {RadioWidgetProvider.refresh(RadioPlaybackService.this);}
   @Override public void onPlayerError(PlaybackException failure) {error=getString(R.string.radio_error);player.pause();RadioWidgetProvider.refresh(RadioPlaybackService.this);}
  });
 }
 @Override public MediaSession onGetSession(MediaSession.ControllerInfo controller) {return session;}
 private Notification notification(MediaSession mediaSession) {
  PendingIntent toggle=PendingIntent.getService(this,2,new Intent(this,RadioPlaybackService.class).setAction(TRANSPORT_TOGGLE),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,RadioPlaybackService.class).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  return PlaybackNotification.create(this,"stream_radio",mediaSession,toggle,stop);
 }
 private void foreground() {startForeground(2001,notification(session));}
 private void loadArtwork(RadioStation station) {
  int generation=++artworkGeneration;
  if(PlaybackArtwork.bundled(this,station.displayName()) || station.logo.isEmpty()) return;
  artworkWorker.execute(()->{
   byte[] data=PlaybackArtwork.remote(this,station.logo);if(data==null) return;
   new android.os.Handler(getMainLooper()).post(()->{
    if(active!=this || generation!=artworkGeneration || current==null || !current.id.equals(station.id)) return;
    MediaItem item=player.getCurrentMediaItem();if(item!=null) player.replaceMediaItem(0,item.buildUpon().setMediaMetadata(item.mediaMetadata.buildUpon().setArtworkData(data,MediaMetadata.PICTURE_TYPE_FRONT_COVER).build()).build());
   });
  });
 }
 @Override public int onStartCommand(Intent intent,int flags,int startId) {
  super.onStartCommand(intent,flags,startId);
  if(intent==null) return START_NOT_STICKY;
  String action=intent.getAction();
  if(STOP.equals(action)) {artworkGeneration++;player.stop();current=null;error="";RadioWidgetProvider.refresh(this);androidx.core.app.ServiceCompat.stopForeground(this,androidx.core.app.ServiceCompat.STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;}
  if(TRANSPORT_TOGGLE.equals(action)) {if(current==null) {stopSelf();return START_NOT_STICKY;}if(player.getPlayWhenReady()) player.pause();else {if(player.getPlaybackState()==Player.STATE_IDLE) player.prepare();player.play();}RadioWidgetProvider.refresh(this);return START_NOT_STICKY;}
  if(!PLAY.equals(action) && !TOGGLE.equals(action) && !WIDGET_TOGGLE.equals(action)) return START_NOT_STICKY;
  // A foreground-service PendingIntent must promptly show a notification even if the favorite was removed.
  foreground();
  try {
   RadioStation station;
   String favoriteId=intent.getStringExtra("favoriteId");
   if(favoriteId!=null) station=new LibraryStore(this).load().findRadio(favoriteId);
   else station=RadioStation.fromJson(new JSONObject(intent.getStringExtra("station")));
   if(station==null || okhttp3.HttpUrl.parse(station.url)==null) throw new IllegalArgumentException("Station missing");
   error="";
   if(WIDGET_TOGGLE.equals(action)) RadioWidgetProvider.showFeedback(this,intent.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID),!playing(station.id));
   if(WIDGET_TOGGLE.equals(action) && current!=null && current.id.equals(station.id) && player.getPlayWhenReady()) {player.stop();current=null;androidx.core.app.ServiceCompat.stopForeground(this,androidx.core.app.ServiceCompat.STOP_FOREGROUND_REMOVE);stopSelf();}
   else if(TOGGLE.equals(action) && current!=null && current.id.equals(station.id) && player.getPlayWhenReady()) player.pause();
   else {
    current=station;
    MediaMetadata metadata=new MediaMetadata.Builder().setTitle(station.displayName()).setArtist(getString(R.string.app_name)).setArtworkData(PlaybackArtwork.local(this,station.displayName()),MediaMetadata.PICTURE_TYPE_FRONT_COVER).build();
    player.setMediaItem(new MediaItem.Builder().setUri(station.url).setMimeType(station.hls?MimeTypes.APPLICATION_M3U8:null).setMediaId(station.id).setMediaMetadata(metadata).build());player.prepare();player.play();loadArtwork(station);
   }
   RadioWidgetProvider.refresh(this);
  } catch(Exception failure) {player.stop();current=null;androidx.core.app.ServiceCompat.stopForeground(this,androidx.core.app.ServiceCompat.STOP_FOREGROUND_REMOVE);stopSelf();RadioWidgetProvider.refresh(this);}
  return START_NOT_STICKY;
 }
 @Override public void onTaskRemoved(Intent rootIntent) {if(!player.getPlayWhenReady()) stopSelf();}
 @Override public void onDestroy() {artworkGeneration++;artworkWorker.shutdownNow();if(active==this) active=null;if(session!=null) session.release();if(player!=null) player.release();RadioWidgetProvider.refresh(this);super.onDestroy();}
}
