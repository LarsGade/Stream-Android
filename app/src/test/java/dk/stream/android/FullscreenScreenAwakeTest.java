package dk.stream.android;
import java.io.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class FullscreenScreenAwakeTest {
 @Test public void newAndOldSettingsDefaultOff()throws Exception{assertFalse(new Library().keepScreenOnFullscreen);org.json.JSONObject saved=new org.json.JSONObject(new Library().toJson());saved.remove("keepScreenOnFullscreen");assertFalse(Library.fromJson(saved.toString()).keepScreenOnFullscreen);}
 @Test public void choiceSurvivesRestartAndBackup()throws Exception{for(boolean enabled:new boolean[]{true,false}){Library library=new Library();library.keepScreenOnFullscreen=enabled;assertEquals(enabled,Library.fromJson(library.toJson()).keepScreenOnFullscreen);assertEquals(enabled,SettingsBackup.decode(new ByteArrayInputStream(SettingsBackup.encode(library))).keepScreenOnFullscreen);}}
 @Test public void fullscreenUsesPreferenceEvenWhilePausedOrPlaying(){for(boolean playing:new boolean[]{true,false}){assertTrue(FullscreenScreenAwake.keep(true,true,playing));assertFalse(FullscreenScreenAwake.keep(true,false,playing));}}
 @Test public void leavingFullscreenRestoresOrdinaryPlaybackBehaviour(){assertFalse(FullscreenScreenAwake.keep(false,true,false));assertTrue(FullscreenScreenAwake.keep(false,false,true));}
}
