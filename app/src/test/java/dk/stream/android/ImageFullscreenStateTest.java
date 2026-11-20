package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class ImageFullscreenStateTest {
 @Test public void automaticFullscreenFollowsUnlockedRotation(){ImageFullscreenState state=new ImageFullscreenState(-1);assertFalse(state.update(false,true));assertTrue(state.update(true,true));assertFalse(state.update(false,true));}
 @Test public void lockedRotationDoesNotAutomaticallyEnterFullscreen(){ImageFullscreenState state=new ImageFullscreenState(-1);assertFalse(state.update(true,false));}
 @Test public void buttonWorksEvenWhenRotationIsLocked(){ImageFullscreenState state=new ImageFullscreenState(-1);state.update(false,false);assertTrue(state.toggle());assertTrue(state.update(false,false));assertFalse(state.toggle());}
 @Test public void manualExitLastsUntilOrientationChanges(){ImageFullscreenState state=new ImageFullscreenState(-1);assertTrue(state.update(true,true));assertFalse(state.toggle());assertFalse(state.update(true,true));assertFalse(state.update(false,true));assertTrue(state.update(true,true));}
 @Test public void manualChoiceSurvivesRecreation(){ImageFullscreenState state=new ImageFullscreenState(-1);state.update(false,false);state.toggle();ImageFullscreenState restored=new ImageFullscreenState(state.saved());assertTrue(restored.update(false,false));}
}
