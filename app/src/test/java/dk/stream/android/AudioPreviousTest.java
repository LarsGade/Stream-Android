package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class AudioPreviousTest {
 @Test public void establishedTrackRestarts(){assertEquals(AudioPrevious.Action.RESTART,new AudioPrevious().choose("track",15000,true,true,1000));}
 @Test public void newlyStartedTrackSkipsBack(){assertEquals(AudioPrevious.Action.PREVIOUS,new AudioPrevious().choose("track",3000,true,true,1000));}
 @Test public void doublePressSkipsEvenWhenCastPositionHasNotUpdated(){AudioPrevious button=new AudioPrevious();assertEquals(AudioPrevious.Action.RESTART,button.choose("track",25000,true,true,1000));assertEquals(AudioPrevious.Action.PREVIOUS,button.choose("track",25000,true,true,1300));}
 @Test public void slowerSecondPressRestartsAgain(){AudioPrevious button=new AudioPrevious();button.choose("track",25000,true,true,1000);assertEquals(AudioPrevious.Action.RESTART,button.choose("track",25000,true,true,2000));}
 @Test public void firstTrackCanRestartWithoutPreviousTrack(){AudioPrevious button=new AudioPrevious();assertEquals(AudioPrevious.Action.RESTART,button.choose("first",20000,true,false,1000));assertEquals(AudioPrevious.Action.RESTART,button.choose("first",0,true,false,1200));}
 @Test public void rapidPressAfterTrackChangeDoesNotSkipWrongTrack(){AudioPrevious button=new AudioPrevious();button.choose("old",20000,true,true,1000);assertEquals(AudioPrevious.Action.RESTART,button.choose("new",20000,true,true,1200));}
 @Test public void unseekableTrackStillAllowsPreviousWhenAvailable(){AudioPrevious button=new AudioPrevious();assertEquals(AudioPrevious.Action.PREVIOUS,button.choose("track",20000,false,true,1000));assertEquals(AudioPrevious.Action.NONE,button.choose("track",20000,false,false,2000));}
}
