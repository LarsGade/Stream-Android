package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class ExternalPlaybackTypeTest {
 @Test public void signedHlsUrlGetsHlsPlayers(){assertEquals("application/vnd.apple.mpegurl",ExternalPlaybackType.mime("https://tv.example/LIVE.M3U8?token=a.mp3","video/*"));}
 @Test public void extensionlessRadioStaysAudio(){assertEquals("audio/*",ExternalPlaybackType.mime("https://radio.example/live","audio/*"));}
 @Test public void pcloudOpaqueUriPreservesActualFileType(){assertEquals("audio/mpeg",ExternalPlaybackType.mime("content://dk.stream.android.pcloud/account?url=track.mp4","audio/mpeg"));}
 @Test public void movieAndAudioFormatsReachSuitablePlayers(){assertEquals("video/x-matroska",ExternalPlaybackType.mime("https://tv.example/film.mkv",null));assertEquals("audio/flac",ExternalPlaybackType.mime("content://files/music.flac","audio/*"));}
 @Test public void opaqueDocumentsUseActualFileName(){assertEquals("audio/mp4",ExternalPlaybackType.mime("content://provider/1234","Book.m4b","audio/mpeg"));assertEquals("video/quicktime",ExternalPlaybackType.mime("content://provider/1234","Movie.mov","application/octet-stream"));}
 @Test public void hlsCanReachVideoPlayersWithoutApplicationMimeRegistration(){assertTrue(ExternalPlaybackType.candidates("application/vnd.apple.mpegurl","video").contains("video/*"));assertFalse(ExternalPlaybackType.candidates("application/vnd.apple.mpegurl","video").contains("audio/x-mpegurl"));assertTrue(ExternalPlaybackType.candidates("application/vnd.apple.mpegurl","audio").contains("audio/*"));}
 @Test public void mp4AudioStaysAudio(){assertEquals("audio/mp4",ExternalPlaybackType.mime("https://example.com/sound.mp4","audio/mp4"));}
 @Test public void extensionlessVideoAndAudioHaveSeparatePlayerLists(){assertEquals(java.util.Arrays.asList("audio/mpeg","audio/*"),ExternalPlaybackType.candidates("audio/mpeg","audio"));assertEquals(java.util.Arrays.asList("video/mp4","video/*"),ExternalPlaybackType.candidates("video/mp4","video"));}
}
