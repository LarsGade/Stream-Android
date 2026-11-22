package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class MusicAlbumFavoritesTest {
 private LocalMedia song(String id){return new LocalMedia("content://music/"+id.replace(" ","_"),id+".m4a","audio/mp4",false);}
 @Test public void addsAlbumInTrackOrderWithoutDuplicates(){Library library=new Library();LocalMedia old=song("Existing"),first=song("Z first"),second=song("A second");FavoriteLists.assign(library,FavoriteLists.of(old),"Musik");MusicAlbumFavorites.add(library,Arrays.asList(first,second,first));List<FavoriteLists.Item> favorites=FavoriteLists.items(library,"Musik");assertEquals(3,favorites.size());assertEquals(old.uri,favorites.get(0).file.uri);assertEquals(first.uri,favorites.get(1).file.uri);assertEquals(second.uri,favorites.get(2).file.uri);MusicAlbumFavorites.add(library,Arrays.asList(first,second));assertEquals(3,FavoriteLists.items(library,"Musik").size());}
 @Test public void albumOrderSurvivesPortableBackup()throws Exception{Library library=new Library();MusicAlbumFavorites.add(library,Arrays.asList(song("Z first"),song("A second")));Library restored=SettingsBackup.decode(new java.io.ByteArrayInputStream(SettingsBackup.encode(library)));assertEquals("Z first",FavoriteLists.items(restored,"Musik").get(0).name());assertEquals("A second",FavoriteLists.items(restored,"Musik").get(1).name());}
}
