package dk.stream.android;

import org.junit.Test;
import static org.junit.Assert.*;

/** Pure model checks: only URI references are persisted, separate from TV and radio. */
public class LocalMediaTest {
 @org.junit.Test public void searchLabelsHideRelativePathAndExtension(){LocalMedia file=new LocalMedia("content://test/file","Music / Album / Æble.mp3","audio/mpeg",false);org.junit.Assert.assertEquals("Æble",file.displayFileName());org.junit.Assert.assertEquals("Music / Album / Æble",file.displayName());}
 @org.junit.Test public void searchFolderLabelsKeepFolderName(){LocalMedia folder=new LocalMedia("content://test/folder","Music / Album.v1","",true);org.junit.Assert.assertEquals("Album.v1",folder.displayFileName());}

 @Test public void linksAndFavoritesSurviveRoundTripWithoutChangingUris() throws Exception {
  String tree="content://com.android.externalstorage.documents/tree/primary%3AMusic/document/primary%3AMusic";
  String file="content://com.android.externalstorage.documents/tree/primary%3AMusic/document/primary%3AMusic%2FTrack%201.mp3";
  Library library=new Library();LocalMedia folder=new LocalMedia(tree,"Music","vnd.android.document/directory",true),song=new LocalMedia(file,"Track 1.mp3","audio/mpeg",false);
  library.addLocal(folder);library.addLocal(folder);library.addLocal(song);library.toggleLocalFavorite(folder);library.toggleLocalFavorite(song);library.favoritesLocal=true;
  Library restored=Library.fromJson(library.toJson());assertEquals(2,restored.localFiles.size());assertEquals(2,restored.localFavorites.size());assertTrue(restored.favoritesLocal);assertEquals(tree,restored.localFavorites.get(0).uri);assertEquals(file,restored.localFavorites.get(1).uri);assertTrue(restored.localFavorites.get(0).folder);assertTrue(restored.localFavorites.get(1).audio());
  restored.localFiles.clear();assertEquals(2,restored.localFavorites.size());restored.toggleLocalFavorite(song);assertEquals(1,restored.localFavorites.size());assertFalse(restored.isLocalFavorite(file));
 }
 @Test public void oldLibrariesOpenWithEmptyLocalListsAndKeepRadioTab() throws Exception {
  Library original=new Library();original.favoritesRadio=true;org.json.JSONObject old=new org.json.JSONObject(original.toJson());old.remove("localFiles");old.remove("localFavorites");old.remove("favoritesLocal");
  Library restored=Library.fromJson(old.toString());assertTrue(restored.favoritesRadio);assertFalse(restored.favoritesLocal);assertTrue(restored.localFiles.isEmpty());assertTrue(restored.localFavorites.isEmpty());
 }
 @Test public void classificationUsesMimeAndRecognizesCommonFilesWithGenericMime() {
  assertTrue(new LocalMedia("content://docs/1","Track.FLAC","application/octet-stream",false).audio());
  LocalMedia video=new LocalMedia("content://docs/2","Movie.MKV","application/octet-stream",false);assertTrue(video.supported());assertFalse(video.audio());
  assertFalse(new LocalMedia("content://docs/3","Notes.pdf","application/pdf",false).supported());
  assertTrue(new LocalMedia("content://docs/4","Folder","vnd.android.document/directory",true).supported());
 }
 @Test public void networkUrlsAndRawPathsCannotBecomeLocalLinks() {
  for(String uri:new String[]{"https://example.com/track.mp3","file:///sdcard/track.mp3","/sdcard/track.mp3"}) try {new LocalMedia(uri,"Track","audio/mpeg",false);fail("Expected document URI validation");}catch(IllegalArgumentException expected) { }
 }
 @Test public void replacingSourcePreservesOtherLinksAndUpdatesRootFavorite() throws Exception {
  Library library=new Library();LocalMedia old=new LocalMedia("content://docs/tree/old/document/old","Old","",true),next=new LocalMedia("content://docs/tree/new/document/new","New","",true),song=new LocalMedia("content://docs/song","Song.mp3","audio/mpeg",false);
  library.addLocal(old);library.addLocal(song);library.addLocal(next);library.toggleLocalFavorite(old);library.toggleLocalFavorite(next);library.toggleLocalFavorite(song);
  library.replaceLocalFolder(old.uri,next);
  Library restored=Library.fromJson(library.toJson());assertEquals(2,restored.localFiles.size());assertEquals(next.uri,restored.localFiles.get(0).uri);assertEquals(song.uri,restored.localFiles.get(1).uri);assertEquals(2,restored.localFavorites.size());assertFalse(restored.isLocalFavorite(old.uri));assertTrue(restored.isLocalFavorite(next.uri));assertTrue(restored.isLocalFavorite(song.uri));
 }

 @Test public void localDisplayNamesHideMediaExtensionsWithoutChangingUris() {
  LocalMedia file=new LocalMedia("content://docs/song.MP3","Artist / My.song.MP3","audio/mpeg",false);assertEquals("Artist / My.song",file.displayName());assertTrue(file.uri.endsWith(".MP3"));assertEquals("Folder.mp3",new LocalMedia("content://docs/root","Folder.mp3","",true).displayName());
 }
 @Test public void selectingOneSourceReplacesOldRootsAndKeepsFavorites() {
  Library library=new Library();LocalMedia file=new LocalMedia("content://docs/song","Song.mp3","audio/mpeg",false),root=new LocalMedia("content://docs/root","Root","",true),next=new LocalMedia("content://docs/next","Next","",true);library.addLocal(root);library.addLocal(file);library.toggleLocalFavorite(file);library.setLocalSource(next);assertEquals(1,library.localFiles.size());assertSame(next,library.localFiles.get(0));assertTrue(library.isLocalFavorite(file.uri));
 }

}
