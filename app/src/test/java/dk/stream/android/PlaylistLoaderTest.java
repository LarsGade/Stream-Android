package dk.stream.android;

import org.junit.Test;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import static org.junit.Assert.*;

public class PlaylistLoaderTest {
 @Test public void acceptsPastedUrlsAndEscapesSpaces() {
  HttpUrl url=Playlist.httpUrl(" \"https://example.com/get.php?username=a b&amp;password=x%2By\" ");
  assertEquals("a b",url.queryParameter("username"));
  assertEquals("x+y",url.queryParameter("password"));
  assertEquals("example.com",Playlist.httpUrl("example.com/list.m3u").host());
  assertEquals(8080,Playlist.httpUrl("example.com:8080/list.m3u").port());
 }
 @Test public void xtreamEncodesCredentialsWithoutChangingTheirMeaning() {
  HttpUrl url=HttpUrl.get(Playlist.xtreamUrl("https://example.com:8443/player_api.php?old=1","user+name","p&? #+æ%"));
  assertEquals("/get.php",url.encodedPath()); assertEquals(8443,url.port());
  assertEquals("user+name",url.queryParameter("username")); assertEquals("p&? #+æ%",url.queryParameter("password"));
  assertEquals("m3u_plus",url.queryParameter("type")); assertEquals("m3u8",url.queryParameter("output")); assertNull(url.queryParameter("old"));
 }
 @Test(expected=IllegalArgumentException.class) public void emptyXtreamLoginRejected() { Playlist.xtreamUrl("https://example.com","","password"); }
 @Test public void followsRedirectAndUsesFinalUrlForRelativeChannels() throws Exception {
  try(MockWebServer server=new MockWebServer()) {
   server.enqueue(new MockResponse().setResponseCode(302).addHeader("Location","/lists/tv.m3u"));
   server.enqueue(new MockResponse().setBody("#EXTM3U\n#EXTINF:-1,News\nlive.m3u8\n"));
   var channels=new PlaylistLoader().load(server.url("/get.php").toString());
   assertEquals(1,channels.size()); assertEquals(server.url("/lists/live.m3u8").toString(),channels.get(0).url);
   assertEquals("Stream-Android/1.1",server.takeRequest().getHeader("User-Agent"));
  }
 }
 @Test public void readsGzipPlaylist() throws Exception {
  ByteArrayOutputStream compressed=new ByteArrayOutputStream();
  try(GZIPOutputStream gzip=new GZIPOutputStream(compressed)) { gzip.write("#EXTM3U\r#EXTINF:-1,News\rhttps://example.com/live.m3u8\r".getBytes(StandardCharsets.UTF_8)); }
  try(MockWebServer server=new MockWebServer()) {
   server.enqueue(new MockResponse().addHeader("Content-Encoding","gzip").setBody(new Buffer().write(compressed.toByteArray())));
   assertEquals("News",new PlaylistLoader().load(server.url("/get.php").toString()).get(0).name);
  }
 }
 @Test public void readsUtf16Playlist() throws Exception {
  try(MockWebServer server=new MockWebServer()) {
   server.enqueue(new MockResponse().setBody(new Buffer().write("#EXTM3U\n#EXTINF:-1,Æble\nhttps://example.com/live\n".getBytes(StandardCharsets.UTF_16))));
   assertEquals("Æble",new PlaylistLoader().load(server.url("/get.php").toString()).get(0).name);
  }
 }
 @Test public void distinguishesAccessDenialFromInvalidPlaylist() throws Exception {
  try(MockWebServer server=new MockWebServer()) {
   server.enqueue(new MockResponse().setResponseCode(403));
   server.enqueue(new MockResponse().setBody("<html>Login required</html>"));
   for(int expected:new int[]{403,0}) {
    try { new PlaylistLoader().load(server.url("/get.php").toString()); fail("Expected failure"); }
    catch(PlaylistLoader.Failure failure) { assertEquals(expected,failure.status); assertFalse(failure.getMessage().contains("get.php")); }
   }
  }
 }
 @Test public void rejectsPlainTextProviderErrors() throws Exception {
  try(MockWebServer server=new MockWebServer()) {
   server.enqueue(new MockResponse().setBody("Invalid username or password"));
   try { new PlaylistLoader().load(server.url("/get.php").toString()); fail("Expected failure"); }
   catch(PlaylistLoader.Failure failure) { assertEquals(0,failure.status); }
  }
 }
 @Test public void handlesLargeListsPreviouslyRejectedAtTenMegabytes() throws Exception {
  try(MockWebServer server=new MockWebServer()) {
   String text="#EXTM3U\n#"+"x".repeat(10*1024*1024)+"\n#EXTINF:-1,News\nhttps://example.com/live\n";
   server.enqueue(new MockResponse().setBody(text));
   assertEquals(1,new PlaylistLoader().load(server.url("/get.php").toString()).size());
  }
 }
}
