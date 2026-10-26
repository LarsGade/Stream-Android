package dk.stream.android;

import android.content.*;
import androidx.media3.common.*;
import androidx.media3.session.*;
import androidx.media3.session.MediaLibraryService.*;
import java.util.*;
import java.util.concurrent.*;
import okhttp3.mockwebserver.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class CarRadioTest {
 private Context context;private Library library;private MockWebServer server;private CarRadioCatalog catalog;private CarRadioCallback callback;
 @Before public void setup() throws Exception {
  context=RuntimeEnvironment.getApplication();library=new Library();server=new MockWebServer();server.start();
  library.addRadio(new RadioStation("p1","Station one",server.url("/p1.mp3").toString(),"","DR P1"));library.addRadio(new RadioStation("nova","Nova",server.url("/nova.mp3").toString(),""));
  library.addSource(Library.Source.xtream("Secret TV","https://example.com","user","private_password"));
  catalog=new CarRadioCatalog(context,new LibraryPersistence(){public Library load(){return library;}public void save(Library value){}},new RadioBrowser(Arrays.asList(server.url("/"))));callback=new CarRadioCallback(catalog);
 }
 @After public void close() throws Exception {callback.close();server.shutdown();}
 @Test public void rootAndFavoritesExposeOnlyAudioWithAliasesOrderAndPagination() throws Exception {
  List<MediaItem> folders=catalog.children(CarRadioCatalog.ROOT,0,10);assertEquals(2,folders.size());assertEquals(CarRadioCatalog.FAVORITES,folders.get(0).mediaId);assertFalse(folders.get(0).mediaMetadata.isPlayable);
  List<MediaItem> items=catalog.children(CarRadioCatalog.FAVORITES,0,1);assertEquals(1,items.size());assertEquals("DR P1",items.get(0).mediaMetadata.title);assertTrue(items.get(0).mediaMetadata.isPlayable);assertFalse(items.get(0).mediaMetadata.isBrowsable);assertNull(items.get(0).localConfiguration);assertNull(items.get(0).mediaMetadata.artworkData);assertEquals("content",items.get(0).mediaMetadata.artworkUri.getScheme());
  assertEquals("radio:nova",catalog.children(CarRadioCatalog.FAVORITES,1,1).get(0).mediaId);assertTrue(catalog.children(CarRadioCatalog.FAVORITES,Integer.MAX_VALUE,100).isEmpty());assertEquals(0,server.getRequestCount());
  assertEquals(2,callback.onGetChildren(null,null,CarRadioCatalog.FAVORITES,0,10,null).get(5,TimeUnit.SECONDS).value.size());
 }
 @Test public void browseCacheSurvivesNetworkFailureAndVoiceQueryResolvesKnownStation() throws Exception {
  CarRadioCatalog.writeCache(context,Arrays.asList(new RadioStation("cached","DR P3","https://example.com/p3","")));
  assertEquals("radio:cached",catalog.children(CarRadioCatalog.DANISH,0,20).get(0).mediaId);
  assertEquals("radio:p1",catalog.search("DR P1").get(0).mediaId);
  MediaItem request=new MediaItem.Builder().setRequestMetadata(new MediaItem.RequestMetadata.Builder().setSearchQuery("DR P1").build()).build();
  List<MediaItem> resolved=callback.onAddMediaItems(null,null,Arrays.asList(request)).get(5,TimeUnit.SECONDS);assertEquals(server.url("/p1.mp3").toString(),resolved.get(0).localConfiguration.uri.toString());assertNotNull(resolved.get(0).mediaMetadata.artworkData);
  try {callback.onAddMediaItems(null,null,Arrays.asList(MediaItem.fromUri("https://example.com/arbitrary-video.mp4"))).get(5,TimeUnit.SECONDS);fail();} catch(ExecutionException expected) { }
 }
 @Test public void favoritesAreReadFreshAndKeepRenamesAndReordering() throws Exception {
  library.renameRadio("p1","P1 renamed");library.reorderRadio(Arrays.asList(library.findRadio("nova"),library.findRadio("p1")));
  List<MediaItem> items=catalog.children(CarRadioCatalog.FAVORITES,0,20);assertEquals("Nova",items.get(0).mediaMetadata.title);assertEquals("P1 renamed",items.get(1).mediaMetadata.title);
  library.removeRadio("nova");assertEquals(1,catalog.children(CarRadioCatalog.FAVORITES,0,20).size());
 }
 @Test public void artworkProviderExposesOnlyReadOnlyPackagedLogoBytes() throws Exception {
  CarArtworkProvider provider=Robolectric.buildContentProvider(CarArtworkProvider.class).create().get();android.net.Uri uri=CarArtworkProvider.uri(context,"DR P1");
  try(android.os.ParcelFileDescriptor fd=provider.openFile(uri,"r");java.io.InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)) {assertArrayEquals(PlaybackArtwork.local(context,"DR P1"),in.readAllBytes());}
  try {provider.openFile(uri,"w");fail();} catch(java.io.FileNotFoundException expected) { }
  try {provider.openFile(android.net.Uri.parse("content://dk.stream.android.carart/../../stream_library.xml"),"r");fail();} catch(java.io.FileNotFoundException expected) { }
 }
 private <T> T await(com.google.common.util.concurrent.ListenableFuture<T> future) throws Exception {
  long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);while(!future.isDone() && System.nanoTime()<deadline) {Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();Thread.sleep(10);}return future.get(1,TimeUnit.SECONDS);
 }
 @Test public void aRealMediaBrowserCanConnectAndBrowseTheAudioOnlyLibrary() throws Exception {
  org.robolectric.android.controller.ServiceController<RadioPlaybackService> controller=Robolectric.buildService(RadioPlaybackService.class).create();RadioPlaybackService service=controller.get();MediaBrowser browser=null;
  try {
   CarRadioCatalog serviceCatalog=org.robolectric.util.ReflectionHelpers.getField(service,"carCatalog");org.robolectric.util.ReflectionHelpers.setField(serviceCatalog,"store",new LibraryPersistence(){public Library load(){return library;}public void save(Library value){} });
   browser=await(new MediaBrowser.Builder(context,service.onGetSession(null).getToken()).buildAsync());
   assertEquals(CarRadioCatalog.ROOT,await(browser.getLibraryRoot(null)).value.mediaId);
   assertEquals(2,await(browser.getChildren(CarRadioCatalog.ROOT,0,20,null)).value.size());
   assertEquals("DR P1",await(browser.getChildren(CarRadioCatalog.FAVORITES,0,20,null)).value.get(0).mediaMetadata.title);
  } finally {if(browser!=null) browser.release();controller.destroy();}
 }
 @Test public void libraryRootAdvertisesSearchWithoutOpeningNetwork() throws Exception {
  LibraryResult<MediaItem> root=callback.onGetLibraryRoot(null,null,null).get();assertTrue(root.value.mediaMetadata.isBrowsable);assertTrue(root.params.extras.getBoolean("android.media.browse.SEARCH_SUPPORTED"));assertEquals(0,server.getRequestCount());
 }
}
