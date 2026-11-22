package dk.stream.android;

import android.app.*;
import android.content.*;
import androidx.media3.common.Player;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk={36,37})
public class ModernAndroidTest {
 @Test public void activityRadioAndAndroidAutoServiceStartOnAndroid16And17() {
  Application app=RuntimeEnvironment.getApplication();Shadows.shadowOf(app).grantPermissions("android.permission.POST_NOTIFICATIONS","dk.stream.android.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION");
  org.robolectric.android.controller.ActivityController<MainActivity> activity=Robolectric.buildActivity(MainActivity.class).setup();
  org.robolectric.android.controller.ServiceController<RadioPlaybackService> service=Robolectric.buildService(RadioPlaybackService.class).create();
  try {assertNotNull(activity.get().findViewById(android.R.id.content));assertTrue(service.get().onGetSession(null) instanceof androidx.media3.session.MediaLibraryService.MediaLibrarySession);assertFalse(service.get().onGetSession(null).getPlayer().getPlayWhenReady());}
  finally {service.destroy();activity.pause().stop().destroy();}
 }
}
