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
 @Test public void homeHasSevenNamedDestinations(){Activity activity=Robolectric.buildActivity(Activity.class).setup().get();activity.setTheme(R.style.AppTheme);Library library=new Library();String[] opened={""};FavoriteHub hub=new FavoriteHub(activity,new FavoriteHub.Actions(){public Library library(){return library;}public boolean save(Runnable change){change.run();return true;}public void play(FavoriteLists.Item item,List<LocalMedia> queue){}public boolean playing(FavoriteLists.Item item){return false;}public void navigation(boolean back){}public void openMenu(){}public void openPage(String name){opened[0]=name;}});try{LinearLayout home=ReflectionHelpers.getField(hub,"home");assertEquals(7,home.getChildCount());for(int i=0;i<7;i++){assertEquals(FavoriteHomeOrder.NAMES[i],((Button)home.getChildAt(i)).getText().toString());home.getChildAt(i).performClick();assertEquals(FavoriteHomeOrder.NAMES[i],opened[0]);}hub.showList("Radio");assertTrue(hub.canBack());hub.back();assertFalse(hub.canBack());assertEquals(View.VISIBLE,home.getVisibility());}finally{hub.close();}}
 @Test public void tvFavoriteShowsCurrentProgrammeAndRefreshesIt(){Activity activity=Robolectric.buildActivity(Activity.class).setup().get();activity.setTheme(R.style.AppTheme);Library library=new Library();Library.Source source=library.addSource(Library.Source.m3u("TV","https://example.com/tv.m3u"));library.toggleFavorite(new Library.Entry(source.id,new Playlist.Channel("Test TV","https://example.com/live","",Playlist.Kind.TV,null,"")));String[] programme={"Nyhederne"};FavoriteHub hub=new FavoriteHub(activity,new FavoriteHub.Actions(){public Library library(){return library;}public boolean save(Runnable change){change.run();return true;}public void play(FavoriteLists.Item item,List<LocalMedia> queue){}public boolean playing(FavoriteLists.Item item){return false;}public void navigation(boolean back){}public void openMenu(){}public String programme(Library.Entry entry){assertEquals("Test TV",entry.channel.name);return programme[0];}});try{hub.showList("Streaming");ListView list=ReflectionHelpers.getField(hub,"list");View first=list.getAdapter().getView(0,null,list);assertTrue(hasText(first,"Test TV"));assertTrue(hasText(first,"Nyhederne"));programme[0]="Vejret";hub.programmeChanged();View updated=list.getAdapter().getView(0,null,list);assertTrue(hasText(updated,"Vejret"));assertFalse(hasText(updated,"Nyhederne"));}finally{hub.close();}}
 private boolean hasText(View view,String text){if(view instanceof TextView&&text.contentEquals(((TextView)view).getText()))return true;if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++)if(hasText(group.getChildAt(i),text))return true;}return false;}
}
