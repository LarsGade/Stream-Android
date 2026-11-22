package dk.stream.android;
import org.junit.*;
import static org.junit.Assert.*;
import okhttp3.*;
import okhttp3.mockwebserver.*;
import java.io.*;
public class CastMusicStreamTest {
 private MockWebServer server;private final OkHttpClient client=new OkHttpClient();
 @Before public void start()throws Exception{server=new MockWebServer();server.start();}
 @After public void stop()throws Exception{server.shutdown();}
 @Test public void readsTrackStart()throws Exception{server.enqueue(new MockResponse().setBody("music"));try(InputStream input=CastMusicStream.open(client,server.url("/song.mp4"),0)){assertEquals('m',input.read());}assertEquals("identity",server.takeRequest().getHeader("Accept-Encoding"));}
 @Test public void seeksUsingUpstreamByteRange()throws Exception{server.enqueue(new MockResponse().setResponseCode(206).setHeader("Content-Range","bytes 3-5/6").setBody("def"));try(InputStream input=CastMusicStream.open(client,server.url("/song.mp4"),3)){assertEquals('d',input.read());}assertEquals("bytes=3-",server.takeRequest().getHeader("Range"));}
 @Test public void handlesSourceIgnoringRange()throws Exception{server.enqueue(new MockResponse().setBody("abcdef"));try(InputStream input=CastMusicStream.open(client,server.url("/song.mp4"),3)){assertEquals('d',input.read());}}
 @Test(expected=IOException.class) public void rejectsWrongRange()throws Exception{server.enqueue(new MockResponse().setResponseCode(206).setHeader("Content-Range","bytes 0-2/6").setBody("abc"));CastMusicStream.open(client,server.url("/song.mp4"),3);}
 @Test(expected=IOException.class) public void rejectsUnavailableTrack()throws Exception{server.enqueue(new MockResponse().setResponseCode(403));CastMusicStream.open(client,server.url("/song.mp4"),0);}
}
