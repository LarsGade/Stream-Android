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
 @Test public void serviceStartsForegroundMediaSessionAndCanPauseResumeAndStop() throws Exception {
  try(MockWebServer server=new MockWebServer()) {
   server.enqueue(new MockResponse().setResponseCode(404));server.enqueue(new MockResponse().setResponseCode(404));
   ServiceController<RadioPlaybackService> controller=Robolectric.buildService(RadioPlaybackService.class).create();RadioPlaybackService service=controller.get();
   try {
    RadioStation station=new RadioStation("station","P1",server.url("/radio.mp3").toString(),"");
    service.onStartCommand(RadioPlaybackService.playIntent(service,station),0,1);Player player=service.onGetSession(null).getPlayer();
    assertTrue(player.getPlayWhenReady());assertEquals("P1",RadioPlaybackService.nowPlaying());assertTrue(RadioPlaybackService.playing(station.id));assertNotNull(Shadows.shadowOf(service).getLastForegroundNotification());
    Intent toggle=RadioPlaybackService.playIntent(service,station).setAction(RadioPlaybackService.TOGGLE);
    service.onStartCommand(toggle,0,2);assertFalse(player.getPlayWhenReady());assertFalse(RadioPlaybackService.playing(station.id));
    service.onStartCommand(toggle,0,3);assertTrue(player.getPlayWhenReady());
    service.onStartCommand(new Intent(service,RadioPlaybackService.class).setAction(RadioPlaybackService.STOP),0,4);assertEquals("",RadioPlaybackService.nowPlaying());assertEquals(Player.STATE_IDLE,player.getPlaybackState());
   } finally {controller.destroy();}
   assertFalse(RadioPlaybackService.playing("station"));
  }
 }
 @Test public void invalidStationStopsCleanly() {
  ServiceController<RadioPlaybackService> controller=Robolectric.buildService(RadioPlaybackService.class).create();RadioPlaybackService service=controller.get();
  try {service.onStartCommand(new Intent(service,RadioPlaybackService.class).setAction(RadioPlaybackService.PLAY).putExtra("station","not-json"),0,1);assertEquals("",RadioPlaybackService.nowPlaying());assertTrue(Shadows.shadowOf(service).isStoppedBySelf());} finally {controller.destroy();}
 }
}
