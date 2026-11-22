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
public class RadioSessionPlayerTest {
 @Test public void standardPausePreservesPositionAndPlaybackState() throws Exception {
  try(MockWebServer server=new MockWebServer()) {
   server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
   ExoPlayer delegate=new ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build();RadioSessionPlayer player=new RadioSessionPlayer(delegate,()->{});
   try {
    delegate.setMediaItem(MediaItem.fromUri(server.url("/live.mp3").toString()));player.play();delegate.seekTo(95000);
    player.pause();assertFalse(delegate.getPlayWhenReady());assertNotEquals(Player.STATE_IDLE,delegate.getPlaybackState());assertEquals(95000,delegate.getCurrentPosition());
    player.play();assertTrue(delegate.getPlayWhenReady());assertEquals(95000,delegate.getCurrentPosition());
    player.setPlayWhenReady(false);assertFalse(delegate.getPlayWhenReady());assertEquals(95000,delegate.getCurrentPosition());
    player.stop();assertEquals(Player.STATE_IDLE,delegate.getPlaybackState());assertFalse(delegate.getPlayWhenReady());
   } finally {delegate.release();}
  }
 }
 @Test public void standardPlayPauseCommandRemainsAvailableWhilePlayingAndStopped() {
  ExoPlayer delegate=new ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build();RadioSessionPlayer player=new RadioSessionPlayer(delegate,()->{});
  try {
   delegate.setMediaItem(MediaItem.fromUri("https://example.com/live.mp3"));assertTrue(player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE));
   delegate.setPlayWhenReady(true);assertTrue(player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE));
   player.pause();assertTrue(player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE));
   player.stop();assertTrue(player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE));
  } finally {delegate.release();}
 }
}
