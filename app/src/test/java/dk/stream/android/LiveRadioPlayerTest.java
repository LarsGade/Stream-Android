package dk.stream.android;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import okhttp3.mockwebserver.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class LiveRadioPlayerTest {
 @Test public void pauseAndPlayWhenReadyFalseStopAndDiscardThePreviousPosition() throws Exception {
  try(MockWebServer server=new MockWebServer()) {
   server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
   ExoPlayer delegate=new ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build();LiveRadioPlayer player=new LiveRadioPlayer(delegate);
   try {
    delegate.setMediaItem(MediaItem.fromUri(server.url("/live.mp3").toString()));delegate.seekTo(95000);delegate.play();
    player.pause();assertFalse(delegate.getPlayWhenReady());assertEquals(Player.STATE_IDLE,delegate.getPlaybackState());
    player.play();assertEquals(0,delegate.getCurrentPosition());assertTrue(delegate.getPlayWhenReady());
    delegate.seekTo(50000);player.setPlayWhenReady(false);assertEquals(Player.STATE_IDLE,delegate.getPlaybackState());player.setPlayWhenReady(true);assertEquals(0,delegate.getCurrentPosition());
   } finally {delegate.release();}
  }
 }
 @Test public void nativePlayerCommandsDropPauseWhilePlayingAndRestorePlayAfterStop() throws Exception {
  ExoPlayer delegate=new ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build();LiveRadioPlayer player=new LiveRadioPlayer(delegate);
  java.util.List<Player.Commands> changes=new java.util.ArrayList<>();player.addListener(new Player.Listener(){@Override public void onAvailableCommandsChanged(Player.Commands commands){changes.add(commands);}});
  try {
   delegate.setMediaItem(MediaItem.fromUri("https://example.com/live.mp3"));Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE));
   delegate.setPlayWhenReady(true);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertFalse(player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE));assertFalse(changes.get(changes.size()-1).contains(Player.COMMAND_PLAY_PAUSE));
   player.stop();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE));assertTrue(changes.get(changes.size()-1).contains(Player.COMMAND_PLAY_PAUSE));
  } finally {delegate.release();}
 }
}
