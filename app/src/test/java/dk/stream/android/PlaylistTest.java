package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaylistTest {
 @Test public void parsesMetadataAndRelativeLinks() {
  var list=Playlist.parse("\uFEFF#EXTM3U\r\n#EXTINF:-1 group-title=\"News, DK\",DR 1\r\n../live.m3u8\n", "https://example.com/lists/tv.m3u");
  assertEquals(1,list.size()); assertEquals("DR 1",list.get(0).name); assertEquals("News, DK",list.get(0).group); assertEquals("https://example.com/live.m3u8",list.get(0).url);
 }
 @Test public void rejectsUnsafeProtocolsAndResetsMetadata() {
  var list=Playlist.parse("#EXTINF:-1,Invalid\nfile:///etc/passwd\nhttps://example.com/tv\n", "https://example.com/list");
  assertEquals(1,list.size()); assertEquals("Kanal 1",list.get(0).name);
 }
 @Test(expected=IllegalArgumentException.class) public void rejectsInvalidSource(){Playlist.httpUri("javascript:bad");}
 @Test public void acceptsSpacesInStreamUrls() {
  var list=Playlist.parse("#EXTM3U\n#EXTINF:-1,Name\nhttps://example.com/my channel.m3u8\n", "https://example.com/list");
  assertEquals("https://example.com/my%20channel.m3u8",list.get(0).url);
 }
 @Test public void emptyList(){assertTrue(Playlist.parse("#EXTM3U\n", "https://example.com").isEmpty());}
}
