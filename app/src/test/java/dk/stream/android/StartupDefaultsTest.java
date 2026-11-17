package dk.stream.android;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
public class StartupDefaultsTest {
 @Test public void newSetupStartsOnHomeWithAllThreeOptionsEnabled(){Library library=new Library();assertTrue(library.favoritesOnStart);assertTrue(library.autoPlayLast);assertTrue(library.miniPlayer);assertEquals(6,library.favoritesStartTab);}
 @Test public void missingOptionsUseNewDefaults()throws Exception{JSONObject root=new JSONObject(new Library().toJson());for(String key:new String[]{"favoritesOnStart","autoPlayLast","miniPlayer","favoritesStartTab"})root.remove(key);Library library=Library.fromJson(root.toString());assertTrue(library.favoritesOnStart);assertTrue(library.autoPlayLast);assertTrue(library.miniPlayer);assertEquals(6,library.favoritesStartTab);}
 @Test public void explicitSavedChoicesArePreserved()throws Exception{Library library=new Library();library.favoritesOnStart=false;library.autoPlayLast=false;library.miniPlayer=false;library.setFavoritesStartOption(3);Library restored=Library.fromJson(library.toJson());assertFalse(restored.favoritesOnStart);assertFalse(restored.autoPlayLast);assertFalse(restored.miniPlayer);assertEquals(3,restored.favoritesStartOption());}
}
