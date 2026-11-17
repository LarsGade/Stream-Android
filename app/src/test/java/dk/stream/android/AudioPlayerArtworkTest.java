package dk.stream.android;

import android.content.Intent;
import android.view.View;
import android.widget.*;
import androidx.media3.common.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class AudioPlayerArtworkTest {
 private ServiceController<LocalAudioService> service;private ActivityController<AudioPlayerActivity> screen;
 @Before public void setup(){Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions("dk.stream.android.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION");service=Robolectric.buildService(LocalAudioService.class).create();}
 @After public void close(){if(screen!=null)screen.destroy();service.destroy();}
 private void select(String name,byte[] cover){LocalMedia file=new LocalMedia("content://test/"+name,name,"audio/mpeg",false);MediaItem item=LocalAudioService.item(file);LocalAudioService.player().setMediaItem(item.buildUpon().setMediaMetadata(item.mediaMetadata.buildUpon().setTitle("Udsendelse fra metadata").setArtworkData(cover,MediaMetadata.PICTURE_TYPE_FRONT_COVER).build()).build());LocalAudioService.player().prepare();}
 @Test public void embeddedCoverIsShownAndClearsForNextFileWithoutCover() throws Exception {
  byte[] cover;try(java.io.InputStream in=getClass().getResourceAsStream("/logos/blue.png")){cover=in.readAllBytes();}
  select("First.mp3",cover);screen=Robolectric.buildActivity(AudioPlayerActivity.class,new Intent(RuntimeEnvironment.getApplication(),AudioPlayerActivity.class).putExtra("local",true)).create();AudioPlayerActivity activity=screen.get();
  assertEquals("First",ReflectionHelpers.<TextView>getField(activity,"title").getText().toString());assertArrayEquals(cover,ReflectionHelpers.<byte[]>getField(activity,"artworkModel"));assertEquals(View.VISIBLE,ReflectionHelpers.<ImageView>getField(activity,"artwork").getVisibility());
  select("Second.mp3",null);ReflectionHelpers.callInstanceMethod(activity,"refresh");assertEquals("Second",ReflectionHelpers.<TextView>getField(activity,"title").getText().toString());assertNull(ReflectionHelpers.getField(activity,"artworkModel"));assertEquals(View.GONE,ReflectionHelpers.<ImageView>getField(activity,"artwork").getVisibility());assertEquals(PlaybackText.SOURCE,LocalAudioService.item(new LocalMedia("content://test/file","file.mp3","audio/mpeg",false)).mediaMetadata.artist);
 }
}
