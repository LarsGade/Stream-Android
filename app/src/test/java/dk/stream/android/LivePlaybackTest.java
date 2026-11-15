package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class LivePlaybackTest {
 @Test public void ordinaryFilesNeverOfferLive(){assertFalse(LivePlayback.behind(false,true,0,60000L));}
 @Test public void pausedLiveStreamOffersLiveEvenWithoutSeekableRange(){assertTrue(LivePlayback.behind(true,true,0,null));assertFalse(LivePlayback.behind(true,false,0,null));}
 @Test public void onlyOffersLiveWhenOutsideLiveTolerance(){assertTrue(LivePlayback.behind(true,false,54000,60000L));assertFalse(LivePlayback.behind(true,false,55000,60000L));assertFalse(LivePlayback.behind(true,false,60000,60000L));}
}
