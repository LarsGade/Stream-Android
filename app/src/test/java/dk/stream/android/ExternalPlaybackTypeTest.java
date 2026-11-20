package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class ExternalPlaybackTypeTest {
 @Test public void signedHlsUrlGetsHlsPlayers(){assertEquals("application/vnd.apple.mpegurl",ExternalPlaybackType.mime("https://tv.example/LIVE.M3U8?token=a.mp3","video/*"));}
 @Test public void extensionlessRadioStaysAudio(){assertEquals("audio/*",ExternalPlaybackType.mime("https://radio.example/live","audio/*"));}
 @Test public void pcloudOpaqueUriPreservesActualFileType(){assertEquals("audio/mpeg",ExternalPlaybackType.mime("content://dk.stream.android.pcloud/account?url=track.mp4","audio/mpeg"));}
 @Test public void movieAndAudioFormatsReachSuitablePlayers(){assertEquals("video/x-matroska",ExternalPlaybackType.mime("https://tv.example/film.mkv",null));assertEquals("audio/flac",ExternalPlaybackType.mime("content://files/music.flac","audio/*"));}
}
