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
   "[{\"stream_id\":2,\"name\":\"Z channel\",\"category_id\":\"10\"},{\"stream_id\":1,\"name\":\"A channel\",\"category_id\":\"20\"}]",
   "[{\"category_id\":\"30\",\"category_name\":\"Movies\"}]",
   "[{\"stream_id\":3,\"name\":\"Movie\",\"category_id\":\"30\",\"container_extension\":\"mkv\"}]",
   "[{\"category_id\":\"40\",\"category_name\":\"Shows\"}]",
   "[{\"series_id\":4,\"name\":\"Series\",\"category_id\":\"40\"}]"}) server.enqueue(new MockResponse().setBody(body));
 }
 @Test public void fetchesAllSectionsAndPreservesCategoryAndStreamOrder() throws Exception {
  try(MockWebServer server=new MockWebServer()) {
   catalogResponses(server);
   XtreamClient client=new XtreamClient(server.url("/").toString(),"user+name","p& +");
   Catalog catalog=client.load();
   assertEquals(4,catalog.items.size());
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
