package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaybackMetadataTest {
 @Test public void radioHasSourceAndRepeatedTitleInsteadOfAnEmptyAlbum(){androidx.media3.common.MediaMetadata metadata=PlaybackText.metadata("DR P1").build();assertEquals("DR P1",metadata.title);assertEquals("DR P1",metadata.displayTitle);assertEquals("Afspilles via Stream",metadata.artist);assertEquals("Afspilles via Stream",metadata.subtitle);assertEquals("DR P1",metadata.albumTitle);assertEquals("DR P1",metadata.albumArtist);}
 @Test public void podcastUsesItsOwnTitleInExtraFields(){androidx.media3.common.MediaMetadata metadata=PlaybackText.metadata("Anders og Anders – Afsnit 2").build();assertEquals("Anders og Anders",metadata.albumTitle);assertEquals("Anders og Anders",metadata.albumArtist);assertEquals("Anders og Anders – Afsnit 2",metadata.title);assertEquals("Afspilles via Stream",metadata.artist);}
 @Test public void seasonEpisodeSuffixIsOnlyRemovedFromExtraFields(){androidx.media3.common.MediaMetadata metadata=PlaybackText.metadata("The Show - S01E02 - The Arrival").build();assertEquals("The Show - S01E02 - The Arrival",metadata.title);assertEquals(metadata.title,metadata.displayTitle);assertEquals("The Show",metadata.albumTitle);}
 @Test public void episodePrefixesAreRemoved(){assertEquals("The Arrival",PlaybackText.shortTitle("S1 · E2 · The Arrival"));assertEquals("The Arrival",PlaybackText.shortTitle("Season 1 Episode 2 - The Arrival"));}
 @Test public void normalNumbersArePreserved(){for(String name:new String[]{"DR P1","TV 2","Studio 54","1984","Room 104"})assertEquals(name,PlaybackText.shortTitle(name));}
 @Test public void emptyResultFallsBackToOriginal(){assertEquals("S01E02",PlaybackText.shortTitle("S01E02"));}
}
