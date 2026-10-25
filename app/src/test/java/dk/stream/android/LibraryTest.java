package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class LibraryTest {
 private Playlist.Channel channel(String name,String url) {return new Playlist.Channel(name,url,"DK",Playlist.Kind.TV,null,"https://example.com/logo.png");}
 @Test public void favoritesAndStartupPreferenceSurviveReload() throws Exception {
  Library library=new Library();Library.Source source=library.addSource(Library.Source.m3u("Min liste","https://example.com/get.php?password=p%26x"));
  library.lastSourceId=source.id;library.favoritesOnStart=true;
  Library.Entry z=new Library.Entry(source.id,channel("Z","https://example.com/z"));
  Library.Entry a=new Library.Entry(source.id,channel("A","https://example.com/a"));
  library.toggleFavorite(z);library.toggleFavorite(a);
  Library restored=Library.fromJson(library.toJson());
  assertTrue(restored.favoritesOnStart);assertEquals(source.id,restored.lastSourceId);assertTrue(restored.isFavorite(z));
  assertEquals("Z",restored.favorites.get(0).channel.name);assertEquals("A",restored.favorites.get(1).channel.name);
  assertEquals("https://example.com/logo.png",restored.favorites.get(0).channel.logoUrl);
 }
 @Test public void togglingIsSourceSpecificAndRemovesDuplicates() {
  Library library=new Library();Playlist.Channel channel=channel("TV","https://example.com/tv");
  Library.Entry one=new Library.Entry("one",channel),two=new Library.Entry("two",channel);
  library.toggleFavorite(one);library.toggleFavorite(two);assertEquals(2,library.favorites.size());
  library.toggleFavorite(new Library.Entry("one",channel("Renamed","https://example.com/tv")));
  assertEquals(1,library.favorites.size());assertTrue(library.isFavorite(two));assertFalse(library.isFavorite(one));
 }
 @Test public void filtersFavoritesWithoutSortingOrMixingSections() {
  Library library=new Library();library.toggleFavorite(new Library.Entry("s",channel("Z","https://example.com/z")));
  library.toggleFavorite(new Library.Entry("s",new Playlist.Channel("Movie","https://example.com/movie","Action",Playlist.Kind.FILM,null)));
  library.toggleFavorite(new Library.Entry("s",channel("A","https://example.com/a")));
  var tv=library.favoriteItems(Playlist.Kind.TV,"DK","");assertEquals(2,tv.size());assertEquals("Z",tv.get(0).channel.name);
  assertEquals(1,library.favoriteItems(Playlist.Kind.FILM,null,"movie").size());assertTrue(library.favoriteItems(Playlist.Kind.TV,null,"movie").isEmpty());
 }
 @Test public void savedXtreamSourceRetainsExactCredentialsAndSeriesFavorites() throws Exception {
  Library library=new Library();Library.Source source=library.addSource(Library.Source.xtream("TV server","https://example.com:8443","u+æ","p& +#"));
  library.toggleFavorite(new Library.Entry(source.id,new Playlist.Channel("Series","","Drama",Playlist.Kind.SERIES,"99","https://example.com/cover.jpg")));
  Library restored=Library.fromJson(library.toJson());
  assertEquals("p& +#",restored.findSource(source.id).password);assertEquals("u+æ",restored.findSource(source.id).username);assertTrue(restored.findSource(source.id).xtream);
  assertEquals("99",restored.favorites.get(0).channel.seriesId);
 }
 @Test public void removingSourceRemovesOnlyItsFavorites() {
  Library library=new Library();var one=library.addSource(Library.Source.m3u("one","https://example.com/one"));var two=library.addSource(Library.Source.m3u("two","https://example.com/two"));library.lastSourceId=one.id;
  library.toggleFavorite(new Library.Entry(one.id,channel("one","https://example.com/1")));library.toggleFavorite(new Library.Entry(two.id,channel("two","https://example.com/2")));
  library.removeSource(one.id);assertEquals(1,library.sources.size());assertEquals(1,library.favorites.size());assertEquals(two.id,library.lastSourceId);
 }
 @Test public void reloadingSameSourceDoesNotDuplicateIt() {
  Library library=new Library();var old=library.addSource(Library.Source.m3u("One","https://example.com/list"));
  var again=library.addSource(Library.Source.m3u("Other name","https://example.com/list"));assertEquals(old.id,again.id);assertEquals(1,library.sources.size());
 }
 @Test public void refreshUpdatesLogoAndTitleWithoutReorderingFavorites() {
  Library library=new Library();library.toggleFavorite(new Library.Entry("s",channel("Old Z","https://example.com/z")));library.toggleFavorite(new Library.Entry("s",channel("A","https://example.com/a")));
  var fresh=new Playlist.Channel("New Z","https://example.com/z","DK",Playlist.Kind.TV,null,"https://example.com/new.png");
  library.refreshFavorites("s",Arrays.asList(channel("A","https://example.com/a"),fresh));
  assertEquals("New Z",library.favorites.get(0).channel.name);assertEquals("https://example.com/new.png",library.favorites.get(0).channel.logoUrl);assertEquals("A",library.favorites.get(1).channel.name);
 }
 @Test(expected=org.json.JSONException.class) public void rejectsUnknownStorageVersion() throws Exception {Library.fromJson("{\"version\":999}");}
 @Test public void guidesAndSourceSelectionSurviveUpgradeAndDeletionKeepsFavorites() throws Exception {
  Library library=new Library();var guide=Library.Guide.create("Dansk TV","https://example.com/guide.xml.gz");library.putGuide(guide);
  var source=library.addSource(Library.Source.m3u("TV","https://example.com/list").withEpg(guide.id));
  var channel=new Playlist.Channel("DR1","https://example.com/live","DK",Playlist.Kind.TV,null,"","dr1.dk","DR 1");library.toggleFavorite(new Library.Entry(source.id,channel));
  Library loaded=Library.fromJson(library.toJson());assertEquals(guide.id,loaded.findSource(source.id).epgId);assertEquals("dr1.dk",loaded.favorites.get(0).channel.epgId);
  loaded.removeGuide(guide.id);assertEquals("",loaded.findSource(source.id).epgId);assertEquals(1,loaded.favorites.size());assertEquals(1,loaded.sources.size());
 }
 @Test public void editingSourceKeepsItsIdentityAndRefreshesFavoriteUrls() {
  Library library=new Library();var source=library.addSource(Library.Source.m3u("Old name","https://example.com/old"));
  library.toggleFavorite(new Library.Entry(source.id,new Playlist.Channel("DR1","https://example.com/old-stream","DK")));
  library.addSource(new Library.Source(source.id,"New name","https://example.com/new","","",false));
  library.refreshFavorites(source.id,java.util.Arrays.asList(new Playlist.Channel("DR1","https://example.com/new-stream","DK")));
  assertEquals(1,library.sources.size());assertEquals("New name",library.findSource(source.id).name);assertEquals("https://example.com/new-stream",library.favorites.get(0).channel.url);
 }
 @Test public void manualFavoriteOrderSurvivesReloadAndRemovalDoesNotToggleBackOn() throws Exception {
  Library library=new Library();var source=library.addSource(Library.Source.m3u("TV","https://example.com/list"));
  Library.Entry first=new Library.Entry(source.id,channel("A","https://example.com/a")),second=new Library.Entry(source.id,channel("B","https://example.com/b"));
  library.toggleFavorite(first);library.toggleFavorite(second);library.reorderFavorites(java.util.Arrays.asList(second,first));
  Library restored=Library.fromJson(library.toJson());assertEquals("B",restored.favorites.get(0).channel.name);assertEquals("A",restored.favorites.get(1).channel.name);
  restored.removeFavorite(second);restored.removeFavorite(second);assertEquals(1,restored.favorites.size());assertEquals("A",restored.favorites.get(0).channel.name);
 }
 @Test public void invalidReorderDoesNotLoseFavorites() {
  Library library=new Library();Library.Entry one=new Library.Entry("s",channel("A","https://example.com/a")),two=new Library.Entry("s",channel("B","https://example.com/b"));library.toggleFavorite(one);library.toggleFavorite(two);
  try {library.reorderFavorites(java.util.Arrays.asList(one,one));fail("Expected invalid favorite set");} catch(IllegalArgumentException expected) { }
  assertEquals(2,library.favorites.size());assertTrue(library.isFavorite(two));
 }
}
