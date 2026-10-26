package dk.stream.android;
import java.util.*;
import okhttp3.mockwebserver.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class RadioBrowserTest {
 @Test public void fetchesDanishWorkingHttpStationsAndDeduplicatesInServerOrder() throws Exception {
  try(MockWebServer server=new MockWebServer()) {
   server.enqueue(new MockResponse().setBody("["+
    "{\"stationuuid\":\"a\",\"name\":\"P1\",\"countrycode\":\"DK\",\"lastcheckok\":1,\"hls\":1,\"url_resolved\":\"https://example.com/p1\",\"favicon\":\"https://example.com/p1.png\"},"+
    "{\"stationuuid\":\"a\",\"name\":\"Duplicate\",\"countrycode\":\"DK\",\"url\":\"https://example.com/duplicate\"},"+
    "{\"stationuuid\":\"b\",\"name\":\"Swedish\",\"countrycode\":\"SE\",\"url\":\"https://example.com/se\"},"+
    "{\"stationuuid\":\"c\",\"name\":\"Broken\",\"countrycode\":\"DK\",\"lastcheckok\":0,\"url\":\"https://example.com/broken\"},"+
    "{\"stationuuid\":\"d\",\"name\":\"Invalid\",\"countrycode\":\"DK\",\"url\":\"file:///test\"},"+
    "{\"stationuuid\":\"e\",\"name\":\"Nova\",\"countrycode\":\"dk\",\"url_resolved\":null,\"url\":\"http://example.com/nova\"}]"));
   List<RadioStation> stations=new RadioBrowser(Arrays.asList(server.url("/"))).loadDanish();
   assertEquals(2,stations.size());assertTrue(stations.get(0).hls);assertEquals("a",stations.get(0).id);assertEquals("Nova",stations.get(1).name);assertEquals("http://example.com/nova",stations.get(1).url);
   RecordedRequest request=server.takeRequest();assertEquals("/json/stations/bycountrycodeexact/DK",request.getRequestUrl().encodedPath());assertEquals("true",request.getRequestUrl().queryParameter("hidebroken"));assertNotNull(request.getHeader("User-Agent"));
  }
 }
 @Test public void fallsBackToAnotherMirrorAfterBadResponse() throws Exception {
  try(MockWebServer first=new MockWebServer();MockWebServer second=new MockWebServer()) {
   first.enqueue(new MockResponse().setResponseCode(503));second.enqueue(new MockResponse().setBody("[]"));
   assertTrue(new RadioBrowser(Arrays.asList(first.url("/"),second.url("/"))).loadDanish().isEmpty());assertEquals(1,first.getRequestCount());assertEquals(1,second.getRequestCount());
  }
 }
 @Test public void invalidJsonProducesAnError() throws Exception {
  try(MockWebServer server=new MockWebServer()) {server.enqueue(new MockResponse().setBody("<html>unavailable</html>"));try {new RadioBrowser(Arrays.asList(server.url("/"))).loadDanish();fail();} catch(java.io.IOException expected) { }}
 }
}
