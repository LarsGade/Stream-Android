package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class PodcastFavoritesTest {
 @Test public void localAndCloudFilesAndFoldersSharePodcastAndSurviveBackup()throws Exception{
  Library library=new Library();
  for(String authority:new String[]{"documents","dk.stream.android.pcloud"})for(boolean folder:new boolean[]{false,true}){
   LocalMedia file=new LocalMedia("content://"+authority+"/"+folder,folder?"Episodes":"Episode.mp3",folder?"vnd.android.document/directory":"audio/mpeg",folder);
   FavoriteLists.assignHome(library,FavoriteLists.of(file),"Podcast");
   FavoriteLists.assignHome(library,FavoriteLists.of(file),"Podcast");
  }
  Library restored=Library.fromJson(library.toJson());
  assertEquals(4,FavoriteLists.items(restored,"Podcast").size());
  assertEquals(2,restored.sourceFavorites(false).size());assertEquals(2,restored.sourceFavorites(true).size());
  assertTrue(FavoriteLists.items(restored,"Podcast").get(0).file.folder);
 }
 @Test public void podcastStartupIsAlwaysFavoritesAndPortable()throws Exception{
  Library library=new Library();StartupPages.select(library,"Podcast");library.startupFavorites=false;
  Library restored=Library.fromJson(library.toJson());assertEquals("Podcast",StartupPages.selected(restored));assertTrue(StartupPages.favorites(restored));
  StartupPages.select(restored,"Radio");assertEquals("Radio",StartupPages.selected(restored));
 }
 @Test public void radioCannotBeAddedToPodcast(){assertFalse(FavoriteLists.homeAllowed(FavoriteLists.of(new RadioStation("id","Radio","https://example.com/radio","")),"Podcast"));}
 @Test public void previousDefaultAddsPodcastWithoutLosingOtherButtons()throws Exception{
  assertEquals(java.util.Arrays.asList(FavoriteHomeOrder.NAMES),FavoriteHomeOrder.read(new org.json.JSONArray(java.util.Arrays.asList("TV","Film","Serier","Musik","Radio","Lokale filer","Cloud"))));
 }
}
