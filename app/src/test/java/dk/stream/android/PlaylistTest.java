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
 @Test public void readsAndResolvesChannelLogos() {
  var items=Playlist.parse("#EXTM3U\n#EXTINF:-1 tvg-logo=\"../logos/dr.png?x=1&amp;y=2\",DR\nhttps://example.com/live\n", "https://example.com/lists/tv.m3u");
  assertEquals("https://example.com/logos/dr.png?x=1&y=2",items.get(0).logoUrl);
 }
 @Test public void supportsSingleQuotedLogoAndRejectsUnsafeLogoUrls() {
  var base=Playlist.httpUrl("https://example.com/list");
  assertEquals("https://example.com/logo.png",Playlist.logo("tvg-logo='/logo.png'",base));
  assertEquals("",Playlist.logo("tvg-logo=\"file:///secret\"",base));
  assertEquals("",Playlist.logo("tvg-logo=\"\"",base));
 }
 @Test public void readsUnquotedM3uLogoAndEncodedCdnAddress() {
  var items=Playlist.parse("#EXTM3U\n#EXTINF:-1 tvg-logo=cdn.example.org/logo.png,DR\nhttps://example.com/live\n", "https://example.com/list");
  assertEquals("https://cdn.example.org/logo.png",items.get(0).logoUrl);
  assertEquals("https://cdn.example.org/a.png",LogoUrls.resolve("https%3A%2F%2Fcdn.example.org%2Fa.png",Playlist.httpUrl("https://example.com/list")));
 }
 @Test public void logoCommaInSingleQuotesDoesNotTruncateMetadata() {
  var items=Playlist.parse("#EXTM3U\n#EXTINF:-1 tvg-logo='https://example.com/logo?size=64,64',DR\nhttps://example.com/live\n", "https://example.com/list");
  assertEquals("https://example.com/logo?size=64,64",items.get(0).logoUrl);assertEquals("DR",items.get(0).name);
 }
}
