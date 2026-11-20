package dk.stream.android;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class DownloadNamesTest {
 @Test public void removesUnsafePathsAndControlCharacters(){assertEquals("episode.mp3",DownloadNames.path("../../episode.mp3"));assertEquals("Folder/episode_.mp3",DownloadNames.path("/Folder/episode\n.mp3"));assertEquals("Download",DownloadNames.clean(".."));}
 @Test public void duplicateNamesKeepExtensionsAndDanishCharacters(){Set<String> used=new HashSet<>();assertEquals("Bæltestedet.mp3",DownloadNames.unique("Bæltestedet.mp3",used));assertEquals("bæltestedet (2).MP3",DownloadNames.unique("bæltestedet.MP3",used));}
 @Test public void episodeExtensionComesFromUrlWithoutCopyingCredentials(){assertEquals("Episode 1.mp4",DownloadNames.file("Episode 1","https://example.com/series/user/password/123.mp4?token=secret"));assertEquals("Episode.mp3",DownloadNames.file("Episode.mp3","https://example.com/123.mp3"));}
}
