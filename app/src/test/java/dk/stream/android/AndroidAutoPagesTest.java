package dk.stream.android;
import java.io.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class AndroidAutoPagesTest {
 @Test public void newAndOlderSettingsPreferRadioFavorites()throws Exception{assertEquals(AndroidAutoPages.FAVORITES,new Library().androidAutoStartPage);assertEquals(AndroidAutoPages.FAVORITES,Library.fromJson("{\"version\":1,\"sources\":[],\"favorites\":[]}").androidAutoStartPage);}
 @Test public void everyStartPageSurvivesLibraryRoundTrip()throws Exception{for(String page:AndroidAutoPages.IDS){Library library=new Library();library.androidAutoStartPage=page;assertEquals(page,Library.fromJson(library.toJson()).androidAutoStartPage);}}
 @Test public void choiceIsIncludedInPortableSettingsBackup()throws Exception{Library library=new Library();library.androidAutoStartPage=AndroidAutoPages.PODCAST;assertEquals(AndroidAutoPages.PODCAST,SettingsBackup.decode(new ByteArrayInputStream(SettingsBackup.encode(library))).androidAutoStartPage);}
 @Test public void unknownFuturePageFallsBackToFavorites()throws Exception{assertEquals(AndroidAutoPages.FAVORITES,Library.fromJson("{\"version\":1,\"sources\":[],\"favorites\":[],\"androidAutoStartPage\":\"not-supported\"}").androidAutoStartPage);}
 @Test public void emptyFavoritesHaveAnAccessibleOverview(){assertEquals(AndroidAutoPages.ROOT,AndroidAutoPages.effective(AndroidAutoPages.FAVORITES,false));assertEquals(AndroidAutoPages.ROOT,AndroidAutoPages.effective("radio-england",false));assertEquals(AndroidAutoPages.FAVORITES,AndroidAutoPages.effective(AndroidAutoPages.FAVORITES,true));}
 @Test public void directStartStillOffersOtherPagesWithoutChangingTheOriginalList(){List<String> channels=new ArrayList<>(Arrays.asList("P1","P3"));assertEquals(Arrays.asList("P1","P3","Alle sider"),AndroidAutoPages.startItems(AndroidAutoPages.FAVORITES,channels,"Alle sider"));assertEquals(Arrays.asList("P1","P3"),channels);assertEquals(Collections.singletonList("Alle sider"),AndroidAutoPages.startItems("radio-usa",Collections.emptyList(),"Alle sider"));}
 @Test public void overviewDoesNotLinkToItself(){assertEquals(Arrays.asList("Favoritter","Danmark","England","USA"),AndroidAutoPages.startItems(AndroidAutoPages.ROOT,Arrays.asList("Favoritter","Danmark","England","USA"),"Alle sider"));}
 @Test public void allCountriesAndStartRootAreBrowsable(){assertTrue(AndroidAutoPages.folder(AndroidAutoPages.START));for(String id:AndroidAutoPages.IDS)assertTrue(AndroidAutoPages.folder(id));assertFalse(AndroidAutoPages.folder("radio:station"));assertFalse(AndroidAutoPages.folder(null));}
 @Test public void phoneStartTabIsIndependent()throws Exception{Library library=new Library();library.setFavoritesStartOption(3);library.androidAutoStartPage=AndroidAutoPages.PODCAST;Library restored=Library.fromJson(library.toJson());assertEquals(3,restored.favoritesStartOption());assertEquals(AndroidAutoPages.PODCAST,restored.androidAutoStartPage);}
 @Test public void onlyFavoritePagesCanBeSelected(){assertArrayEquals(new String[]{"Forside","Radio","Podcast"},AndroidAutoPages.LABELS);assertEquals(3,AndroidAutoPages.IDS.length);assertEquals(AndroidAutoPages.FAVORITES,AndroidAutoPages.normalize("radio-usa"));}
}
