package dk.stream.android;
import org.junit.*;
import static org.junit.Assert.*;
import okhttp3.mockwebserver.*;
import org.json.*;
import java.io.*;
import java.util.*;
public class SaavnApiTest {
 private MockWebServer server;
 @Before public void setup()throws Exception{server=new MockWebServer();server.start();}
 @After public void close()throws Exception{server.shutdown();}
 private String song(){return "{\"id\":\"song01\",\"name\":\"Rock &amp; Roll &#039;Live&#039;\",\"duration\":205,\"album\":{\"name\":\"Album\"},\"artists\":{\"primary\":[{\"name\":\"Artist A\"},{\"name\":\"Artist B\"}]},\"image\":[{\"quality\":\"150x150\",\"url\":\"https://example.com/small.jpg\"},{\"quality\":\"500x500\",\"url\":\"https://example.com/large.jpg\"}],\"downloadUrl\":[{\"quality\":\"320kbps\",\"url\":\"https://example.com/song_320.mp4\"},{\"quality\":\"48kbps\",\"url\":\"https://example.com/song_48.mp4\"}]}";}
 @Test public void searchUsesDocumentedRouteAndEscapesTheQuery()throws Exception{server.enqueue(new MockResponse().setBody("{\"success\":true,\"data\":{\"total\":50,\"results\":["+song()+"]}}"));SaavnApi.Page page=new SaavnApi(server.url("/api").toString()).search("A & B",2);RecordedRequest request=server.takeRequest();assertEquals("/api/search/songs",request.getRequestUrl().encodedPath());assertEquals("A & B",request.getRequestUrl().queryParameter("query"));assertEquals("2",request.getRequestUrl().queryParameter("page"));assertEquals(50,page.total);assertEquals(1,page.songs.size());}
 @Test public void songDetailsReadTheArrayResponse()throws Exception{server.enqueue(new MockResponse().setBody("{\"success\":true,\"data\":["+song()+"]}"));SaavnSong song=new SaavnApi(server.url("/").toString()).song("song01");assertEquals("song01",song.id);assertEquals("/songs/song01",server.takeRequest().getPath());}
 @Test public void metadataAndQualityAreParsedWithoutDependingOnArrayOrder()throws Exception{SaavnSong track=SaavnSong.parse(new JSONObject(song()));assertEquals("Rock & Roll 'Live'",track.title);assertEquals("Artist A, Artist B",track.artist);assertEquals("https://example.com/large.jpg",track.image);assertEquals("https://example.com/song_320.mp4",track.stream);assertEquals(205,track.duration);}
 @Test public void albumReferenceSurvivesCache()throws Exception{SaavnSong song=new SaavnSong("song01","Song","Artist","Album","","",100,"album42");assertEquals("album42",SaavnSong.parse(song.json()).albumId);}
 @Test public void cachedMetadataCanBeReadAgain()throws Exception{SaavnSong track=SaavnSong.parse(new JSONObject(song()));SaavnSong restored=SaavnSong.parse(track.json());assertEquals(track.title,restored.title);assertEquals(track.artist,restored.artist);assertEquals(track.stream,restored.stream);}
 @Test public void searchesSkipInvalidRowsAndDeduplicateSongIds()throws Exception{SaavnApi.Page page=SaavnApi.parseSearch(new JSONObject("{\"data\":{\"results\":[{},"+song()+","+song()+"]}}"));assertEquals(1,page.songs.size());}
 @Test public void searchCanReturnAnEmptyList()throws Exception{assertTrue(SaavnApi.parseSearch(new JSONObject("{\"data\":{\"results\":[]}}" )).songs.isEmpty());}
 @Test public void explicitApiFailuresAreNotShownAsEmptySearches()throws Exception{server.enqueue(new MockResponse().setBody("{\"success\":false,\"data\":{\"results\":[]}}"));try{new SaavnApi(server.url("/").toString()).search("test",0);fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("Musikkilden"));}}
 @Test public void httpErrorsAreVisible()throws Exception{server.enqueue(new MockResponse().setResponseCode(429));try{new SaavnApi(server.url("/").toString()).search("test",0);fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("429"));}}
 @Test public void htmlErrorPagesAreRejected()throws Exception{server.enqueue(new MockResponse().setBody("<html>unavailable</html>"));try{new SaavnApi(server.url("/").toString()).search("test",0);fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("musikdata"));}}
 @Test public void streamClientDoesNotTimeOutAtThirtySeconds(){assertEquals(0,SaavnApi.STREAM.callTimeoutMillis());assertTrue(SaavnApi.HTTP.callTimeoutMillis()>0);}
 @Test public void unsafeMediaSchemesAreIgnored()throws Exception{assertEquals("",SaavnSong.best(new JSONArray("[{\"quality\":\"320kbps\",\"url\":\"file:///private/file\"}]")));}
 @Test(expected=IllegalArgumentException.class) public void apiBaseCannotContainCredentials(){SaavnApi.base("https://user:password@example.com/api");}
 @Test public void musicSourceIsIncludedInPortableSettings()throws Exception{Library library=new Library();library.saavnApiUrl="https://example.com/music-api/";Library restored=SettingsBackup.decode(new ByteArrayInputStream(SettingsBackup.encode(library)));assertEquals(library.saavnApiUrl,restored.saavnApiUrl);}
 @Test public void oldSettingsGetThePublicDefault()throws Exception{JSONObject saved=new JSONObject(new Library().toJson());saved.remove("saavnApiUrl");assertEquals(SaavnApi.DEFAULT,Library.fromJson(saved.toString()).saavnApiUrl);}
 @Test public void savedObsoleteDefaultMigratesToCurrentSource()throws Exception{JSONObject saved=new JSONObject(new Library().toJson());saved.put("saavnApiUrl","https://saavn.dev/api");assertEquals(SaavnApi.DEFAULT,Library.fromJson(saved.toString()).saavnApiUrl);}
 @Test public void customSourcesArePreserved(){assertEquals("https://example.com/api/",SaavnApi.base("https://example.com/api").toString());assertEquals("https://saavn.dev/custom/",SaavnApi.base("https://saavn.dev/custom/").toString());}
 @Test public void dnsFailureHasDanishMessageAndPreservesCause()throws Exception{okhttp3.OkHttpClient client=SaavnApi.HTTP.newBuilder().proxy(java.net.Proxy.NO_PROXY).dns(host->{throw new java.net.UnknownHostException(host);}).build();try{new SaavnApi("https://missing.invalid/api/",client).search("metallica",0);fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("adresse kunne ikke findes"));assertTrue(expected.getCause() instanceof java.net.UnknownHostException);}}
 private void artists(String results){server.enqueue(new MockResponse().setBody("{\"success\":true,\"data\":{\"results\":"+results+"}}"));}
 @Test public void exactArtistUsesArtistCatalogueInsteadOfTributeTitleSearch()throws Exception{
  artists("[{\"id\":\"tribute\",\"name\":\"Artist A Tribute Band\"},{\"id\":\"original\",\"name\":\"Artist A\"}]");
  server.enqueue(new MockResponse().setBody("{\"data\":{\"total\":40,\"songs\":["+song()+"]}}"));
  SaavnApi.Page page=new SaavnApi(server.url("/api/").toString()).searchMusic("  ARTIST   a ",2);
  RecordedRequest lookup=server.takeRequest(),catalogue=server.takeRequest();
  assertEquals("/api/search/artists",lookup.getRequestUrl().encodedPath());
  assertEquals("/api/artists/original/songs",catalogue.getRequestUrl().encodedPath());
  assertEquals("2",catalogue.getRequestUrl().queryParameter("page"));assertEquals("desc",catalogue.getRequestUrl().queryParameter("sortOrder"));
  assertEquals("Artist A",page.artist);assertEquals(40,page.total);assertEquals(1,page.songs.size());assertEquals(2,server.getRequestCount());
 }
 @Test public void emptyArtistCatalogueDoesNotSubstituteKaraokeResults()throws Exception{
  artists("[{\"id\":\"original\",\"name\":\"Metallica\"}]");server.enqueue(new MockResponse().setBody("{\"data\":{\"total\":0,\"songs\":[]}}"));
  SaavnApi.Page page=new SaavnApi(server.url("/api/").toString()).searchMusic("Metallica",0);
  assertTrue(page.songs.isEmpty());assertTrue(page.summary(0).contains("Metallica"));assertTrue(page.summary(0).contains("kataloget"));assertEquals(2,server.getRequestCount());
 }
 @Test public void songTitleSearchDoesNotChoosePartialArtistMatch()throws Exception{
  artists("[{\"id\":\"tribute\",\"name\":\"Rock Tribute Band\"}]");server.enqueue(new MockResponse().setBody("{\"data\":{\"results\":["+song()+"]}}"));
  SaavnApi.Page page=new SaavnApi(server.url("/api/").toString()).searchMusic("Rock",0);
  server.takeRequest();assertEquals("/api/search/songs",server.takeRequest().getRequestUrl().encodedPath());assertEquals("",page.artist);assertEquals(1,page.songs.size());
 }
 @Test public void artistFailureIsNotReportedAsMissingOriginals()throws Exception{
  artists("[{\"id\":\"original\",\"name\":\"Metallica\"}]");server.enqueue(new MockResponse().setResponseCode(503));
  try{new SaavnApi(server.url("/api/").toString()).searchMusic("Metallica",0);fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("503"));}assertEquals(2,server.getRequestCount());
 }
 @Test public void malformedArtistResultsDoNotSilentlyFallBackToCovers()throws Exception{
  server.enqueue(new MockResponse().setBody("{\"data\":{}}"));try{new SaavnApi(server.url("/api/").toString()).searchMusic("Metallica",0);fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("kunstnerdata"));}assertEquals(1,server.getRequestCount());
 }
}
