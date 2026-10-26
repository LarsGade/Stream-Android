package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.*;
import java.util.*;
public class XtreamClientTest {
 private void catalogResponses(MockWebServer server) {
  for(String body:new String[]{
   "{\"user_info\":{\"auth\":1,\"status\":\"Active\"}}",
   "[{\"category_id\":\"20\",\"category_name\":\"Z TV\"},{\"category_id\":\"10\",\"category_name\":\"A TV\"}]",
   "[{\"stream_id\":2,\"name\":\"Z channel\",\"stream_icon\":\"/logos/tv.png\",\"category_id\":\"10\"},{\"stream_id\":1,\"name\":\"A channel\",\"category_id\":\"20\"}]",
   "[{\"category_id\":\"30\",\"category_name\":\"Movies\"}]",
   "[{\"stream_id\":3,\"name\":\"Movie\",\"stream_icon\":\"/logos/movie.jpg\",\"category_id\":\"30\",\"container_extension\":\"mkv\"}]",
   "[{\"category_id\":\"40\",\"category_name\":\"Shows\"}]",
   "[{\"series_id\":4,\"name\":\"Series\",\"cover\":\"/logos/series.jpg\",\"category_id\":\"40\"}]"}) server.enqueue(new MockResponse().setBody(body));
 }
 @Test public void largeCatalogSkipsUnusedMetadataAndRetainsServerOrder() throws Exception {
  try(MockWebServer server=new MockWebServer()) {
   server.enqueue(new MockResponse().setBody("{\"user_info\":{\"auth\":1}}"));
   server.enqueue(new MockResponse().setBody("[]"));
   StringBuilder body=new StringBuilder("[");
   for(int i=1;i<=20000;i++) {
    if(i>1) body.append(',');
    body.append("{\"stream_id\":").append(i).append(",\"name\":\"Channel ").append(i)
     .append("\",\"stream_icon\":null,\"unused_metadata\":{\"details\":[1,2,{\"nested\":true}]}}");
   }
   server.enqueue(new MockResponse().setBody(body.append(']').toString()));
   for(int i=0;i<4;i++) server.enqueue(new MockResponse().setBody("null"));
   Catalog catalog=new XtreamClient(server.url("/").toString(),"u","p").load();
   assertEquals(20000,catalog.items.size());assertEquals("Channel 1",catalog.items.get(0).name);
   assertEquals("Channel 20000",catalog.items.get(19999).name);assertEquals("",catalog.items.get(0).logoUrl);
  }
 }
 @Test public void catalogCanExceedOneHundredThousandItems() throws Exception {
  try(MockWebServer server=new MockWebServer()) {
   server.enqueue(new MockResponse().setBody("{\"user_info\":{\"auth\":1}}"));
   server.enqueue(new MockResponse().setBody("[]"));
   StringBuilder body=new StringBuilder("[");
   for(int i=1;i<=100001;i++) {if(i>1) body.append(',');body.append("{\"stream_id\":").append(i).append('}');}
   server.enqueue(new MockResponse().setBody(body.append(']').toString()));
   for(int i=0;i<4;i++) server.enqueue(new MockResponse().setBody("[]"));
   Catalog catalog=new XtreamClient(server.url("/").toString(),"u","p").load();
   assertEquals(100001,catalog.items.size());
   assertTrue(catalog.items.get(100000).url.endsWith("/100001.m3u8"));
  }
 }
 @Test public void catalogResponseCanExceedSixtyFourMiB() throws Exception {
  try(MockWebServer server=new MockWebServer()) {
   server.enqueue(new MockResponse().setBody("{\"user_info\":{\"auth\":1}}"));
   server.enqueue(new MockResponse().setBody("[]"));
   // Compress on the server side to exercise a >64 MiB decoded response without a large test fixture.
   java.io.ByteArrayOutputStream compressed=new java.io.ByteArrayOutputStream();
   try(java.util.zip.GZIPOutputStream gzip=new java.util.zip.GZIPOutputStream(compressed)) {
    gzip.write("[{\"stream_id\":1,\"name\":\"Large source\",\"unused\":\"".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    byte[] block=new byte[1024];Arrays.fill(block,(byte)'x');
    for(int i=0;i<65*1024;i++) gzip.write(block);
    gzip.write("\"}]".getBytes(java.nio.charset.StandardCharsets.UTF_8));
   }
   server.enqueue(new MockResponse().setHeader("Content-Encoding","gzip").setBody(new okio.Buffer().write(compressed.toByteArray())));
   for(int i=0;i<4;i++) server.enqueue(new MockResponse().setBody("[]"));
   Catalog catalog=new XtreamClient(server.url("/").toString(),"u","p").load();
   assertEquals(1,catalog.items.size());assertEquals("Large source",catalog.items.get(0).name);
  }
 }
 @Test public void malformedCatalogReturnsAnError() throws Exception {
  try(MockWebServer server=new MockWebServer()) {
   server.enqueue(new MockResponse().setBody("{\"user_info\":{\"auth\":1}}"));
   server.enqueue(new MockResponse().setBody("[]"));
   server.enqueue(new MockResponse().setBody("[{\"stream_id\":1},"));
   try {new XtreamClient(server.url("/").toString(),"u","p").load();fail("Expected parse error");}
   catch(java.io.IOException expected) { }
  }
 }
 @Test public void fetchesAllSectionsAndPreservesCategoryAndStreamOrder() throws Exception {
  try(MockWebServer server=new MockWebServer()) {
   catalogResponses(server);
   XtreamClient client=new XtreamClient(server.url("/").toString(),"user+name","p& +");
   Catalog catalog=client.load();
   assertEquals(4,catalog.items.size());
   assertEquals(server.url("/logos/tv.png").toString(),catalog.items.get(0).logoUrl);
   assertEquals(server.url("/logos/movie.jpg").toString(),catalog.items.get(2).logoUrl);
   assertEquals(server.url("/logos/series.jpg").toString(),catalog.items.get(3).logoUrl);
   assertEquals(Arrays.asList("Z TV","A TV"),catalog.groups(Playlist.Kind.TV));
   assertEquals("Z channel",catalog.filter(Playlist.Kind.TV,null,"").get(0).name);
   var movie=catalog.filter(Playlist.Kind.FILM,null,"").get(0);
   assertTrue(movie.url.endsWith("/3.mkv"));
   HttpUrl movieUrl=HttpUrl.get(movie.url);
   assertEquals(Arrays.asList("movie","user+name","p& +","3.mkv"),movieUrl.pathSegments());
   var series=catalog.filter(Playlist.Kind.SERIES,null,"").get(0); assertEquals("4",series.seriesId); assertEquals("",series.url);
   String[] actions={null,"get_live_categories","get_live_streams","get_vod_categories","get_vod_streams","get_series_categories","get_series"};
   for(String action:actions) {
    HttpUrl request=server.takeRequest().getRequestUrl();
    assertEquals("/player_api.php",request.encodedPath()); assertEquals(action,request.queryParameter("action"));
    assertEquals("user+name",request.queryParameter("username"));assertEquals("p& +",request.queryParameter("password"));
   }
  }
 }
 @Test public void opensSeasonsAndEpisodesInServerOrder() throws Exception {
  try(MockWebServer server=new MockWebServer()) {
   server.enqueue(new MockResponse().setBody("{\"seasons\":[{\"season_number\":2},{\"season_number\":1}],\"episodes\":{\"1\":[{\"id\":12,\"episode_num\":1,\"title\":\"Earlier season\"}],\"2\":[{\"id\":22,\"episode_num\":2,\"title\":\"First\",\"container_extension\":\"mp4\"},{\"id\":21,\"episode_num\":1,\"title\":\"Second\",\"container_extension\":\"mkv\"}]}}"));
   var client=new XtreamClient(server.url("/sub/").toString(),"u","p");
   var series=new Playlist.Channel("Show","","Drama",Playlist.Kind.SERIES,"99");
   var episodes=client.episodes(series);
   assertEquals(3,episodes.size()); assertTrue(episodes.get(0).name.contains("S2 · E2"));
   assertTrue(episodes.get(0).url.endsWith("/series/u/p/22.mp4"));assertTrue(episodes.get(1).url.endsWith("21.mkv"));
   assertTrue(episodes.get(2).name.contains("S1 · E1"));assertNull(episodes.get(0).seriesId);
   var request=server.takeRequest().getRequestUrl();assertEquals("99",request.queryParameter("series_id"));assertEquals("/sub/player_api.php",request.encodedPath());
  }
 }
 @Test public void badCredentialsStopBeforeCatalogRequests() throws Exception {
  try(MockWebServer server=new MockWebServer()) {
   server.enqueue(new MockResponse().setBody("{\"user_info\":{\"auth\":0}}"));
   try { new XtreamClient(server.url("/").toString(),"u","p").load();fail("Expected auth failure"); }
   catch(PlaylistLoader.Failure e) {assertEquals(401,e.status);}
   assertEquals(1,server.getRequestCount());
  }
 }
 @Test public void emptyFilmAndSeriesSectionsAreValid() throws Exception {
  try(MockWebServer server=new MockWebServer()) {
   server.enqueue(new MockResponse().setBody("{\"user_info\":{\"auth\":1}}"));
   server.enqueue(new MockResponse().setBody("[]"));
   server.enqueue(new MockResponse().setBody("[{\"stream_id\":7,\"name\":\"TV\"}]"));
   for(int i=0;i<4;i++) server.enqueue(new MockResponse().setBody("[]"));
   assertEquals(1,new XtreamClient(server.url("/").toString(),"u","p").load().items.size());
  }
 }
}
