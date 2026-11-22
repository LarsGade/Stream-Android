package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaybackCoverStateTest {
 @Test public void identicalCoverBytesDoNotReload(){PlaybackCoverState state=new PlaybackCoverState();assertTrue(state.update("song",new byte[]{1,2,3},5));assertFalse(state.update("song",new byte[]{1,2,3},5));assertTrue(state.update("song",new byte[]{1,2,4},5));}
 @Test public void transientMissingMetadataKeepsSameSongsCover(){PlaybackCoverState state=new PlaybackCoverState();state.update("song","cover",5);assertFalse(state.update("song",null,5));assertEquals("cover",state.model());}
 @Test public void nextSongNeverKeepsPreviousSongsCover(){PlaybackCoverState state=new PlaybackCoverState();state.update("first","cover",5);assertTrue(state.update("second",null,5));assertNull(state.model());}
 @Test public void coverCanArriveAfterInitialFallback(){PlaybackCoverState state=new PlaybackCoverState();assertTrue(state.update("song",null,5));assertFalse(state.update("song",null,5));assertTrue(state.update("song","cover",5));assertEquals("cover",state.model());}
 @Test public void remembersBytesIndependentlyOfMutableMetadata(){PlaybackCoverState state=new PlaybackCoverState();byte[] data={1,2};state.update("song",data,5);data[0]=3;assertTrue(state.update("song",data,5));}
 @Test public void clearAllowsViewToLoadCoverAgain(){PlaybackCoverState state=new PlaybackCoverState();state.update("song","cover",5);state.clear();assertTrue(state.update("song","cover",5));}
}
