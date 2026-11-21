package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class ExternalOutputStateTest {
 @Test public void chooserAloneNeverTurnsBlue(){ExternalOutputState.begin("one");assertFalse(ExternalOutputState.blue(false));}
 @Test public void foreignPlayerTurnsBlueWhileStreamIsStopped(){ExternalOutputState.begin("two");ExternalOutputState.choose("two","other.player","stream");assertTrue(ExternalOutputState.blue(false));ExternalOutputState.end("two");assertFalse(ExternalOutputState.blue(false));}
 @Test public void playbackInStreamClearsBlueForGood(){ExternalOutputState.begin("three");ExternalOutputState.choose("three","other.player","stream");assertFalse(ExternalOutputState.blue(true));assertFalse(ExternalOutputState.blue(false));}
 @Test public void choosingStreamCannotMarkAnExternalOutput(){ExternalOutputState.begin("four");ExternalOutputState.choose("four","stream","stream");assertFalse(ExternalOutputState.blue(false));}
 @Test public void staleCallbackCannotActivateNewSession(){ExternalOutputState.begin("old");ExternalOutputState.begin("new");ExternalOutputState.choose("old","other.player","stream");assertFalse(ExternalOutputState.blue(false));ExternalOutputState.choose("new","other.player","stream");ExternalOutputState.end("old");assertTrue(ExternalOutputState.blue(false));ExternalOutputState.end("new");}
 @Test public void musicInStreamDuringThePickerDoesNotDiscardTheLaterChoice(){ExternalOutputState.begin("picker");assertFalse(ExternalOutputState.blue(true));ExternalOutputState.choose("picker","other.player","stream");assertTrue(ExternalOutputState.blue(false));ExternalOutputState.end("picker");}
}
