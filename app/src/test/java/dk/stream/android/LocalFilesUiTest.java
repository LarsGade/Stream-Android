package dk.stream.android;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class LocalFilesUiTest {
 private ActivityController<MainActivity> controller;private MainActivity activity;
 @Before public void setup() {
  Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions("dk.stream.android.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION");
  controller=Robolectric.buildActivity(MainActivity.class).setup();activity=controller.get();
  ReflectionHelpers.setField(activity,"store",new LibraryPersistence(){public Library load(){return new Library();}public void save(Library library){}});
 }
 @After public void close() {controller.pause().stop().destroy();}
 @Test public void favoriteDropdownSelectsTheLocalPanel(){
  Library library=ReflectionHelpers.getField(activity,"library");library.toggleLocalFavorite(new LocalMedia("content://test/audio","Audio.mp3","audio/mpeg",false));
  ReflectionHelpers.callInstanceMethod(activity,"showFavoriteTab",ReflectionHelpers.ClassParameter.from(int.class,2));
  Spinner choices=ReflectionHelpers.getField(activity,"favoriteCategories");assertEquals(View.VISIBLE,choices.getVisibility());assertEquals("Lokale",choices.getSelectedItem());assertEquals(View.VISIBLE,ReflectionHelpers.<LocalFilesPanel>getField(activity,"localPanel").getVisibility());assertEquals(View.GONE,ReflectionHelpers.<RadioPanel>getField(activity,"radioPanel").getVisibility());
 }
 @Test public void emptyFavoriteCategoriesAreHiddenAndLastRemovalFallsBack(){
  Library library=ReflectionHelpers.getField(activity,"library");library.favorites.clear();library.radioFavorites.clear();library.localFavorites.clear();library.imageFavorites.clear();
  LocalMedia image=new LocalMedia("content://test/photo","Photo.jpg","image/jpeg",false);library.toggleLocalFavorite(image);
  ReflectionHelpers.callInstanceMethod(activity,"showFavoriteTab",ReflectionHelpers.ClassParameter.from(int.class,0));
  Spinner choices=ReflectionHelpers.getField(activity,"favoriteCategories");assertEquals(1,choices.getCount());assertEquals("Lokale",choices.getSelectedItem());
  library.toggleLocalFavorite(image);ReflectionHelpers.callInstanceMethod(activity,"filter");assertEquals(View.GONE,choices.getVisibility());assertEquals(View.GONE,ReflectionHelpers.<LocalFilesPanel>getField(activity,"localPanel").getVisibility());
 }
 @Test public void pickersAskForReadOnlyPersistentLinksAndSupportMultipleFiles() {
  ReflectionHelpers.callInstanceMethod(activity,"pickLocal",ReflectionHelpers.ClassParameter.from(boolean.class,false));
  Intent files=Shadows.shadowOf(activity).getNextStartedActivityForResult().intent;
  assertEquals(Intent.ACTION_OPEN_DOCUMENT,files.getAction());assertTrue(files.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE,false));assertArrayEquals(new String[]{"audio/*","video/*","image/*"},files.getStringArrayExtra(Intent.EXTRA_MIME_TYPES));assertTrue((files.getFlags()&Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)!=0);assertEquals(0,files.getFlags()&Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
  ReflectionHelpers.callInstanceMethod(activity,"pickLocal",ReflectionHelpers.ClassParameter.from(boolean.class,true));
  Intent folder=Shadows.shadowOf(activity).getNextStartedActivityForResult().intent;assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE,folder.getAction());assertTrue((folder.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0);
 }
 public static class Documents extends ContentProvider {
  static Uri queried;static int streams;
  public boolean onCreate(){return true;}
  public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort) {
   queried=uri;MatrixCursor c=new MatrixCursor(new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE});
   c.addRow(new Object[]{"root/song","Song.mp3","audio/mpeg"});c.addRow(new Object[]{"root/folder","Folder",DocumentsContract.Document.MIME_TYPE_DIR});c.addRow(new Object[]{"root/video","Movie.mp4","video/mp4"});c.addRow(new Object[]{"root/text","Notes.txt","text/plain"});return c;
  }
  public String getType(Uri uri){return null;}public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}public int update(Uri uri,ContentValues values,String s,String[] a){throw new UnsupportedOperationException();}public int delete(Uri uri,String s,String[] a){throw new UnsupportedOperationException();}
  @Override public android.os.ParcelFileDescriptor openFile(Uri uri,String mode) {streams++;throw new UnsupportedOperationException("Browsing must not open or copy files");}
 }
 @Test public void folderBrowsingKeepsDocumentUrisAndDoesNotReadMediaBytes() {
  Documents.streams=0;Documents provider=Robolectric.buildContentProvider(Documents.class).create().get();org.robolectric.shadows.ShadowContentResolver.registerProviderInternal("local.test",provider);
  LocalMedia root=new LocalMedia("content://local.test/tree/root/document/root","Root",DocumentsContract.Document.MIME_TYPE_DIR,true);
  List<LocalMedia> entries=LocalMedia.children(activity,root);assertEquals(3,entries.size());assertTrue(entries.get(0).folder);assertEquals("Folder",entries.get(0).name);assertTrue(entries.get(1).uri.startsWith("content://local.test/tree/root/document/"));assertTrue(Documents.queried.toString().endsWith("/children"));assertEquals(0,Documents.streams);
 }
 public static class NestedDocuments extends Documents {
  @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort) {
   MatrixCursor c=new MatrixCursor(new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE});
   String id=DocumentsContract.getDocumentId(uri);
   if(id.equals("root")) {c.addRow(new Object[]{"a","First",DocumentsContract.Document.MIME_TYPE_DIR});c.addRow(new Object[]{"root/direct","Direct.mp4","video/mp4"});}
   else if(id.equals("a")) {c.addRow(new Object[]{"b","Second",DocumentsContract.Document.MIME_TYPE_DIR});}
   else if(id.equals("b")) {c.addRow(new Object[]{"b/song","Deep.mp3","audio/mpeg"});c.addRow(new Object[]{"a","Cycle",DocumentsContract.Document.MIME_TYPE_DIR});}
   return c;
  }
 }
 @Test public void recursiveViewIncludesDeepFilesAndStopsProviderCycles() {
  NestedDocuments provider=Robolectric.buildContentProvider(NestedDocuments.class).create().get();org.robolectric.shadows.ShadowContentResolver.registerProviderInternal("nested.test",provider);
  LocalMedia root=new LocalMedia("content://nested.test/tree/root/document/root","Root","",true);
  List<LocalMedia> all=LocalMedia.contents(activity,root);assertEquals(3,all.size());assertEquals("First / Second / Deep.mp3",all.get(2).name);assertEquals("content://nested.test/tree/root/document/b%2Fsong",all.get(2).uri);
 }
 @Test public void rowChildrenDoNotStealFolderOrPlaybackClicks() {
  Library library=new Library();LocalMedia root=new LocalMedia("content://local.test/tree/root/document/root","Root","",true),song=new LocalMedia("content://local.test/song","Song.mp3","audio/mpeg",false);library.setLocalSource(root);library.toggleLocalFavorite(root);library.toggleLocalFavorite(song);
  LocalMedia[] played={null},changed={null};
  LocalFilesPanel panel=new LocalFilesPanel(activity,new LocalFilesPanel.Actions() {
   public Library library(){return library;}public boolean save(Runnable action){action.run();return true;}public void pickFiles(){}public void pickFolder(){}public void play(LocalMedia entry){played[0]=entry;}public void changeFolder(LocalMedia entry){changed[0]=entry;}
  });
  try {
   panel.open(true);ListView list=ReflectionHelpers.getField(panel,"list");
   View row=list.getAdapter().getView(1,null,list);assertFalse(row.hasFocusable());list.performItemClick(row,1,1);assertEquals(song,played[0]);
   Documents provider=Robolectric.buildContentProvider(Documents.class).create().get();org.robolectric.shadows.ShadowContentResolver.registerProviderInternal("local.test",provider);
   list.performItemClick(list.getAdapter().getView(0,null,list),0,0);assertEquals(root,ReflectionHelpers.<ArrayDeque<LocalMedia>>getField(panel,"parents").peekLast());
   panel.open(false);Button change=ReflectionHelpers.getField(panel,"changeFolder");assertEquals(View.VISIBLE,change.getVisibility());change.performClick();assertEquals(root,changed[0]);assertTrue(panel.back());
  } finally {panel.close();}
 }

 @Test public void bothFileTypesStartTheSameQueueServiceWithoutPassingTheFolderThroughBinder() {
  for(LocalMedia file:Arrays.asList(new LocalMedia("content://local.test/song","Song.mp3","audio/mpeg",false),new LocalMedia("content://local.test/movie","Movie.mp4","video/mp4",false))) {
   ReflectionHelpers.callInstanceMethod(activity,"playLocal",ReflectionHelpers.ClassParameter.from(LocalMedia.class,file));
   Intent intent=Shadows.shadowOf(activity.getApplication()).getNextStartedService();assertEquals(LocalAudioService.PLAY,intent.getAction());assertEquals(file.uri,intent.getStringExtra("uri"));assertNotNull(intent.getStringExtra("queue"));assertEquals(5,intent.getExtras().size());
  }
 }
 @Test public void playbackServiceAdvancesThroughMixedQueueAndSurvivesScreenChanges() {
  LocalMedia first=new LocalMedia("content://local.test/first","First.mp4","video/mp4",false),second=new LocalMedia("content://local.test/second","Second.mp3","audio/mpeg",false),third=new LocalMedia("content://local.test/third","Third.mp4","video/mp4",false);
  org.robolectric.android.controller.ServiceController<LocalAudioService> serviceController=Robolectric.buildService(LocalAudioService.class).create();
  try {
   LocalAudioService service=serviceController.get();service.onStartCommand(LocalAudioService.playIntent(activity,first,Arrays.asList(first,second,third)),0,1);
   androidx.media3.exoplayer.ExoPlayer player=LocalAudioService.player();assertEquals(3,player.getMediaItemCount());assertEquals(first.uri,LocalAudioService.current().uri);assertEquals(androidx.media3.common.Player.REPEAT_MODE_OFF,player.getRepeatMode());assertFalse(player.getShuffleModeEnabled());
   ReflectionHelpers.callInstanceMethod(activity,"syncLocalPlayback");assertSame(player,ReflectionHelpers.getField(activity,"player"));
   controller.pause().stop();assertSame(player,LocalAudioService.player());assertEquals(3,player.getMediaItemCount());assertTrue(player.getPlayWhenReady());
   controller.start().resume();assertSame(player,ReflectionHelpers.getField(activity,"player"));
   player.seekToNextMediaItem();assertEquals(second.uri,LocalAudioService.current().uri);assertTrue(LocalAudioService.playing(second.uri));
   service.onStartCommand(LocalAudioService.command(activity,LocalAudioService.TOGGLE),0,2);assertFalse(LocalAudioService.playing(second.uri));
   service.onStartCommand(LocalAudioService.command(activity,LocalAudioService.TOGGLE),0,3);assertTrue(LocalAudioService.playing(second.uri));
   player.seekToNextMediaItem();assertEquals(third.uri,LocalAudioService.current().uri);assertFalse(player.hasNextMediaItem());
  } finally {serviceController.destroy();}
 }

 @Test public void localFavoritesCanBeSearchedByVisibleName() {
  Library library=ReflectionHelpers.getField(activity,"library");library.toggleLocalFavorite(new LocalMedia("content://docs/a","Alpha.mp3","audio/mpeg",false));library.toggleLocalFavorite(new LocalMedia("content://docs/b","Beta.mp4","video/mp4",false));
  LocalFilesPanel panel=ReflectionHelpers.getField(activity,"localPanel");panel.open(true);EditText search=ReflectionHelpers.getField(panel,"search");search.setText("beta");ListView list=ReflectionHelpers.getField(panel,"list");assertEquals(1,list.getAdapter().getCount());assertEquals("Beta",((LocalMedia)list.getAdapter().getItem(0)).displayName());
 }

}
