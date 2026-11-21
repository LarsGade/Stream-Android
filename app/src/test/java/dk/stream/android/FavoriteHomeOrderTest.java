package dk.stream.android;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class FavoriteHomeOrderTest {
 @Test public void movesHomeShortcutsIndependently(){Library library=new Library();library.moveFavoriteHome("Film",-1);assertEquals(Arrays.asList("Film","TV","Serier","Radio","Lokale filer","Cloud","Musik"),library.favoriteHomeOrder);assertEquals(7,new HashSet<>(library.favoriteHomeOrder).size());}
 @Test public void boundaryMovesDoNotChangeOrder(){Library library=new Library();library.moveFavoriteHome("TV",-1);library.moveFavoriteHome("Musik",1);assertEquals(Arrays.asList(FavoriteHomeOrder.NAMES),library.favoriteHomeOrder);}
 @Test public void orderSurvivesBackup()throws Exception{Library library=new Library();library.moveFavoriteHome("Musik",-1);Library restored=SettingsBackup.decode(new java.io.ByteArrayInputStream(SettingsBackup.encode(library)));assertEquals(library.favoriteHomeOrder,restored.favoriteHomeOrder);}
 @Test public void legacySettingsGetDefaultOrder()throws Exception{org.json.JSONObject saved=new org.json.JSONObject(new Library().toJson());saved.remove("favoriteHomeOrder");assertEquals(Arrays.asList(FavoriteHomeOrder.NAMES),Library.fromJson(saved.toString()).favoriteHomeOrder);}
 @Test public void oldStreamingButtonExpandsAtItsSavedPosition()throws Exception{org.json.JSONObject saved=new org.json.JSONObject(new Library().toJson());saved.put("favoriteHomeOrder",new org.json.JSONArray(Arrays.asList("Radio","Podcast","Streaming","Musik")));assertEquals(Arrays.asList("Radio","TV","Film","Serier","Musik","Lokale filer","Cloud"),Library.fromJson(saved.toString()).favoriteHomeOrder);}
 @Test(expected=org.json.JSONException.class) public void duplicateCategoryIsRejected()throws Exception{org.json.JSONObject saved=new org.json.JSONObject(new Library().toJson());saved.put("favoriteHomeOrder",new org.json.JSONArray(Arrays.asList("Radio","Radio","Podcast","Musik")));Library.fromJson(saved.toString());}
 @Test public void visibilitySurvivesExportAndImport()throws Exception{Library library=new Library();library.hiddenHomeButtons.add("Film");library.hiddenHomeButtons.add("Musik");Library restored=SettingsBackup.decode(new java.io.ByteArrayInputStream(SettingsBackup.encode(library)));assertEquals(library.hiddenHomeButtons,restored.hiddenHomeButtons);assertEquals(library.favoriteHomeOrder,restored.favoriteHomeOrder);}
 @Test public void oldSettingsShowAllButtons()throws Exception{org.json.JSONObject saved=new org.json.JSONObject(new Library().toJson());saved.remove("hiddenHomeButtons");assertTrue(Library.fromJson(saved.toString()).hiddenHomeButtons.isEmpty());}
 @Test(expected=org.json.JSONException.class)public void invalidHiddenButtonIsRejected()throws Exception{org.json.JSONObject saved=new org.json.JSONObject(new Library().toJson());saved.put("hiddenHomeButtons",new org.json.JSONArray(Arrays.asList("Unknown")));Library.fromJson(saved.toString());}
}
