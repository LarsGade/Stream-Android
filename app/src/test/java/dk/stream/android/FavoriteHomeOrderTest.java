package dk.stream.android;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class FavoriteHomeOrderTest {
 @Test public void movesButtonsAndKeepsAllFourCategories(){Library library=new Library();library.moveFavoriteHome("Radio",-1);assertEquals(Arrays.asList("Radio","Streaming","Podcast","Musik"),library.favoriteHomeOrder);library.moveFavoriteHome("Streaming",1);assertEquals(Arrays.asList("Radio","Podcast","Streaming","Musik"),library.favoriteHomeOrder);assertEquals(4,new HashSet<>(library.favoriteHomeOrder).size());}
 @Test public void boundaryMovesDoNotChangeOrder(){Library library=new Library();library.moveFavoriteHome("Streaming",-1);library.moveFavoriteHome("Musik",1);assertEquals(Arrays.asList(FavoriteLists.NAMES),library.favoriteHomeOrder);}
 @Test public void orderSurvivesBackup()throws Exception{Library library=new Library();library.moveFavoriteHome("Musik",-1);library.moveFavoriteHome("Musik",-1);Library restored=SettingsBackup.decode(new java.io.ByteArrayInputStream(SettingsBackup.encode(library)));assertEquals(library.favoriteHomeOrder,restored.favoriteHomeOrder);}
 @Test public void legacySettingsGetDefaultOrder()throws Exception{org.json.JSONObject saved=new org.json.JSONObject(new Library().toJson());saved.remove("favoriteHomeOrder");assertEquals(Arrays.asList(FavoriteLists.NAMES),Library.fromJson(saved.toString()).favoriteHomeOrder);}
 @Test(expected=org.json.JSONException.class) public void duplicateCategoryIsRejected()throws Exception{org.json.JSONObject saved=new org.json.JSONObject(new Library().toJson());saved.put("favoriteHomeOrder",new org.json.JSONArray(Arrays.asList("Radio","Radio","Podcast","Musik")));Library.fromJson(saved.toString());}
}
