package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import java.io.*;
import java.util.*;
public class PageFavoriteViewsTest {
 @Test public void everyPageStartsWithItsOrdinaryList(){Library library=new Library();for(String name:FavoriteHomeOrder.NAMES)assertFalse(Boolean.TRUE.equals(library.pageFavorites.get(name)));}
 @Test public void pageChoicesAreIndependentAndExported()throws Exception{Library library=new Library();library.pageFavorites.put("Radio",true);library.pageFavorites.put("Cloud",true);library.pageFavorites.put("Musik",true);library.pageFavorites.put("TV",false);Library restored=SettingsBackup.decode(new ByteArrayInputStream(SettingsBackup.encode(library)));assertEquals(library.pageFavorites,restored.pageFavorites);assertFalse(Boolean.TRUE.equals(restored.pageFavorites.get("Lokale filer")));}
 @Test public void previousHomeKeepsPodcastAndPreservesRelativeOrder()throws Exception{JSONObject saved=new JSONObject(new Library().toJson());saved.put("favoriteHomeOrder",new JSONArray(Arrays.asList("Musik","TV","Film","Serier","Radio","Podcast")));saved.put("hiddenHomeButtons",new JSONArray(Arrays.asList("Podcast","TV")));Library restored=Library.fromJson(saved.toString());assertEquals(Arrays.asList("Musik","TV","Film","Serier","Radio","Podcast","Lokale filer","Cloud"),restored.favoriteHomeOrder);assertEquals(new LinkedHashSet<>(Arrays.asList("Podcast","TV")),restored.hiddenHomeButtons);}
 @Test(expected=JSONException.class) public void unknownPageStateIsRejected()throws Exception{JSONObject saved=new JSONObject(new Library().toJson());saved.put("pageFavorites",new JSONObject().put("unknown",true));Library.fromJson(saved.toString());}
 @Test public void filesCanBeAssignedToTheirOwnSourceFavoritesAndExported()throws Exception{for(boolean cloud:new boolean[]{false,true}){Library library=new Library();String uri=cloud?"content://dk.stream.android.pcloud/account?url=test":"content://documents/file";LocalMedia file=new LocalMedia(uri,"Song.mp3","audio/mpeg",false);String list=cloud?"Cloud":"Lokale filer";FavoriteLists.assignHome(library,FavoriteLists.of(file),list);Library restored=SettingsBackup.decode(new ByteArrayInputStream(SettingsBackup.encode(library)));assertEquals(1,restored.sourceFavorites(cloud).size());assertEquals(1,FavoriteLists.items(restored,list).size());assertFalse(FavoriteLists.homeAllowed(FavoriteLists.of(file),cloud?"Lokale filer":"Cloud"));}}
}
