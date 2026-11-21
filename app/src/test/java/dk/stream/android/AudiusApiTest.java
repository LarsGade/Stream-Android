package dk.stream.android;

import org.junit.*;
import static org.junit.Assert.*;
import okhttp3.*;
import okhttp3.mockwebserver.*;
import org.json.*;
import java.io.*;
import java.net.*;

public class AudiusApiTest {
 private MockWebServer server;
 @Before public void setup()throws Exception{server=new MockWebServer();server.start();}
 @After public void close()throws Exception{server.shutdown();}
 private JSONObject track(String id)throws Exception{return new JSONObject().put("id",id).put("title","Original & Live").put("duration",123).put("is_streamable",true).put("is_stream_gated",false).put("user",new JSONObject().put("name","Artist")).put("artwork",new JSONObject().put("150x150","https://example.com/small.jpg").put("1000x1000","https://example.com/cover.jpg"));}
 private void enqueue(Object data)throws Exception{server.enqueue(new MockResponse().setBody(new JSONObject().put("data",data).toString()));}
 @Test public void searchUsesAudiusRouteQueryAndOffsets()throws Exception{
  enqueue(new JSONArray().put(track("D7KyD")));AudiusApi.Page page=new AudiusApi(server.url("/v1/").toString()).searchMusic("A & B",2);RecordedRequest request=server.takeRequest();
  assertEquals("/v1/tracks/search",request.getRequestUrl().encodedPath());assertEquals("A & B",request.getRequestUrl().queryParameter("query"));assertEquals("40",request.getRequestUrl().queryParameter("offset"));assertEquals("Stream",request.getRequestUrl().queryParameter("app_name"));
  assertEquals(1,page.songs.size());SaavnSong song=page.songs.get(0);assertEquals("audius_D7KyD",song.id);assertEquals("Artist",song.artist);assertEquals("https://example.com/cover.jpg",song.image);assertEquals(123,song.duration);
  HttpUrl stream=HttpUrl.get(song.stream);assertEquals("/v1/tracks/D7KyD/stream",stream.encodedPath());assertEquals("Stream",stream.queryParameter("app_name"));assertFalse(page.hasMore);
 }
 @Test public void excludesPaidBlockedDeletedAndUnavailableTracks()throws Exception{
  JSONArray items=new JSONArray().put(track("free")).put(track("paid").put("is_stream_gated",true)).put(track("blocked").put("is_streamable",false)).put(track("deleted").put("is_delete",true)).put(track("hidden").put("is_unlisted",true)).put(new JSONObject().put("id","unknown").put("title","Unknown"));
  enqueue(items);AudiusApi.Page page=new AudiusApi(server.url("/").toString()).searchMusic("test",0);assertEquals(1,page.songs.size());assertEquals("audius_free",page.songs.get(0).id);
 }
 @Test public void paginationUsesRawCountWhenSomeResultsAreUnavailable()throws Exception{
  JSONArray items=new JSONArray();for(int i=0;i<20;i++)items.put(track("item"+i).put("is_stream_gated",i>0));enqueue(items);AudiusApi.Page page=new AudiusApi(server.url("/").toString()).searchMusic("test",0);assertTrue(page.hasMore);assertEquals(1,page.songs.size());
 }
 @Test public void refreshUsesOriginalIdAndPreservesNamespace()throws Exception{
  enqueue(track("D7KyD"));SaavnSong song=new AudiusApi(server.url("/v1/").toString()).song("audius_D7KyD");assertEquals("/v1/tracks/D7KyD",server.takeRequest().getRequestUrl().encodedPath());assertEquals("audius_D7KyD",song.id);assertFalse(AudiusApi.owns("D7KyD"));
 }
 @Test public void refreshRejectsTrackThatBecameGated()throws Exception{
  enqueue(track("D7KyD").put("is_stream_gated",true));try{new AudiusApi(server.url("/").toString()).song("audius_D7KyD");fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("ikke frit tilgængeligt"));}
 }
 @Test public void malformedRowsAreSkippedAndDuplicatesRemoved()throws Exception{
  enqueue(new JSONArray().put(track("same")).put(new JSONObject()).put(track("same")).put(track("../bad")));assertEquals(1,new AudiusApi(server.url("/").toString()).searchMusic("test",0).songs.size());
 }
 @Test public void metadataSurvivesExistingCacheFormat()throws Exception{
  enqueue(track("D7KyD"));SaavnSong song=new AudiusApi(server.url("/").toString()).song("audius_D7KyD"),cached=SaavnSong.parse(song.json());assertEquals(song.id,cached.id);assertEquals(song.title,cached.title);assertEquals(song.stream,cached.stream);
 }
 @Test public void missingResultsAreNotTreatedAsSuccessfulEmptySearch()throws Exception{
  server.enqueue(new MockResponse().setBody("{}"));try{new AudiusApi(server.url("/").toString()).searchMusic("test",0);fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("ugyldige musikdata"));}
 }
 @Test public void rateLimitHasDanishError()throws Exception{
  server.enqueue(new MockResponse().setResponseCode(429));try{new AudiusApi(server.url("/").toString()).searchMusic("test",0);fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("grænsen"));}
 }
 @Test public void dnsErrorIsLocalized()throws Exception{
  OkHttpClient client=SaavnApi.HTTP.newBuilder().proxy(Proxy.NO_PROXY).dns(host->{throw new UnknownHostException(host);}).build();try{new AudiusApi("https://missing.invalid/",client).searchMusic("test",0);fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("Audius-adressen"));assertTrue(expected.getCause() instanceof UnknownHostException);}
 }
 @Test public void portableSettingsIncludeAudiusAndKeepLegacySource()throws Exception{
  Library library=new Library();library.audiusApiUrl="https://example.com/audius/";library.saavnApiUrl="https://example.com/saavn/";Library restored=SettingsBackup.decode(new ByteArrayInputStream(SettingsBackup.encode(library)));assertEquals(library.audiusApiUrl,restored.audiusApiUrl);assertEquals(library.saavnApiUrl,restored.saavnApiUrl);
 }
 @Test public void olderSettingsSwitchSearchToAudiusWithoutRemovingOldSource()throws Exception{
  JSONObject saved=new JSONObject(new Library().toJson());saved.remove("audiusApiUrl");saved.put("saavnApiUrl","https://example.com/saavn/");Library loaded=Library.fromJson(saved.toString());assertEquals(AudiusApi.DEFAULT,loaded.audiusApiUrl);assertEquals("https://example.com/saavn/",loaded.saavnApiUrl);
 }
}
