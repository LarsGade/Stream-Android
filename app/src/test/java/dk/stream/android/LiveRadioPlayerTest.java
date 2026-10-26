package dk.stream.android;
import androidx.media3.common.*;
import java.lang.reflect.Proxy;
import org.junit.Test;
import static org.junit.Assert.*;
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=28)
public class LiveRadioPlayerTest {
 @Test public void pauseAndPlayWhenReadyFalseStopAndDiscardThePreviousPosition() {
  boolean[] ready={true};int[] state={Player.STATE_READY},prepares={0},pauses={0};long[] position={95000};
  MediaItem item=MediaItem.fromUri("https://example.com/live.mp3");
  Player delegate=(Player)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Player.class},(proxy,method,args)->{
   switch(method.getName()) {
    case "getPlayWhenReady":return ready[0];case "getPlaybackState":return state[0];case "getCurrentMediaItem":return item;
    case "setPlayWhenReady":ready[0]=(boolean)args[0];return null;case "stop":state[0]=Player.STATE_IDLE;return null;
    case "pause":pauses[0]++;return null;case "setMediaItem":assertTrue((boolean)args[1]);position[0]=0;return null;
    case "prepare":prepares[0]++;state[0]=Player.STATE_BUFFERING;return null;case "play":ready[0]=true;return null;default:return null;
   }
  });
  LiveRadioPlayer player=new LiveRadioPlayer(delegate);player.pause();assertFalse(ready[0]);assertEquals(Player.STATE_IDLE,state[0]);assertEquals(0,pauses[0]);
  player.play();assertEquals(0,position[0]);assertTrue(ready[0]);assertEquals(1,prepares[0]);
  position[0]=50000;player.setPlayWhenReady(false);assertEquals(Player.STATE_IDLE,state[0]);player.setPlayWhenReady(true);assertEquals(0,position[0]);assertEquals(2,prepares[0]);
 }
}
