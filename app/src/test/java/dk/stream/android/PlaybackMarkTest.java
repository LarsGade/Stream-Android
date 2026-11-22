package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaybackMarkTest {
 @Test public void remembersTheExactUnfinishedPosition(){PlaybackMark mark=new PlaybackMark(123456,600000,false);assertEquals(123456,mark.resume());assertEquals(205,mark.progress());}
 @Test public void completedMediaShowsFullProgressAndStartsAtZero(){PlaybackMark mark=new PlaybackMark(590000,600000,true);assertEquals(1000,mark.progress());assertEquals(0,mark.resume());}
 @Test public void reachingTheEndMarksCompletion(){PlaybackMark mark=new PlaybackMark(600000,600000,false);assertTrue(mark.complete);assertEquals(0,mark.resume());}
 @Test public void unknownDurationShowsNoProgress(){PlaybackMark mark=new PlaybackMark(5000,-1,false);assertEquals(0,mark.progress());assertEquals(0,mark.resume());}
 @Test public void invalidPositionsAreClamped(){assertEquals(0,new PlaybackMark(-5,1000,false).position);assertEquals(1000,new PlaybackMark(5000,1000,false).position);}
 @Test public void largeDurationsDoNotOverflow(){PlaybackMark mark=new PlaybackMark(Long.MAX_VALUE/2,Long.MAX_VALUE,false);assertEquals(500,mark.progress());}
}
