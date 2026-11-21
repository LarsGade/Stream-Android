package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.io.*;
public class StartupPagesTest {
 @Test public void startupViewOverridesRememberedPageAndSurvivesBackup()throws Exception{Library library=new Library();StartupPages.select(library,"Radio");library.pageFavorites.put("Radio",true);assertTrue(StartupPages.favorites(library));library.startupFavorites=false;Library restored=SettingsBackup.decode(new ByteArrayInputStream(SettingsBackup.encode(library)));assertFalse(StartupPages.favorites(restored));assertTrue(restored.pageFavorites.get("Radio"));restored.startupFavorites=true;assertTrue(StartupPages.favorites(SettingsBackup.decode(new ByteArrayInputStream(SettingsBackup.encode(restored)))));}
 @Test public void oldSettingsKeepRememberedView()throws Exception{Library library=new Library();StartupPages.select(library,"Cloud");library.pageFavorites.put("Cloud",true);org.json.JSONObject saved=new org.json.JSONObject(library.toJson());saved.remove("startupFavorites");assertTrue(StartupPages.favorites(Library.fromJson(saved.toString())));}
 @Test public void offersHomeAndExactlyTheVisibleButtonsInTheirOrder(){Library library=new Library();library.hiddenHomeButtons.add("Film");library.moveFavoriteHome("Cloud",-1);List<String> expected=new ArrayList<>();expected.add("Forside");for(String name:library.favoriteHomeOrder)if(!name.equals("Film"))expected.add(name);assertEquals(expected,StartupPages.choices(library));assertFalse(StartupPages.choices(library).contains("Podcast"));}
 @Test public void everyDestinationRoundTripsThroughPortableSettings()throws Exception{Library library=new Library();for(String name:StartupPages.choices(library)){StartupPages.select(library,name);Library restored=SettingsBackup.decode(new ByteArrayInputStream(SettingsBackup.encode(library)));assertEquals(name,StartupPages.selected(restored));}}
 @Test public void hidingSelectedPageFallsBackToHome(){Library library=new Library();StartupPages.select(library,"Radio");library.hiddenHomeButtons.add("Radio");assertEquals("Forside",StartupPages.selected(library));}
 @Test public void olderDisabledFavoriteStartupBecomesHome(){Library library=new Library();library.setFavoritesStartOption(3);library.favoritesOnStart=false;assertEquals("Forside",StartupPages.selected(library));StartupPages.select(library,"Cloud");assertTrue(library.favoritesOnStart);assertEquals("Cloud",StartupPages.selected(library));}
 @Test public void allHiddenButtonsLeaveOnlyHome(){Library library=new Library();library.hiddenHomeButtons.addAll(Arrays.asList(FavoriteHomeOrder.NAMES));assertEquals(Collections.singletonList("Forside"),StartupPages.choices(library));}
 @Test(expected=IllegalArgumentException.class) public void hiddenPageCannotBeSelected(){Library library=new Library();library.hiddenHomeButtons.add("TV");StartupPages.select(library,"TV");}
}
