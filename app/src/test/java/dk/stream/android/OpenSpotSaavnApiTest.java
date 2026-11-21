package dk.stream.android;

import org.junit.*;
import static org.junit.Assert.*;
import okhttp3.*;
import okhttp3.mockwebserver.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class OpenSpotSaavnApiTest {
 private MockWebServer server;
 @Before public void setup()throws Exception{server=new MockWebServer();server.start();}
 @After public void close()throws Exception{server.shutdown();}
 private OpenSpotSaavnApi api(){return new OpenSpotSaavnApi(server.url("/api.php").toString(),server.url("/proxy").toString());}
 private static String encode(String value)throws Exception{Cipher cipher=Cipher.getInstance("DES/ECB/PKCS5Padding");cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec("38346591".getBytes(StandardCharsets.UTF_8),"DES"));return okio.ByteString.of(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8))).base64();}
 private JSONObject song()throws Exception{return new JSONObject().put("id","song01").put("title","Rock &amp; Roll").put("image","http://example.com/cover-150x150.jpg").put("more_info",new JSONObject().put("duration","250").put("album","Album").put("320kbps","true").put("encrypted_media_url",encode("https://example.com/audio_96.mp4")).put("artistMap",new JSONObject().put("primary_artists",new JSONArray().put(new JSONObject().put("name","Artist A")).put(new JSONObject().put("name","Artist B")))));}
 private void proxy(JSONObject response)throws Exception{server.enqueue(new MockResponse().setBody(new JSONObject().put("status",200).put("body",response.toString()).toString()));}
 @Test public void searchUsesSamePostTransportAndOneBasedPagesAsOpenSpot()throws Exception{
  proxy(new JSONObject().put("total",80).put("results",new JSONArray().put(song())));OpenSpotSaavnApi.Page result=api().searchMusic("A & B",0);RecordedRequest request=server.takeRequest();assertEquals("POST",request.getMethod());assertEquals("/proxy",request.getPath());JSONObject body=new JSONObject(request.getBody().readUtf8());assertEquals("GET",body.getString("method"));HttpUrl url=HttpUrl.get(body.getString("url"));assertEquals("/api.php",url.encodedPath());assertEquals("search.getResults",url.queryParameter("__call"));assertEquals("1",url.queryParameter("p"));assertEquals("20",url.queryParameter("n"));assertEquals("A & B",url.queryParameter("q"));assertEquals("web6dot0",url.queryParameter("ctx"));assertEquals("4",url.queryParameter("api_version"));assertTrue(body.getJSONObject("headers").getString("User-Agent").contains("Android"));assertTrue(result.hasMore);assertEquals("jiosaavn_song01",result.songs.get(0).id);assertEquals("",result.songs.get(0).stream);
 }
 @Test public void searchReadsNestedResponseAndLastPage()throws Exception{
  proxy(new JSONObject().put("response",new JSONObject().put("songs",new JSONArray().put(song())).put("total",21)));OpenSpotSaavnApi.Page page=api().searchMusic("Artist",1);assertEquals(1,page.songs.size());assertFalse(page.hasMore);JSONObject payload=new JSONObject(server.takeRequest().getBody().readUtf8());assertEquals("2",HttpUrl.get(payload.getString("url")).queryParameter("p"));
 }
 @Test public void detailsGoDirectAndResolveRawMetadataAndMediaAddress()throws Exception{
  server.enqueue(new MockResponse().setBody(new JSONObject().put("songs",new JSONArray().put(song())).toString()));SaavnSong song=api().song("jiosaavn_song01");RecordedRequest request=server.takeRequest();assertEquals("GET",request.getMethod());assertEquals("/api.php",request.getRequestUrl().encodedPath());assertEquals("song.getDetails",request.getRequestUrl().queryParameter("__call"));assertEquals("song01",request.getRequestUrl().queryParameter("pids"));assertEquals("Rock & Roll",song.title);assertEquals("Artist A, Artist B",song.artist);assertEquals("https://example.com/cover-500x500.jpg",song.image);assertEquals("https://example.com/audio_320.mp4",song.stream);assertEquals("Album",song.album);assertEquals(250,song.duration);
 }
 @Test public void doesNotInventHighQualityWhenNotAvailable()throws Exception{
  JSONObject song=song();song.getJSONObject("more_info").put("320kbps","false");server.enqueue(new MockResponse().setBody(new JSONObject().put("songs",new JSONArray().put(song)).toString()));assertEquals("https://example.com/audio_96.mp4",api().song("jiosaavn_song01").stream);
 }
 @Test public void wrongDetailsTrackIsRejectedInsteadOfPlayingAnotherSong()throws Exception{
  server.enqueue(new MockResponse().setBody(new JSONObject().put("songs",new JSONArray().put(song().put("id","different"))).toString()));try{api().song("jiosaavn_song01");fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("valgte sang"));}
 }
 @Test public void proxyUpstreamFailureIsNotShownAsEmptyCatalogue()throws Exception{
  server.enqueue(new MockResponse().setBody("{\"status\":403,\"body\":\"{}\"}"));try{api().searchMusic("Metallica",0);fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("403"));}
 }
 @Test public void missingProxyBodyIsReportedAsConnectionFailure()throws Exception{
  server.enqueue(new MockResponse().setBody("{}"));try{api().searchMusic("test",0);fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("intet svar"));}
 }
 @Test public void malformedRowsAreSkippedAndDuplicatesRemoved()throws Exception{
  proxy(new JSONObject().put("results",new JSONArray().put(song()).put(song()).put(new JSONObject()).put(song().put("id","../bad"))));assertEquals(1,api().searchMusic("test",0).songs.size());
 }
 @Test public void invalidEncodedMediaIsRejected()throws Exception{
  try{OpenSpotSaavnApi.decodeMedia("invalid");fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("ugyldig"));}
  try{OpenSpotSaavnApi.decodeMedia(encode("file:///private/file"));fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("ugyldig"));}
 }
 @Test public void decodedUrlMatchesPublishedFormat()throws Exception{assertEquals("https://example.com/track_96.mp4?x=1",OpenSpotSaavnApi.decodeMedia(encode("https://example.com/track_96.mp4?x=1")));}
 @Test public void connectionSettingsExportBothEndpointsAndPreserveOtherProviders()throws Exception{
  Library library=new Library();library.openSpotApiUrl="https://example.com/api.php";library.openSpotProxyUrl="https://example.com/search-proxy";library.audiusApiUrl="https://example.com/audius/";Library restored=SettingsBackup.decode(new ByteArrayInputStream(SettingsBackup.encode(library)));assertEquals(library.toJson(),restored.toJson());assertEquals("https://example.com/api.php",restored.openSpotApiUrl);
 }
 @Test public void olderSettingsGetOpenSpotConnectionWithoutChangingOldFavorites()throws Exception{
  JSONObject saved=new JSONObject(new Library().toJson());saved.remove("openSpotApiUrl");saved.remove("openSpotProxyUrl");Library restored=Library.fromJson(saved.toString());assertEquals(OpenSpotSaavnApi.DEFAULT,restored.openSpotApiUrl);assertEquals(OpenSpotSaavnApi.PROXY,restored.openSpotProxyUrl);assertFalse(OpenSpotSaavnApi.owns("audius_song01"));assertFalse(OpenSpotSaavnApi.owns("song01"));
 }
 @Test public void endpointCannotIncludeCredentials(){try{OpenSpotSaavnApi.endpoint("https://user:secret@example.com/api.php");fail();}catch(IllegalArgumentException expected){assertTrue(expected.getMessage().contains("uden login"));}}
 private HttpUrl proxiedUrl()throws Exception{RecordedRequest request=server.takeRequest();assertEquals("POST",request.getMethod());return HttpUrl.get(new JSONObject(request.getBody().readUtf8()).getString("url"));}
 @Test public void artistSearchUsesArtistEndpointAndReturnsProfiles()throws Exception{
  proxy(new JSONObject().put("response",new JSONObject().put("artists",new JSONArray().put(new JSONObject().put("id","42").put("name","Artist &amp; Band").put("image","https://example.com/150x150.jpg"))).put("total",25)));
  OpenSpotSaavnApi.EntityPage result=api().searchEntities("Artist",0,OpenSpotSaavnApi.ARTIST);assertEquals("search.getArtistResults",proxiedUrl().queryParameter("__call"));assertEquals(1,result.items.size());assertEquals("42",result.items.get(0).id);assertEquals("Artist & Band",result.items.get(0).name);assertEquals(OpenSpotSaavnApi.ARTIST,result.items.get(0).type);assertTrue(result.hasMore);
 }
 @Test public void albumSearchUsesAlbumEndpointAndReadsLastPage()throws Exception{
  proxy(new JSONObject().put("results",new JSONArray().put(new JSONObject().put("albumid","99").put("title","Album").put("year","2020"))).put("total",21));
  OpenSpotSaavnApi.EntityPage result=api().searchEntities("Album",1,OpenSpotSaavnApi.ALBUM);HttpUrl url=proxiedUrl();assertEquals("search.getAlbumResults",url.queryParameter("__call"));assertEquals("2",url.queryParameter("p"));assertEquals("99",result.items.get(0).id);assertTrue(result.items.get(0).detail.contains("2020"));assertFalse(result.hasMore);
 }
 @Test public void openingArtistUsesProfileIdAndLoadsNextSongPage()throws Exception{
  proxy(new JSONObject().put("topSongs",new JSONObject().put("songs",new JSONArray().put(song())).put("total",120)));
  OpenSpotSaavnApi.Page result=api().collection(new OpenSpotSaavnApi.Entity(OpenSpotSaavnApi.ARTIST,"42","Artist","",""),1);HttpUrl url=proxiedUrl();assertEquals("artist.getArtistMoreSong",url.queryParameter("__call"));assertEquals("42",url.queryParameter("artistId"));assertEquals("2",url.queryParameter("page"));assertEquals("50",url.queryParameter("n_song"));assertEquals(1,result.songs.size());assertTrue(result.hasMore);
 }
 @Test public void openingAlbumLoadsItsSongsWithoutArtistPagination()throws Exception{
  proxy(new JSONObject().put("list",new JSONArray().put(song())).put("song_count",12));OpenSpotSaavnApi.Page result=api().collection(new OpenSpotSaavnApi.Entity(OpenSpotSaavnApi.ALBUM,"99","Album","",""),0);HttpUrl url=proxiedUrl();assertEquals("content.getAlbumDetails",url.queryParameter("__call"));assertEquals("99",url.queryParameter("albumid"));assertNull(url.queryParameter("artistId"));assertEquals(1,result.songs.size());assertFalse(result.hasMore);
 }
 @Test public void brokenCollectionIsNotShownAsEmptyAlbum()throws Exception{
  proxy(new JSONObject().put("error","unavailable"));try{api().collection(new OpenSpotSaavnApi.Entity(OpenSpotSaavnApi.ALBUM,"99","Album","",""),0);fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("ikke hente sangene"));}
 }
}
