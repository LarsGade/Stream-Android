package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class CatalogSnapshotTest {
 @Test public void roundTripKeepsChannelsMetadataOrderAndEmptyGroups()throws Exception{
  Library.Source source=Library.Source.m3u("Test","https://example.com/list.m3u");Catalog catalog=new Catalog();
  catalog.add(new Playlist.Channel("TV","https://example.com/live","News",Playlist.Kind.TV,null,"https://example.com/logo","guide-id","Guide name"));
  catalog.add(new Playlist.Channel("Serie","https://example.com/series","Drama",Playlist.Kind.SERIES,"42"));catalog.add(catalog.items.get(0));catalog.addGroup(Playlist.Kind.FILM,"Empty");
  Catalog restored=CatalogSnapshot.decode(source,CatalogSnapshot.encode(source,catalog));assertEquals(3,restored.items.size());assertEquals("42",restored.items.get(1).seriesId);assertEquals("guide-id",restored.items.get(0).epgId);assertEquals("Guide name",restored.items.get(0).epgName);assertEquals("https://example.com/logo",restored.items.get(0).logoUrl);assertEquals("Empty",restored.groups(Playlist.Kind.FILM).get(0));
 }
 @Test public void emptyCatalogIsStillValid()throws Exception{Library.Source source=Library.Source.m3u("Test","https://example.com/list");assertTrue(CatalogSnapshot.decode(source,CatalogSnapshot.encode(source,new Catalog())).items.isEmpty());}
 @Test(expected=IllegalArgumentException.class)public void rejectsChangedConnection()throws Exception{Library.Source first=Library.Source.m3u("Test","https://example.com/one"),second=Library.Source.m3u("Test","https://example.com/two");CatalogSnapshot.decode(second,CatalogSnapshot.encode(first,new Catalog()));}
}
