package dk.stream.android;
import java.io.*;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
public class DailyGuideSettingsTest {
 @Test public void dailyRefreshIsOptIn(){assertFalse(new Library().dailyGuideUpdate);}
 @Test public void settingAndGuideLinksSurviveCompleteBackup()throws Exception{Library library=new Library();library.dailyGuideUpdate=true;library.guides.add(new Library.Guide("guide","Guide","https://example.com/guide.xml"));Library restored=SettingsBackup.decode(new ByteArrayInputStream(SettingsBackup.encode(library)));assertTrue(restored.dailyGuideUpdate);assertEquals(library.guides.get(0).url,restored.guides.get(0).url);restored.dailyGuideUpdate=false;assertFalse(Library.fromJson(restored.toJson()).dailyGuideUpdate);}
 @Test public void legacyBackupsDoNotEnableBackgroundDownloads()throws Exception{JSONObject json=new JSONObject(new Library().toJson());json.remove("dailyGuideUpdate");assertFalse(Library.fromJson(json.toString()).dailyGuideUpdate);}
}
