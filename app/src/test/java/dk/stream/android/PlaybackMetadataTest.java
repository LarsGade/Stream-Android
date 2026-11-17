package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaybackMetadataTest {
 @Test public void radioHasSourceAndRepeatedTitleInsteadOfAnEmptyAlbum(){androidx.media3.common.MediaMetadata metadata=PlaybackText.metadata("DR P1").build();assertEquals("DR P1",metadata.title);assertEquals("DR P1",metadata.displayTitle);assertEquals("Afspilles via Stream",metadata.artist);assertEquals("Afspilles via Stream",metadata.subtitle);assertEquals("DR P1",metadata.albumTitle);assertEquals("DR P1",metadata.albumArtist);}
 @Test public void podcastUsesItsOwnTitleInExtraFields(){androidx.media3.common.MediaMetadata metadata=PlaybackText.metadata("Anders og Anders – Afsnit 2").build();assertEquals(metadata.title,metadata.albumTitle);assertEquals(metadata.title,metadata.albumArtist);assertEquals("Afspilles via Stream",metadata.artist);}
}
