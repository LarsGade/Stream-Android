package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class CatalogTest {
 @Test public void m3uSectionsRetainOriginalOrderAndGroups() {
  String source="#EXTM3U\n#EXTINF:-1 group-title=\"DK\",Z TV\nhttps://example.com/live/u/p/1.ts\n"
   +"#EXTINF:-1 group-title=\"Action\",Z Film\nhttps://example.com/movie/u/p/2.mp4\n"
   +"#EXTINF:-1 group-title=\"DK\",A TV\nhttps://example.com/live/u/p/3.ts\n"
   +"#EXTINF:-1 group-title=\"Drama\",A Film\nhttps://example.com/movie/u/p/4.mp4\n"
   +"#EXTINF:-1 group-title=\"Serier\",Afsnit\nhttps://example.com/series/u/p/5.mp4\n";
  Catalog catalog=new Catalog(Playlist.parse(source,"https://example.com/list"));
  assertEquals(Arrays.asList("Action","Drama"),catalog.groups(Playlist.Kind.FILM));
  var tv=catalog.filter(Playlist.Kind.TV,null,""); assertEquals("Z TV",tv.get(0).name); assertEquals("A TV",tv.get(1).name);
  assertEquals("A Film",catalog.filter(Playlist.Kind.FILM,"Drama","").get(0).name);
  assertEquals(1,catalog.filter(Playlist.Kind.SERIES,null,"").size());
  assertTrue(catalog.filter(Playlist.Kind.TV,null,"Film").isEmpty());
 }
 @Test public void metadataCanClassifyExtensionlessMoviesAndSeries() {
  assertEquals(Playlist.Kind.FILM,Playlist.inferKind("tvg-type=\"vod\"","Drama","https://example.com/123"));
  assertEquals(Playlist.Kind.SERIES,Playlist.inferKind("","DK | Serier","https://example.com/456"));
  assertEquals(Playlist.Kind.TV,Playlist.inferKind("","DK","https://example.com/789"));
 }
 @Test public void explicitLiveMetadataOverridesFileExtension() {
  assertEquals(Playlist.Kind.TV,Playlist.inferKind("tvg-type=\"live\"","DK","https://example.com/123.mp4"));
 }
 @Test public void extgrpMetadataIsRetained() {
  Catalog catalog=new Catalog(Playlist.parse("#EXTM3U\n#EXTINF:-1,Show\n#EXTGRP:Serier\nhttps://example.com/123.mp4\n","https://example.com/list"));
  assertEquals("Serier",catalog.groups(Playlist.Kind.SERIES).get(0));
 }
 @Test public void serverCategoryOrderIsIndependentOfItemOrder() {
  Catalog catalog=new Catalog(); catalog.addGroup(Playlist.Kind.TV,"Z");catalog.addGroup(Playlist.Kind.TV,"A");
  catalog.add(new Playlist.Channel("First","https://example.com/1","A"));
  catalog.add(new Playlist.Channel("Second","https://example.com/2","Z"));
  assertEquals(Arrays.asList("Z","A"),catalog.groups(Playlist.Kind.TV));
  assertEquals("First",catalog.filter(Playlist.Kind.TV,null,"").get(0).name);
 }
}
