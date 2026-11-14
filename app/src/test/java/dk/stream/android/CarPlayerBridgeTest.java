package dk.stream.android;

import android.content.Intent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class CarPlayerBridgeTest {
 @Test public void documentHandoffGrantsOnlyReadAccessToTheExactSelectedUri(){CarVideo video=new CarVideo("Movie","content://docs/tree/root/document/root%2FMovie.mp4","video/mp4");Intent intent=CarPlayerBridge.viewIntent(video,12000);assertEquals(Intent.ACTION_VIEW,intent.getAction());assertEquals(video.uri,intent.getDataString());assertEquals(video.uri,intent.getClipData().getItemAt(0).getUri().toString());assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION,intent.getFlags());assertEquals(12000,intent.getLongExtra("position",0));}
 @Test public void networkHandoffHasNoDocumentGrantAndClampsInvalidPosition(){Intent intent=CarPlayerBridge.viewIntent(new CarVideo("TV","https://example.com/live.m3u8","application/x-mpegurl"),-1);assertEquals(0,intent.getFlags());assertNull(intent.getClipData());assertEquals(0,intent.getLongExtra("position",-1));assertEquals("TV",intent.getStringExtra(Intent.EXTRA_TITLE));}
}
