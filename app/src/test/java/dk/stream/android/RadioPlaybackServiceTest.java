package dk.stream.android;

import android.content.Intent;
import androidx.media3.common.Player;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import okhttp3.mockwebserver.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class RadioPlaybackServiceTest {
 @Test public void serviceStartsForegroundMediaSessionAndAlwaysStopsInsteadOfPausing() throws Exception {
  try(MockWebServer server=new MockWebServer()) {
   server.enqueue(new MockResponse().setResponseCode(404));server.enqueue(new MockResponse().setResponseCode(404));
   ServiceController<RadioPlaybackService> controller=Robolectric.buildService(RadioPlaybackService.class).create();RadioPlaybackService service=controller.get();
   try {
    RadioStation station=new RadioStation("station","P1",server.url("/radio.mp3").toString(),"");
    service.onStartCommand(RadioPlaybackService.playIntent(service,station),0,1);Player player=service.onGetSession(null).getPlayer();
    assertTrue(player.getPlayWhenReady());assertEquals("P1",RadioPlaybackService.nowPlaying());assertTrue(RadioPlaybackService.playing(station.id));assertNotNull(Shadows.shadowOf(service).getLastForegroundNotification());
    Intent toggle=RadioPlaybackService.playIntent(service,station).setAction(RadioPlaybackService.TOGGLE);
    service.onStartCommand(toggle,0,2);assertFalse(player.getPlayWhenReady());assertEquals(Player.STATE_IDLE,player.getPlaybackState());assertFalse(RadioPlaybackService.playing(station.id));
    service.onStartCommand(toggle,0,3);assertTrue(player.getPlayWhenReady());
    service.onStartCommand(RadioPlaybackService.internalIntent(service,RadioPlaybackService.STOP),0,4);assertEquals("",RadioPlaybackService.nowPlaying());assertEquals(Player.STATE_IDLE,player.getPlaybackState());
   } finally {controller.destroy();}
   assertFalse(RadioPlaybackService.playing("station"));
  }
 }
 @Test public void widgetBrieflyShowsTheActionAndActuallyStopsInsteadOfPausing() throws Exception {
  try(MockWebServer server=new MockWebServer()) {
   server.enqueue(new MockResponse().setResponseCode(404));
   ServiceController<RadioPlaybackService> controller=Robolectric.buildService(RadioPlaybackService.class).create();RadioPlaybackService service=controller.get();
   try {
    int widgetId=Shadows.shadowOf(android.appwidget.AppWidgetManager.getInstance(service)).createWidget(RadioWidgetProvider.class,R.layout.radio_widget);
    RadioStation station=new RadioStation("station","DR P1",server.url("/radio.mp3").toString(),"");
    Intent toggle=RadioPlaybackService.playIntent(service,station).setAction(RadioPlaybackService.WIDGET_TOGGLE).putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId);service.onStartCommand(toggle,0,1);
    android.content.Context context=service.getApplicationContext();android.view.View widget=RadioWidgetProvider.views(service,widgetId,station).apply(context,new android.widget.FrameLayout(context));
    assertEquals(service.getString(R.string.radio_stop),widget.findViewById(R.id.widget_play).getContentDescription());
    assertEquals(1,((android.widget.ViewFlipper)widget.findViewById(R.id.widget_feedback)).getDisplayedChild());assertEquals(R.drawable.ic_play,Shadows.shadowOf(((android.widget.ImageView)widget.findViewById(R.id.widget_play)).getDrawable()).getCreatedFromResId());
    service.onStartCommand(toggle,0,2);assertFalse(RadioPlaybackService.playing(station.id));assertEquals("",RadioPlaybackService.nowPlaying());assertEquals(Player.STATE_IDLE,service.onGetSession(null).getPlayer().getPlaybackState());assertTrue(Shadows.shadowOf(service).isStoppedBySelf());
    widget=RadioWidgetProvider.views(service,widgetId,station).apply(context,new android.widget.FrameLayout(context));assertEquals(R.drawable.ic_stop,Shadows.shadowOf(((android.widget.ImageView)widget.findViewById(R.id.widget_play)).getDrawable()).getCreatedFromResId());
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(1100));widget=RadioWidgetProvider.views(service,widgetId,station).apply(context,new android.widget.FrameLayout(context));assertEquals(0,((android.widget.ViewFlipper)widget.findViewById(R.id.widget_feedback)).getDisplayedChild());
   } finally {controller.destroy();}
  }
 }
 @Test public void mediaNotificationPublishesArtworkAndSessionTokenAndTransportControls() throws Exception {
  try(MockWebServer server=new MockWebServer()) {
   server.enqueue(new MockResponse().setResponseCode(404));ServiceController<RadioPlaybackService> controller=Robolectric.buildService(RadioPlaybackService.class).create();RadioPlaybackService service=controller.get();
   try {
    RadioStation station=new RadioStation("station","DR P1",server.url("/radio.mp3").toString(),"");service.onStartCommand(RadioPlaybackService.playIntent(service,station),0,1);
    androidx.media3.session.MediaSession session=service.onGetSession(null);assertNotNull(session.getPlayer().getMediaMetadata().artworkData);assertEquals("DR P1",session.getPlayer().getMediaMetadata().title.toString());
    java.lang.reflect.Method method=RadioPlaybackService.class.getDeclaredMethod("notification",androidx.media3.session.MediaSession.class);method.setAccessible(true);android.app.Notification notification=(android.app.Notification)method.invoke(service,session);
    assertEquals(0xFF3686FF,notification.color);assertTrue(notification.extras.getBoolean("android.colorized"));assertEquals(android.app.Notification.VISIBILITY_PUBLIC,notification.visibility);assertNotNull(notification.extras.getParcelable(android.app.Notification.EXTRA_MEDIA_SESSION));assertEquals("android.app.Notification$MediaStyle",notification.extras.getString(android.app.Notification.EXTRA_TEMPLATE));assertEquals(1,notification.actions.length);assertEquals(service.getString(R.string.stop_action),notification.actions[0].title);assertNotNull(notification.getLargeIcon());
    notification.actions[0].actionIntent.send();Intent action=Shadows.shadowOf(service.getApplication()).getNextStartedService();assertEquals(RadioPlaybackService.TRANSPORT_TOGGLE,action.getAction());service.onStartCommand(action,0,2);assertFalse(session.getPlayer().getPlayWhenReady());service.onStartCommand(action,0,3);assertTrue(session.getPlayer().getPlayWhenReady());
    action=RadioPlaybackService.internalIntent(service,RadioPlaybackService.STOP);service.onStartCommand(action,0,4);assertEquals("",RadioPlaybackService.nowPlaying());
   } finally {controller.destroy();}
  }
 }
 @Test public void controllerPauseIsConvertedToStopAndResumeIsFreshLivePlayback() throws Exception {
  try(MockWebServer server=new MockWebServer()) {
   server.enqueue(new MockResponse().setResponseCode(404));server.enqueue(new MockResponse().setResponseCode(404));
   ServiceController<RadioPlaybackService> controller=Robolectric.buildService(RadioPlaybackService.class).create();RadioPlaybackService service=controller.get();
   try {
    RadioStation station=new RadioStation("station","DR P1",server.url("/live.mp3").toString(),"");service.onStartCommand(RadioPlaybackService.playIntent(service,station),0,1);
    Player player=service.onGetSession(null).getPlayer();assertTrue(player instanceof LiveRadioPlayer);player.pause();assertEquals(Player.STATE_IDLE,player.getPlaybackState());assertFalse(player.getPlayWhenReady());
    player.play();assertTrue(player.getPlayWhenReady());assertEquals(0,player.getCurrentPosition());assertFalse(CarRadioCallback.playerCommands(true).contains(Player.COMMAND_PLAY_PAUSE));assertTrue(CarRadioCallback.playerCommands(false).contains(Player.COMMAND_PLAY_PAUSE));assertTrue(CarRadioCallback.sessionCommands().contains(CarRadioCallback.STOP_LIVE));
    CarRadioCallback callback=new CarRadioCallback(new CarRadioCatalog(service));try {callback.onCustomCommand(service.onGetSession(null),null,CarRadioCallback.STOP_LIVE,android.os.Bundle.EMPTY).get();assertEquals(Player.STATE_IDLE,player.getPlaybackState());assertFalse(player.getPlayWhenReady());} finally {callback.close();}
   } finally {controller.destroy();}
  }
 }
 @Test public void invalidStationStopsCleanly() {
  ServiceController<RadioPlaybackService> controller=Robolectric.buildService(RadioPlaybackService.class).create();RadioPlaybackService service=controller.get();
  try {service.onStartCommand(RadioPlaybackService.internalIntent(service,RadioPlaybackService.PLAY).putExtra("station","not-json"),0,1);assertEquals("",RadioPlaybackService.nowPlaying());assertTrue(Shadows.shadowOf(service).isStoppedBySelf());} finally {controller.destroy();}
 }
}
