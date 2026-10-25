package dk.stream.android;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import okhttp3.mockwebserver.*;
import okio.Buffer;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class EpgRepositoryTest {
 private MockWebServer server;
 private EpgRepository repository;
 private final long now=EpgData.time("20261025120000Z");
 private String xml="<tv><channel id='dr1'><display-name>DR 1</display-name></channel><programme channel='dr1' start='20261025110000Z' stop='20261025130000Z'><title>Current</title></programme></tv>";
 @Before public void start() throws Exception {server=new MockWebServer();server.start();repository=new EpgRepository(RuntimeEnvironment.getApplication());}
 @After public void stop() throws Exception {server.shutdown();}
 @Test public void fetchesGzipWithRedirectAndRetainsCacheOnFailure() throws Exception {
  ByteArrayOutputStream compressed=new ByteArrayOutputStream();try(GZIPOutputStream gzip=new GZIPOutputStream(compressed)) {gzip.write(xml.getBytes(StandardCharsets.UTF_8));}
  server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location","/guide.gz"));server.enqueue(new MockResponse().setHeader("Content-Type","application/gzip").setBody(new Buffer().write(compressed.toByteArray())));
  Library.Guide guide=Library.Guide.create("Guide",server.url("/redirect").toString());
  var channel=new Playlist.Channel("DR1","https://example.com/live","");
  assertEquals("Current",repository.fetch(guide,now).current(channel,now).title);
  server.enqueue(new MockResponse().setResponseCode(503));
  try {repository.fetch(guide,now);fail("Expected network error");} catch(IOException expected) { }
  assertEquals("Current",repository.cached(guide).current(channel,now).title);
  assertNull(repository.cached(new Library.Guide(guide.id,guide.name,server.url("/changed").toString())));
  repository.remove(guide);assertNull(repository.cached(guide));
 }
}
