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
public final class RadioPlaybackService extends MediaLibraryService {
 public static final String PREVIOUS="dk.stream.android.RADIO_PREVIOUS",NEXT="dk.stream.android.RADIO_NEXT";
 public static final String QUEUE_WIDGET_TOGGLE="dk.stream.android.RADIO_QUEUE_TOGGLE",QUEUE_WIDGET_PREVIOUS="dk.stream.android.RADIO_QUEUE_PREVIOUS",QUEUE_WIDGET_NEXT="dk.stream.android.RADIO_QUEUE_NEXT";
 public static final String WIDE_WIDGET_TOGGLE="dk.stream.android.RADIO_WIDE_WIDGET_TOGGLE";
 public static final String LIVE="dk.stream.android.RADIO_LIVE",PLAY="dk.stream.android.RADIO_PLAY",TOGGLE="dk.stream.android.RADIO_TOGGLE",STOP="dk.stream.android.RADIO_STOP",WIDGET_TOGGLE="dk.stream.android.RADIO_WIDGET_TOGGLE",TRANSPORT_TOGGLE="dk.stream.android.RADIO_TRANSPORT_TOGGLE";
 private static RadioPlaybackService active;
 static void stopForSourceSwitch(Context context){RadioPlaybackService service=active;if(service!=null)service.stopRadioPlayback();else context.stopService(new Intent(context,RadioPlaybackService.class));}
 static void refreshCarStartPage(){RadioPlaybackService service=active;if(service!=null&&service.carCallback!=null)service.carCallback.startPageChanged(service.session);}
 static LocalMedia selectedFile(){return active==null||active.player==null?null:CarAudioCatalog.file(active.player.getCurrentMediaItem());}
 static boolean playingFile(String uri){LocalMedia file=selectedFile();return file!=null&&file.uri.equals(uri)&&active.player.getPlayWhenReady()&&active.player.getPlaybackState()!=Player.STATE_ENDED;}
 public static RadioStation selectedStation() {return active!=null && active.player!=null && (active.player.getPlaybackState()!=Player.STATE_IDLE||active.recovery.wanted())?active.current:null;}
 private ExoPlayer player;
 private RadioSessionPlayer sessionPlayer;
 private MediaLibrarySession session;
 private CarRadioCatalog carCatalog;
 private CarRadioCallback carCallback;
 private RadioStation current;
 private String error="";
 private boolean behindLive;
 private final RadioRecoveryState recovery=new RadioRecoveryState();
 private final android.os.Handler recoveryHandler=new android.os.Handler(android.os.Looper.getMainLooper());
 private final Runnable recoveryTick=new Runnable(){public void run(){if(active!=RadioPlaybackService.this||player==null)return;if(current!=null&&LocalAudioService.current()==null&&!CastPlayback.hasMedia(RadioPlaybackService.this)&&recovery.retry(android.os.SystemClock.elapsedRealtime(),player.getCurrentPosition(),player.getPlaybackSuppressionReason()!=Player.PLAYBACK_SUPPRESSION_REASON_NONE)){try{restartPlayer(null,true);}catch(RuntimeException failed){handleRadioFailure();}}recoveryHandler.postDelayed(this,2000);}};
 public static boolean recovering(){return active!=null&&active.recovery.wanted()&&active.player!=null&&(active.player.getPlaybackState()==Player.STATE_IDLE||active.player.getPlaybackState()==Player.STATE_ENDED);}

 public static Player audioPlayer() {return active==null?null:active.sessionPlayer;}
 public static boolean needsLive() {return selectedStation()!=null && active.behindLive;}
 private final java.util.concurrent.ExecutorService artworkWorker=java.util.concurrent.Executors.newSingleThreadExecutor();
 private int artworkGeneration;
 public static boolean playing(String id) {return active!=null && active.current!=null && active.current.id.equals(id) && active.player!=null && active.player.getPlayWhenReady();}
 public static String audibleStationId() {return active!=null && active.current!=null && active.player!=null && active.player.isPlaying()?active.current.id:"";}
 public static String nowPlaying() {LocalMedia file=selectedFile();return file!=null?file.displayName():active==null||active.current==null?"":active.current.displayName();}
 public static String error() {return active==null?"":active.error;}
 @android.annotation.SuppressLint("ApplySharedPref") // Widget PendingIntents must retain a valid token after process death.
 private static synchronized String commandToken(Context context) {
  android.content.SharedPreferences prefs=context.getSharedPreferences("radio_commands",MODE_PRIVATE);String value=prefs.getString("token",null);
  if(value==null) {value=java.util.UUID.randomUUID().toString();prefs.edit().putString("token",value).commit();}return value;
 }
 static Intent internalIntent(Context context,String action) {return new Intent(context,RadioPlaybackService.class).setAction(action).putExtra("command_token",commandToken(context));}
 public static Intent playIntent(Context context,RadioStation station) {
  Intent intent=internalIntent(context,PLAY);
  try {intent.putExtra("station",station.toJson().toString());} catch(Exception ignored) { }
  return intent;
 }
 @Override public void onCreate() {
  super.onCreate();active=this;
  createPlayer();
  setMediaNotificationProvider(new MediaNotification.Provider() {
   public MediaNotification createNotification(MediaSession mediaSession,com.google.common.collect.ImmutableList<CommandButton> buttons,MediaNotification.ActionFactory factory,MediaNotification.Provider.Callback callback) {return new MediaNotification(2001,notification(mediaSession));}
   public boolean handleCustomCommand(MediaSession mediaSession,String action,android.os.Bundle extras) {return false;}
  });
  carCatalog=new CarRadioCatalog(this);carCallback=new CarRadioCallback(carCatalog,()->onStartCommand(internalIntent(this,LIVE),0,0),()->onStartCommand(internalIntent(this,PREVIOUS),0,0),()->onStartCommand(internalIntent(this,NEXT),0,0));
  session=new MediaLibrarySession.Builder(this,sessionPlayer,carCallback).setId("stream-radio").setCustomLayout(CarRadioCallback.transportButtons()).setMediaButtonPreferences(CarRadioCallback.transportButtons()).setSessionActivity(PendingIntent.getActivity(this,0,new Intent(this,AudioPlayerActivity.class).putExtra("local",false).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)).build();
  recoveryHandler.postDelayed(recoveryTick,2000);
 }
 private void createPlayer(){
  androidx.media3.datasource.DefaultHttpDataSource.Factory http=new androidx.media3.datasource.DefaultHttpDataSource.Factory().setUserAgent("Stream-Android").setConnectTimeoutMs(15000).setReadTimeoutMs(15000).setAllowCrossProtocolRedirects(true);
  player=new ExoPlayer.Builder(this,new androidx.media3.exoplayer.DefaultRenderersFactory(this).setEnableDecoderFallback(true)).setMediaSourceFactory(new androidx.media3.exoplayer.source.DefaultMediaSourceFactory((()->new PcloudDataSource(this,new androidx.media3.datasource.DefaultDataSource.Factory(this,http))))).build();
  PlaybackTracker.attach(this,player);BluetoothOutputButton.register(this,player,()->sessionPlayer.play());player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),true);player.setHandleAudioBecomingNoisy(true);player.setWakeMode(C.WAKE_MODE_NETWORK);
  sessionPlayer=new RadioSessionPlayer(player,this::foreground,()->!ExternalOutputState.pending()&&(CarAudioCatalog.file(player.getCurrentMediaItem())!=null||!BluetoothPlayback.resumeOther(player)),this::resumeRadio);player.addListener(radioEvents);
 }
 private void resumeRadio(){if(player.getCurrentMediaItem()==null)return;if(CarAudioCatalog.file(player.getCurrentMediaItem())!=null){foreground();if(player.getPlaybackState()==Player.STATE_ENDED)player.seekTo(0);if(player.getPlaybackState()==Player.STATE_IDLE)player.prepare();player.play();return;}recovery.request(android.os.SystemClock.elapsedRealtime());restartPlayer(null,true);}
 private void restartPlayer(MediaItem replacement,boolean live){
  MediaItem item=replacement!=null?replacement:player.getCurrentMediaItem();if(item==null)return;long position=!live&&replacement==null&&player.isCurrentMediaItemSeekable()?player.getCurrentPosition():C.TIME_UNSET;boolean wasBehind=behindLive;
  ExoPlayer previous=player;previous.removeListener(radioEvents);PlaybackTracker.detach(previous);BluetoothOutputButton.unregister(previous);previous.pause();previous.stop();
  createPlayer();session.setPlayer(sessionPlayer);previous.release();player.setMediaItem(item,position);foreground();player.prepare();player.play();behindLive=!live&&position!=C.TIME_UNSET&&wasBehind;recovery.resetProgress(android.os.SystemClock.elapsedRealtime());RadioWidgetProvider.refresh(this);
 }
 private void handleRadioFailure(){error=getString(R.string.radio_error);android.util.Log.w("StreamRadio","Radio playback interrupted; rebuilding on retry");recovery.failure(android.os.SystemClock.elapsedRealtime());if(recovery.exhausted())player.pause();RadioWidgetProvider.refresh(this);}
 private final Player.Listener radioEvents=new Player.Listener() {
   @Override public void onPlayWhenReadyChanged(boolean ready,int reason) {if(!ready&&(reason==Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST||reason==Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS||reason==Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY||reason==Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE)) recovery.pause();if(ready) error="";else if(current!=null && player.getPlaybackState()!=Player.STATE_IDLE) behindLive=true;if(!ready)refreshPausedNotification();}
   @Override public void onPlaybackStateChanged(int state) {if(state==Player.STATE_IDLE) behindLive=false;if(state==Player.STATE_ENDED&&recovery.wanted())recovery.failure(android.os.SystemClock.elapsedRealtime());}
   @Override public void onMediaItemTransition(MediaItem item,int reason) {
    if(CarAudioCatalog.file(item)!=null){LastAudio.file(RadioPlaybackService.this,CarAudioCatalog.file(item));current=null;recovery.pause();behindLive=false;error="";artworkGeneration++;loadFileArtwork(item);LocalAudioService.stopForSourceSwitch(RadioPlaybackService.this);RadioWidgetProvider.refresh(RadioPlaybackService.this);}
    if(item!=null && item.mediaId.startsWith(CarRadioCatalog.PREFIX)) {RadioStation station=carCatalog.known(item.mediaId);if(station!=null) {LastAudio.radio(RadioPlaybackService.this,station);LocalAudioService.stopForSourceSwitch(RadioPlaybackService.this);if(current==null || !current.id.equals(station.id)) behindLive=false;current=station;error="";loadArtwork(station);}}
   }
   @Override public void onEvents(Player source,Player.Events events) {RadioWidgetProvider.refresh(RadioPlaybackService.this);}
   @Override public void onPlayerError(PlaybackException failure) {if(selectedFile()!=null){error="Lydfilen kan ikke afspilles. Kontrollér format og adgang til filen.";player.pause();android.widget.Toast.makeText(RadioPlaybackService.this,error,android.widget.Toast.LENGTH_LONG).show();}else handleRadioFailure();}
 };
 private void refreshPausedNotification(){PausedNotificationRefresh.update(this,session,2001,()->notification(session));}
 @Override public void onUpdateNotification(MediaSession mediaSession,boolean foregroundRequired){super.onUpdateNotification(mediaSession,foregroundRequired&&mediaSession.getPlayer().getPlayWhenReady());}
 @Override public MediaLibrarySession onGetSession(MediaSession.ControllerInfo controller) {return session;}
 private Notification notification(MediaSession mediaSession) {
  PendingIntent toggle=PendingIntent.getService(this,2,internalIntent(this,LIVE),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  PendingIntent stop=PendingIntent.getService(this,1,internalIntent(this,STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  PendingIntent live=PendingIntent.getService(this,3,internalIntent(this,LIVE),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  if(CarAudioCatalog.file(mediaSession.getPlayer().getCurrentMediaItem())!=null)return PlaybackNotification.create(this,"stream_car_audio",mediaSession,toggle,stop);
  return PlaybackNotification.radio(this,mediaSession,toggle,stop,live,mediaSession.getPlayer().isPlaying()&&!needsLive());
 }
 private void foreground() {startForeground(2001,notification(session));}
 private void loadFileArtwork(MediaItem selected){LocalMedia file=CarAudioCatalog.file(selected);if(file==null||selected.mediaMetadata.artworkData!=null)return;int generation=artworkGeneration;artworkWorker.execute(()->{byte[] data=LocalFileArtwork.bytes(this,file);if(data==null)return;new android.os.Handler(getMainLooper()).post(()->{if(active!=this||generation!=artworkGeneration||player==null)return;MediaItem item=player.getCurrentMediaItem();if(item!=null&&item.mediaId.equals(selected.mediaId))player.replaceMediaItem(player.getCurrentMediaItemIndex(),item.buildUpon().setMediaMetadata(item.mediaMetadata.buildUpon().setArtworkData(data,MediaMetadata.PICTURE_TYPE_FRONT_COVER).build()).build());});});}
 private void loadArtwork(RadioStation station) {
  int generation=++artworkGeneration;
  if(PlaybackArtwork.bundled(this,PlaybackArtwork.logoName(this,station)) || station.logo.isEmpty()) return;
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
  if(java.util.Arrays.asList(PLAY,TOGGLE,STOP,WIDGET_TOGGLE,TRANSPORT_TOGGLE,WIDE_WIDGET_TOGGLE,LIVE,PREVIOUS,NEXT,QUEUE_WIDGET_TOGGLE,QUEUE_WIDGET_PREVIOUS,QUEUE_WIDGET_NEXT).contains(action) && !commandToken(this).equals(intent.getStringExtra("command_token"))) return START_NOT_STICKY;
  if(QUEUE_WIDGET_TOGGLE.equals(action)||QUEUE_WIDGET_PREVIOUS.equals(action)||QUEUE_WIDGET_NEXT.equals(action)){
   int widget=intent.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID);foreground();try{java.util.List<RadioStation> stations=RadioQueueWidgetProvider.owns(this,widget)?RadioQueueWidgetPreferences.stations(this,new LibraryStore(this).load(),widget):java.util.Collections.emptyList();boolean toggle=QUEUE_WIDGET_TOGGLE.equals(action);RadioStation target=RadioQueueWidgetPreferences.current(this,stations,widget);if(target==null||!toggle&&stations.size()<2){if(!player.getPlayWhenReady()){androidx.core.app.ServiceCompat.stopForeground(this,androidx.core.app.ServiceCompat.STOP_FOREGROUND_REMOVE);stopSelf(startId);}RadioQueueWidgetProvider.safeUpdate(this,android.appwidget.AppWidgetManager.getInstance(this),widget);return START_NOT_STICKY;}if(!toggle)target=RadioWidgetQueue.adjacent(stations,target.id,QUEUE_WIDGET_PREVIOUS.equals(action)?-1:1);RadioWidgetProvider.bind(this,widget,target.id);if(toggle&&playing(target.id)){stopRadioPlayback();return START_NOT_STICKY;}intent=playIntent(this,target);action=PLAY;}catch(Exception failure){if(!player.getPlayWhenReady()){androidx.core.app.ServiceCompat.stopForeground(this,androidx.core.app.ServiceCompat.STOP_FOREGROUND_REMOVE);stopSelf(startId);}return START_NOT_STICKY;}
  }
  if(PREVIOUS.equals(action)||NEXT.equals(action)){if(selectedFile()!=null){if(PREVIOUS.equals(action)&&player.hasPreviousMediaItem())player.seekToPreviousMediaItem();if(NEXT.equals(action)&&player.hasNextMediaItem())player.seekToNextMediaItem();return START_NOT_STICKY;}try{RadioStation station=RadioFavoriteNavigation.adjacent(new LibraryStore(this).load(),current==null?"":current.id,PREVIOUS.equals(action)?-1:1);if(station==null)return START_NOT_STICKY;intent=playIntent(this,station);action=PLAY;}catch(Exception unavailable){return START_NOT_STICKY;}}
  if(android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH.equals(action)) {
   String query=intent.getStringExtra(android.app.SearchManager.QUERY);if(query==null||query.trim().isEmpty()) return START_NOT_STICKY;
   foreground();MediaItem request=new MediaItem.Builder().setRequestMetadata(new MediaItem.RequestMetadata.Builder().setSearchQuery(query).build()).build();
   com.google.common.util.concurrent.Futures.addCallback(carCallback.onAddMediaItems(session,null,java.util.Collections.singletonList(request)),new com.google.common.util.concurrent.FutureCallback<java.util.List<MediaItem>>() {
    public void onSuccess(java.util.List<MediaItem> items) {if(active!=RadioPlaybackService.this || items==null||items.isEmpty()) return;LocalMedia file=CarAudioCatalog.file(items.get(0));player.setMediaItem(items.get(0),file==null?0:PlaybackHistory.get(RadioPlaybackService.this,file.uri).resume());sessionPlayer.play();}
    public void onFailure(Throwable error) {if(active==RadioPlaybackService.this) {sessionPlayer.stop();androidx.core.app.ServiceCompat.stopForeground(RadioPlaybackService.this,androidx.core.app.ServiceCompat.STOP_FOREGROUND_REMOVE);stopSelf();}}
   },androidx.core.content.ContextCompat.getMainExecutor(this));return START_NOT_STICKY;
  }
  if(STOP.equals(action)) {stopRadioPlayback();return START_NOT_STICKY;}
  if(LIVE.equals(action)) {
   MediaItem item=player.getCurrentMediaItem();
   if(current==null || item==null) {if(current==null) stopSelf();return START_NOT_STICKY;}
   foreground();error="";
   // Reload the item instead of resuming its paused buffer, including non-seekable MP3/AAC radio.
   recovery.request(android.os.SystemClock.elapsedRealtime());restartPlayer(item,true);behindLive=false;
   RadioWidgetProvider.refresh(this);return START_NOT_STICKY;
  }
  if(TRANSPORT_TOGGLE.equals(action)) {if(current==null&&selectedFile()==null) {stopSelf();return START_NOT_STICKY;}if(player.getPlayWhenReady()) sessionPlayer.pause();else sessionPlayer.play();RadioWidgetProvider.refresh(this);return START_NOT_STICKY;}
  if(WIDE_WIDGET_TOGGLE.equals(action)&&current!=null&&current.id.equals(intent.getStringExtra("favoriteId"))){foreground();if(player.getPlayWhenReady())stopRadioPlayback();else sessionPlayer.play();RadioWidgetProvider.refresh(this);return START_NOT_STICKY;}
  if(!PLAY.equals(action) && !TOGGLE.equals(action) && !WIDGET_TOGGLE.equals(action)&&!WIDE_WIDGET_TOGGLE.equals(action)) return START_NOT_STICKY;
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
   if(WIDGET_TOGGLE.equals(action) && current!=null && current.id.equals(station.id) && player.getPlayWhenReady()) {stopRadioPlayback();return START_NOT_STICKY;}
   else if(TOGGLE.equals(action) && current!=null && current.id.equals(station.id) && player.getPlayWhenReady()) sessionPlayer.pause();
   else {
    LocalAudioService.stopForSourceSwitch(this);
    current=station;LastAudio.radio(this,station);
    MediaMetadata metadata=PlaybackText.metadata(station.displayName()).setExtras(CarRadioCatalog.liveExtras()).setDisplayTitle(station.displayName()).setArtworkUri(CarArtworkProvider.station(this,station.id)).setArtworkData(CarRadioCatalog.artworkData(this,station),MediaMetadata.PICTURE_TYPE_FRONT_COVER).build();
    recovery.request(android.os.SystemClock.elapsedRealtime());restartPlayer(new MediaItem.Builder().setUri(station.url).setMimeType(station.hls?MimeTypes.APPLICATION_M3U8:null).setMediaId(station.id).setMediaMetadata(metadata).build(),true);behindLive=false;loadArtwork(station);
   }
   RadioWidgetProvider.refresh(this);
  } catch(Exception failure) {sessionPlayer.stop();current=null;androidx.core.app.ServiceCompat.stopForeground(this,androidx.core.app.ServiceCompat.STOP_FOREGROUND_REMOVE);stopSelf();RadioWidgetProvider.refresh(this);}
  return START_NOT_STICKY;
 }
 private void stopRadioPlayback(){
  recovery.pause();artworkGeneration++;current=null;error="";behindLive=false;sessionPlayer.stop();player.clearMediaItems();
  androidx.core.app.ServiceCompat.stopForeground(this,androidx.core.app.ServiceCompat.STOP_FOREGROUND_REMOVE);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(2001);RadioWidgetProvider.refresh(this);stopSelf();
 }
 @Override public void onTaskRemoved(Intent rootIntent) {artworkGeneration++;current=null;error="";behindLive=false;PlaybackShutdown.stop(this);androidx.core.app.ServiceCompat.stopForeground(this,androidx.core.app.ServiceCompat.STOP_FOREGROUND_REMOVE);stopSelf();}
 @Override public void onDestroy() {recovery.pause();recoveryHandler.removeCallbacks(recoveryTick);androidx.core.app.ServiceCompat.stopForeground(this,androidx.core.app.ServiceCompat.STOP_FOREGROUND_REMOVE);artworkGeneration++;artworkWorker.shutdownNow();if(carCallback!=null) carCallback.close();if(active==this) active=null;if(session!=null) session.release();if(player!=null){PlaybackTracker.detach(player);BluetoothOutputButton.unregister(player);player.release();}RadioWidgetProvider.refresh(this);super.onDestroy();}
}
