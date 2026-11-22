package dk.stream.android;

import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SettingsBackupTest {
 private Library settings(){
  Library library=new Library();Library.Source source=library.addSource(Library.Source.xtream("Film og TV","https://example.com","user@example.com","sødt-password"));library.lastSourceId=source.id;
  library.toggleFavorite(new Library.Entry(source.id,new Playlist.Channel("Æble TV","https://example.com/live","DK",Playlist.Kind.TV,null,"")));
  library.pcloud=new PcloudAccount(PcloudAccount.EU,"cloud@example.com","cloud-secret");library.guides.add(new Library.Guide("guide","TV-guide","https://example.com/guide.xml"));
  LocalMedia folder=new LocalMedia("content://documents/tree/Music/document/Music","Musik","",true);library.setLocalSource(folder);library.toggleLocalFavorite(folder);
  library.toggleLocalFavorite(new LocalMedia(PcloudDocument.uri(library.pcloud,okhttp3.HttpUrl.get(library.pcloud.server+"image.jpg"),42),"Billede.jpg","image/jpeg",false));
  library.addRadio(new RadioStation("radio","Radio","https://example.com/radio.mp3","https://example.com/logo.png"));library.favoritesOnStart=true;library.setFavoritesStartTab(4);library.favoritesCurrentTab=3;library.autoPlayLast=true;library.miniPlayer=true;library.lastPlayed=library.favorites.get(0).channel;library.lastPlayedPosition=1234;return library;
 }
 @Test public void completeSetupRoundTripsIncludingCredentialsAndUnicode() throws Exception {
  Library original=settings();Library restored=SettingsBackup.decode(new ByteArrayInputStream(SettingsBackup.encode(original)));
  assertEquals(original.toJson(),restored.toJson());assertEquals("sødt-password",restored.sources.get(0).password);assertEquals("cloud-secret",restored.pcloud.password);assertEquals(1,restored.sourceFavorites(true).size());assertEquals(1,restored.sourceFavorites(false).size());assertEquals(4,restored.favoritesStartTab);assertEquals(1234,restored.lastPlayedPosition);
 }
 @Test public void acceptsRawSettingsJsonWithoutEnvelope() throws Exception {Library original=settings();assertEquals(original.toJson(),SettingsBackup.decode(new ByteArrayInputStream(original.toJson().getBytes(StandardCharsets.UTF_8))).toJson());}
 @Test public void decodedSetupIsAReplacementNotAMerge() throws Exception {Library old=settings();Library restored=SettingsBackup.decode(new ByteArrayInputStream(SettingsBackup.encode(new Library())));assertTrue(restored.sources.isEmpty());assertTrue(restored.favorites.isEmpty());assertNull(restored.pcloud);assertEquals(1,old.sources.size());}
 @Test(expected=JSONException.class) public void rejectsUnsupportedBackupVersion() throws Exception {JSONObject json=new JSONObject(new String(SettingsBackup.encode(settings()),StandardCharsets.UTF_8));json.put("backupVersion",99);SettingsBackup.decode(new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8)));}
 @Test(expected=JSONException.class) public void rejectsDanglingFavoriteInsteadOfSilentlyDroppingIt() throws Exception {JSONObject json=new JSONObject(settings().toJson());json.put("sources",new JSONArray());SettingsBackup.decode(new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8)));}
 @Test(expected=JSONException.class) public void rejectsTrailingGarbage() throws Exception {SettingsBackup.decode(new ByteArrayInputStream((new Library().toJson()+"bad").getBytes(StandardCharsets.UTF_8)));}
 @Test(expected=IOException.class) public void rejectsOversizedInputBeforeParsing() throws Exception {SettingsBackup.decode(new ByteArrayInputStream(new byte[SettingsBackup.MAX_BYTES+1]));}
 @Test(expected=JSONException.class) public void rejectsAccountWithWrongType() throws Exception {JSONObject json=new JSONObject(settings().toJson());json.put("pcloud","bad");SettingsBackup.decode(new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8)));}
}
