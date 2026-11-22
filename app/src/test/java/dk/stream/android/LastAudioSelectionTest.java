package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import java.util.*;
public class LastAudioSelectionTest {
 @Test public void podcastRoundTripKeepsExactFileAndAccountReference()throws Exception{LocalMedia file=new LocalMedia("content://dk.stream.android.pcloud/account?url=https%3A%2F%2Fexample.com%2Fepisode.mp3","Episode","audio/mpeg",false);LastAudioSelection restored=LastAudioSelection.read(LastAudioSelection.file(file).json());assertNull(restored.radio);assertEquals(file.uri,restored.file.uri);assertEquals("Episode",restored.file.name);}
 @Test public void radioRoundTripKeepsStationAliasAndStream()throws Exception{RadioStation station=new RadioStation("p1","Original","https://example.com/live","","DR P1");LastAudioSelection restored=LastAudioSelection.read(LastAudioSelection.radio(station).json());assertNull(restored.file);assertEquals("DR P1",restored.radio.displayName());assertEquals(station.url,restored.radio.url);}
 @Test public void latestFileDoesNotHaveRadioAsAFallback()throws Exception{String stored=LastAudioSelection.radio(new RadioStation("p1","P1","https://example.com/live","")).json();stored=LastAudioSelection.file(new LocalMedia("content://files/episode","Podcast","audio/mpeg",false)).json();assertNull(LastAudioSelection.read(stored).radio);assertEquals("file:content://files/episode",LastAudioSelection.read(stored).key());}
 @Test(expected=IllegalArgumentException.class) public void videoDoesNotReplaceTheLastAudio(){LastAudioSelection.file(new LocalMedia("content://files/movie","Movie.mp4","video/mp4",false));}
 @Test(expected=IllegalArgumentException.class) public void foldersAreNotPlaybackSelections(){LastAudioSelection.file(new LocalMedia("content://files/folder","Podcast","",true));}
 @Test(expected=JSONException.class) public void ambiguousSelectionIsRejected()throws Exception{LastAudioSelection.read("{\"radio\":{},\"file\":{}}");}
 @Test public void newPreferenceSurvivesBackup()throws Exception{Map<String,Map<String,Object>> data=BackupPreferences.empty();String selection=LastAudioSelection.file(new LocalMedia("content://files/episode","Episode","audio/mpeg",false)).json();data.get("last_audio").put("selection",selection);assertEquals(selection,BackupPreferences.decode(BackupPreferences.encode(data)).get("last_audio").get("selection"));}
 @Test public void olderBackupsWithoutLastAudioStillImport()throws Exception{JSONObject saved=BackupPreferences.encode(BackupPreferences.empty());saved.remove("last_audio");assertTrue(BackupPreferences.decode(saved).get("last_audio").isEmpty());}
}
