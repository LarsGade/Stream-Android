package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class ExternalPlaybackReturnTest {
 @Test public void openingChooserDoesNotImmediatelyResumeStream(){ExternalPlaybackReturn state=new ExternalPlaybackReturn();state.foreground=true;state.launched=true;assertFalse(state.canResume(true));}
 @Test public void returningFromPlayerAllowsResume(){ExternalPlaybackReturn state=new ExternalPlaybackReturn();state.launched=true;state.left=true;state.foreground=true;assertTrue(state.canResume(true));}
 @Test public void ResultWhileExternalAppHasFocusCannotResume(){ExternalPlaybackReturn state=new ExternalPlaybackReturn();state.launched=true;state.left=true;state.foreground=true;assertFalse(state.canResume(false));}
 @Test public void backgroundCannotResumeEvenWithStaleFocus(){ExternalPlaybackReturn state=new ExternalPlaybackReturn();state.launched=true;state.left=true;state.foreground=false;assertFalse(state.canResume(true));}
 @Test public void resumeHappensOnlyOnce(){ExternalPlaybackReturn state=new ExternalPlaybackReturn();state.launched=true;state.left=true;state.foreground=true;assertTrue(state.finish());assertFalse(state.finish());assertFalse(state.canResume(true));}
}
