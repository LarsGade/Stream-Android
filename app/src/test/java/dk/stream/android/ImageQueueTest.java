package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class ImageQueueTest {
 private LocalMedia photo(String name){return new LocalMedia("content://pictures/"+name,name,"",false);}
 @Test public void recognizesImagesAndHidesExtensions(){for(String suffix:new String[]{"JPG","png","webp","heic","gif"}){LocalMedia p=photo("Holiday."+suffix);assertTrue(p.image());assertTrue(p.supported());assertFalse(p.audio());assertEquals("Holiday",p.displayName());}assertFalse(new LocalMedia("content://pictures/folder","folder.jpg","image/jpeg",true).image());}
 @Test public void keepsPictureOrderAndBounds(){LocalMedia a=photo("a.jpg"),b=photo("b.png"),audio=photo("sound.mp3");ImageQueue q=new ImageQueue(Arrays.asList(a,audio,b,a),b);assertEquals(2,q.entries.size());assertEquals(1,q.start);assertEquals(0,q.move(0,-1));assertEquals(1,q.move(1,1));assertEquals(0,q.move(1,-1));}
 @Test public void audioQueueSkipsPictures(){LocalMedia a=photo("song.mp3"),b=photo("photo.jpg");assertEquals(Collections.singletonList(a),new LocalQueue(Arrays.asList(a,b),a).entries);assertEquals(Collections.singletonList(b),new LocalQueue(Arrays.asList(a,b),b).entries);}
 @Test public void missingSelectionFallsBack(){LocalMedia a=photo("a.jpg"),b=photo("b.jpg");ImageQueue q=new ImageQueue(Collections.singletonList(a),b);assertEquals(1,q.entries.size());assertEquals(b,q.entries.get(0));}
 @Test public void separateFavoritesPersistAndMigrate() throws Exception {LocalMedia a=photo("a.jpg"),audio=photo("song.mp3");Library l=new Library();l.toggleLocalFavorite(a);l.toggleLocalFavorite(audio);l.setFavoritesStartTab(3);Library restored=Library.fromJson(l.toJson());assertEquals(1,restored.imageFavorites.size());assertEquals(1,restored.localFavorites.size());assertEquals(3,restored.favoritesStartTab);restored.toggleLocalFavorite(a);assertFalse(restored.isLocalFavorite(a.uri));org.json.JSONObject old=new org.json.JSONObject(l.toJson());old.remove("imageFavorites");old.getJSONArray("localFavorites").put(a.json());assertEquals(1,Library.fromJson(old.toString()).imageFavorites.size());}
 @Test public void sessionMovesWithoutWrapping(){LocalMedia a=photo("a.jpg"),b=photo("b.jpg");String token=ImageSession.start(a,Arrays.asList(a,b));assertFalse(ImageSession.move(-1));assertTrue(ImageSession.move(1));assertEquals(b,ImageSession.current());assertFalse(ImageSession.move(1));assertTrue(ImageSession.restore(token,a));assertEquals(b,ImageSession.current());}
}
