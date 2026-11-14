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
 @Test public void localFavoritesAreToTheRightOfRadioAndSelectTheirOwnPanel() {
  RadioGroup tabs=ReflectionHelpers.getField(activity,"favoriteTabs");assertEquals(3,tabs.getChildCount());assertEquals("Radio",((RadioButton)tabs.getChildAt(1)).getText().toString());assertEquals("Lokale filer",((RadioButton)tabs.getChildAt(2)).getText().toString());
  ((RadioButton)tabs.getChildAt(2)).performClick();assertTrue(ReflectionHelpers.<Library>getField(activity,"library").favoritesLocal);assertFalse(ReflectionHelpers.<Library>getField(activity,"library").favoritesRadio);assertEquals(View.VISIBLE,ReflectionHelpers.<LocalFilesPanel>getField(activity,"localPanel").getVisibility());assertEquals(View.GONE,ReflectionHelpers.<RadioPanel>getField(activity,"radioPanel").getVisibility());
 }
 @Test public void pickersAskForReadOnlyPersistentLinksAndSupportMultipleFiles() {
  ReflectionHelpers.callInstanceMethod(activity,"pickLocal",ReflectionHelpers.ClassParameter.from(boolean.class,false));
  Intent files=Shadows.shadowOf(activity).getNextStartedActivityForResult().intent;
  assertEquals(Intent.ACTION_OPEN_DOCUMENT,files.getAction());assertTrue(files.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE,false));assertArrayEquals(new String[]{"audio/*","video/*"},files.getStringArrayExtra(Intent.EXTRA_MIME_TYPES));assertTrue((files.getFlags()&Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)!=0);assertEquals(0,files.getFlags()&Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
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
}
