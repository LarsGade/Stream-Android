package dk.stream.android;
import android.app.Activity;
import android.view.View;
import android.widget.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class FavoriteHubTest {
 @Test public void homeHasFourButtonsAndBackReturnsHome(){Activity activity=Robolectric.buildActivity(Activity.class).setup().get();activity.setTheme(R.style.AppTheme);Library library=new Library();FavoriteHub hub=new FavoriteHub(activity,new FavoriteHub.Actions(){public Library library(){return library;}public boolean save(Runnable change){change.run();return true;}public void play(FavoriteLists.Item item,List<LocalMedia> queue){}public boolean playing(FavoriteLists.Item item){return false;}public void navigation(boolean back){}});LinearLayout home=ReflectionHelpers.getField(hub,"home");assertEquals(4,home.getChildCount());for(int i=0;i<4;i++)assertEquals(FavoriteLists.NAMES[i],((Button)home.getChildAt(i)).getText().toString());home.getChildAt(2).performClick();assertTrue(hub.canBack());TextView title=ReflectionHelpers.getField(hub,"title");assertEquals("Podcast",title.getText().toString());hub.back();assertFalse(hub.canBack());assertEquals(View.VISIBLE,home.getVisibility());hub.close();}
}
