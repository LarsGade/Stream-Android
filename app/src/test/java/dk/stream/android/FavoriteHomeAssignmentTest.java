package dk.stream.android;
import java.io.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class FavoriteHomeAssignmentTest {
 @Test public void directTvFilmSeriesChoicesSaveTheirRespectiveKinds()throws Exception{for(int kind=0;kind<3;kind++){Library library=new Library();LocalMedia file=new LocalMedia("content://files/one","One.mp4","video/mp4",false);FavoriteLists.Item item=FavoriteLists.of(file);FavoriteLists.assignHome(library,item,FavoriteHomeOrder.NAMES[kind]);Library restored=SettingsBackup.decode(new ByteArrayInputStream(SettingsBackup.encode(library)));assertEquals(1,FavoriteLists.items(restored,"Streaming").size());assertEquals(Playlist.Kind.values()[kind],FavoriteLists.items(restored,"Streaming").get(0).kind(restored));}}
 @Test public void otherHomeChoicesMoveItemsWithoutKeepingStreamingKind(){Library library=new Library();FavoriteLists.Item item=FavoriteLists.of(new RadioStation("id","Radio","https://example.com/radio",""));FavoriteLists.assignHome(library,item,"Film");for(String name:new String[]{"Radio","Podcast","Musik"}){FavoriteLists.assignHome(library,item,name);assertEquals(1,FavoriteLists.items(library,name).size());assertTrue(FavoriteLists.items(library,"Streaming").isEmpty());assertFalse(library.favoriteKinds.containsKey(item.key()));}}
}
