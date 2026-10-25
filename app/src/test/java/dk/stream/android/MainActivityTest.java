package dk.stream.android;

import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.util.ReflectionHelpers;
import java.util.*;
import java.util.concurrent.*;
import okhttp3.mockwebserver.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class MainActivityTest {
 private ActivityController<MainActivity> controller;
 private MainActivity activity;
 private MemoryStore storage;
 private MockWebServer server;
 private static final class MemoryStore implements LibraryPersistence {
  Library saved=new Library();
  public Library load() {return saved;}
  public void save(Library value) throws Exception {saved=Library.fromJson(value.toJson());}
 }
 @Before public void start() throws Exception {
  server=new MockWebServer();server.start();
  controller=Robolectric.buildActivity(MainActivity.class).setup();activity=controller.get();
  storage=new MemoryStore();ReflectionHelpers.setField(activity,"store",storage);
 }
 @After public void stop() throws Exception {controller.pause().stop().destroy();server.shutdown();}
 private <T extends View> List<T> views(View root,Class<T> type) {
  List<T> found=new ArrayList<>();if(type.isInstance(root)) found.add(type.cast(root));
  if(root instanceof ViewGroup) for(int i=0;i<((ViewGroup)root).getChildCount();i++) found.addAll(views(((ViewGroup)root).getChildAt(i),type));return found;
 }
 private void clickText(View root,int resource) {
  for(TextView view:views(root,TextView.class)) if(view.getText().toString().equals(activity.getString(resource))) {view.performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();return;}
  fail("Missing control "+resource);
 }
 private AlertDialog dialog() {Shadows.shadowOf(Looper.getMainLooper()).idle();return (AlertDialog)ShadowDialog.getLatestDialog();}
 private void sourceDialog(boolean xtream) {
  clickText(activity.getWindow().getDecorView(),R.string.sources);
  clickText(dialog().getWindow().getDecorView(),xtream?R.string.xtream_login:R.string.add_m3u);
  Shadows.shadowOf(Looper.getMainLooper()).idle();
 }
 private void awaitLoadAndRender() throws Exception {
  ExecutorService worker=ReflectionHelpers.getField(activity,"worker");worker.submit(()->{}).get(30,TimeUnit.SECONDS);
  Shadows.shadowOf(Looper.getMainLooper()).idle();
  View decor=activity.getWindow().getDecorView();decor.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY));decor.layout(0,0,1080,1920);
  Shadows.shadowOf(Looper.getMainLooper()).idle();
 }
 private void loadM3u() throws Exception {
  server.enqueue(new MockResponse().setBody("#EXTM3U\n#EXTINF:-1 group-title=\"DK\" tvg-logo=\"/logo.png\",DR 1\nhttps://example.com/live/1.m3u8\n"));
  server.enqueue(new MockResponse().setResponseCode(404));
  sourceDialog(false);List<EditText> fields=views(dialog().getWindow().getDecorView(),EditText.class);
  fields.get(0).setText("Testkilde");fields.get(1).setText(server.url("/list.m3u").toString());dialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();awaitLoadAndRender();
 }
 @Test public void addingM3uSourceRendersChannelAndLogoWithoutCrashing() throws Exception {
  loadM3u();ListView list=ReflectionHelpers.getField(activity,"list");
  assertEquals(1,list.getAdapter().getCount());View row=list.getAdapter().getView(0,null,list);
  assertEquals(1,storage.saved.sources.size());assertTrue(views(row,TextView.class).stream().anyMatch(view->view.getText().toString().equals("DR 1")));
 }
 @Test public void addingXtreamSourceRendersAllSections() throws Exception {
  for(String body:new String[]{"{\"user_info\":{\"auth\":1}}","[]","[{\"stream_id\":1,\"name\":\"TV\"}]","[]","[{\"stream_id\":2,\"name\":\"Film\"}]","[]","[{\"series_id\":3,\"name\":\"Serie\"}]"}) server.enqueue(new MockResponse().setBody(body));
  sourceDialog(true);List<EditText> fields=views(dialog().getWindow().getDecorView(),EditText.class);
  fields.get(0).setText("Xtream");fields.get(1).setText(server.url("/").toString());fields.get(2).setText("user");fields.get(3).setText("password");
  dialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();awaitLoadAndRender();
  ListView list=ReflectionHelpers.getField(activity,"list");assertEquals(1,list.getAdapter().getCount());
  clickText(activity.getWindow().getDecorView(),R.string.films);assertEquals(1,list.getAdapter().getCount());
  clickText(activity.getWindow().getDecorView(),R.string.series);assertEquals(1,list.getAdapter().getCount());assertTrue(storage.saved.sources.get(0).xtream);
 }
 @Test public void loadedChannelCanBeFavoritedAndSelectedAsStartupList() throws Exception {
  loadM3u();ListView list=ReflectionHelpers.getField(activity,"list");View row=list.getAdapter().getView(0,null,list);
  views(row,ImageButton.class).get(0).performClick();assertEquals(1,storage.saved.favorites.size());
  views(activity.getWindow().getDecorView(),SwitchCompat.class).get(0).setChecked(true);assertTrue(storage.saved.favoritesOnStart);
 }
 @Test public void favoritesHideFiltersAndCountAndShowEveryContentType() throws Exception {
  loadM3u();Library library=ReflectionHelpers.getField(activity,"library");String sourceId=library.sources.get(0).id;
  library.toggleFavorite(new Library.Entry(sourceId,new Playlist.Channel("TV","https://example.com/live","DK")));
  library.toggleFavorite(new Library.Entry(sourceId,new Playlist.Channel("Film","https://example.com/movie.mp4","Film",Playlist.Kind.FILM,null)));
  library.toggleFavorite(new Library.Entry(sourceId,new Playlist.Channel("Serie","https://example.com/episode.mp4","Serier",Playlist.Kind.SERIES,null)));
  clickText(activity.getWindow().getDecorView(),R.string.favorites);
  ListView list=ReflectionHelpers.getField(activity,"list");assertEquals(3,list.getAdapter().getCount());
  for(String name:new String[]{"search","groups","sections","status"}) {
   View control=ReflectionHelpers.getField(activity,name);assertEquals(name,View.GONE,control.getVisibility());
  }
  TextView status=ReflectionHelpers.getField(activity,"status");assertEquals("",status.getText().toString());
  View row=list.getAdapter().getView(0,null,list);
  List<TextView> labels=views(row,TextView.class);assertEquals("TV",labels.get(0).getText().toString());
  assertEquals(View.GONE,labels.get(1).getVisibility());
  clickText(activity.getWindow().getDecorView(),R.string.library);
  for(String name:new String[]{"search","groups","sections"}) {View control=ReflectionHelpers.getField(activity,name);assertEquals(name,View.VISIBLE,control.getVisibility());}
 }
 private void startTestPlayback() {
  server.enqueue(new MockResponse().setResponseCode(404));
  Playlist.Channel channel=new Playlist.Channel("Test",server.url("/live.ts").toString(),"DK");
  ReflectionHelpers.callInstanceMethod(activity,"play",ReflectionHelpers.ClassParameter.from(Playlist.Channel.class,channel));
 }
 private View control(View root,int description) {
  String label=activity.getString(description);
  for(View view:views(root,View.class)) if(label.contentEquals(view.getContentDescription()==null?"":view.getContentDescription())) return view;
  throw new AssertionError("Missing control "+label);
 }
 @Test public void playerShowsVolumeTransportAndFullscreenControls() {
  startTestPlayback();PlaybackPanel panel=ReflectionHelpers.getField(activity,"playbackPanel");
  assertEquals(View.VISIBLE,panel.getVisibility());
  assertFalse(panel.video.getUseController());assertFalse(panel.controlsVisible());panel.video.performClick();assertTrue(panel.controlsVisible());
  for(int label:new int[]{R.string.pause_action,R.string.stop_action,R.string.fullscreen_action,R.string.player_settings}) assertEquals(View.VISIBLE,control(panel,label).getVisibility());
  control(panel,R.string.pause_action).performClick();
  androidx.media3.exoplayer.ExoPlayer player=ReflectionHelpers.getField(activity,"player");assertFalse(player.getPlayWhenReady());
  control(panel,R.string.play_action).performClick();assertTrue(player.getPlayWhenReady());
  control(panel,R.string.stop_action).performClick();assertEquals(View.GONE,panel.getVisibility());
 }
 @Test public void fullscreenAndBackKeepTheSamePlayer() {
  startTestPlayback();PlaybackPanel panel=ReflectionHelpers.getField(activity,"playbackPanel");Object player=ReflectionHelpers.getField(activity,"player");
  if(!panel.controlsVisible()) panel.video.performClick();control(panel,R.string.fullscreen_action).performClick();
  assertTrue(ReflectionHelpers.<Boolean>getField(activity,"fullscreen"));
  FrameLayout host=ReflectionHelpers.getField(activity,"fullscreenHost");assertSame(host,panel.getParent());
  assertEquals(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,activity.getRequestedOrientation());
  activity.getOnBackPressedDispatcher().onBackPressed();
  assertFalse(ReflectionHelpers.<Boolean>getField(activity,"fullscreen"));assertSame(player,ReflectionHelpers.getField(activity,"player"));
  assertSame(ReflectionHelpers.getField(activity,"content"),panel.getParent());
 }
 @Test public void deviceMusicStreamAndMuteWorkWithoutVolumeSlider() {
  startTestPlayback();PlaybackPanel panel=ReflectionHelpers.getField(activity,"playbackPanel");
  android.media.AudioManager audio=(android.media.AudioManager)activity.getSystemService(android.content.Context.AUDIO_SERVICE);
  panel.video.performClick();audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC,3,0);panel.refresh();
  assertEquals(android.media.AudioManager.STREAM_MUSIC,activity.getVolumeControlStream());
  assertEquals(1,views(panel,SeekBar.class).size()); // Only the playback timeline remains.
  control(panel,R.string.mute_action).performClick();assertEquals(0,audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC));
  control(panel,R.string.unmute_action).performClick();assertEquals(3,audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC));
 }
 @Test public void fullscreenVideoUsesWholeViewportAndHidesControlsAutomatically() {
  startTestPlayback();PlaybackPanel panel=ReflectionHelpers.getField(activity,"playbackPanel");
  if(!panel.controlsVisible()) panel.video.performClick();control(panel,R.string.fullscreen_action).performClick();
  FrameLayout host=ReflectionHelpers.getField(activity,"fullscreenHost");
  host.measure(View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY));host.layout(0,0,1920,1080);
  assertEquals(1920,panel.video.getWidth());assertEquals(1080,panel.video.getHeight());
  assertEquals(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT,panel.video.getResizeMode());
  LinearLayout overlay=ReflectionHelpers.getField(panel,"controls");assertEquals(View.GONE,overlay.getVisibility());
  assertFalse(panel.video.isControllerFullyVisible());
  panel.video.performClick();assertEquals(View.VISIBLE,overlay.getVisibility());
  Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(3000));
  assertEquals(View.GONE,overlay.getVisibility());assertFalse(panel.video.isControllerFullyVisible());
  assertTrue((activity.getWindow().getAttributes().flags & android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)!=0);
  int ui=activity.getWindow().getDecorView().getSystemUiVisibility();
  assertTrue((ui & View.SYSTEM_UI_FLAG_FULLSCREEN)!=0);assertTrue((ui & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)!=0);
  activity.onWindowFocusChanged(true);assertTrue((activity.getWindow().getDecorView().getSystemUiVisibility() & View.SYSTEM_UI_FLAG_FULLSCREEN)!=0);
  activity.getOnBackPressedDispatcher().onBackPressed();
  assertEquals(View.GONE,overlay.getVisibility());assertEquals(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT,panel.video.getResizeMode());
  assertTrue((activity.getWindow().getAttributes().flags & android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)!=0);
  ReflectionHelpers.callInstanceMethod(activity,"stopPlaybackState");assertEquals(0,activity.getWindow().getAttributes().flags & android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
 }
 @Test public void inlinePlayerStartsCleanAndTapControlsAutoHideWithoutChangingViewport() {
  startTestPlayback();PlaybackPanel panel=ReflectionHelpers.getField(activity,"playbackPanel");
  assertFalse(panel.controlsVisible());assertFalse(panel.video.getUseController());
  TextView status=ReflectionHelpers.getField(activity,"status");assertEquals(View.GONE,status.getVisibility());
  panel.measure(View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1500,View.MeasureSpec.AT_MOST));panel.layout(0,0,panel.getMeasuredWidth(),panel.getMeasuredHeight());
  assertEquals(panel.getWidth(),panel.video.getWidth());assertEquals(panel.getHeight(),panel.video.getHeight());
  long now=android.os.SystemClock.uptimeMillis();
  android.view.MotionEvent down=android.view.MotionEvent.obtain(now,now,android.view.MotionEvent.ACTION_DOWN,10,10,0);
  android.view.MotionEvent up=android.view.MotionEvent.obtain(now,now+20,android.view.MotionEvent.ACTION_UP,10,10,0);
  panel.video.dispatchTouchEvent(down);panel.video.dispatchTouchEvent(up);down.recycle();up.recycle();
  assertTrue(panel.controlsVisible());
  int height=panel.video.getHeight();
  Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(3000));
  assertFalse(panel.controlsVisible());assertEquals(height,panel.video.getHeight());
  panel.video.performClick();assertTrue(panel.controlsVisible());panel.video.performClick();assertFalse(panel.controlsVisible());
 }
 @Test public void playbackSettingsRemainAvailableInTapOverlay() {
  startTestPlayback();PlaybackPanel panel=ReflectionHelpers.getField(activity,"playbackPanel");panel.video.performClick();
  control(panel,R.string.player_settings).performClick();
  PopupMenu menu=org.robolectric.shadows.ShadowPopupMenu.getLatestPopupMenu();assertNotNull(menu);
  menu.getMenu().performIdentifierAction(1,0);
  AlertDialog speed=dialog();ListView choices=speed.getListView();choices.performItemClick(null,5,choices.getAdapter().getItemId(5));
  androidx.media3.exoplayer.ExoPlayer player=ReflectionHelpers.getField(activity,"player");assertEquals(2f,player.getPlaybackParameters().speed,0.01f);
 }
 @Test public void fullscreenKeepsEntireVideoAndAddsSideBordersWhenNeeded() {
  startTestPlayback();PlaybackPanel panel=ReflectionHelpers.getField(activity,"playbackPanel");panel.video.performClick();control(panel,R.string.fullscreen_action).performClick();
  androidx.media3.ui.AspectRatioFrameLayout image=panel.video.findViewById(androidx.media3.ui.R.id.exo_content_frame);image.setAspectRatio(16f/9f);
  FrameLayout host=ReflectionHelpers.getField(activity,"fullscreenHost");host.measure(View.MeasureSpec.makeMeasureSpec(2400,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY));host.layout(0,0,2400,1080);
  assertEquals(2400,panel.video.getWidth());assertEquals(1080,panel.video.getHeight());
  assertEquals(1920,image.getWidth());assertEquals(1080,image.getHeight());assertEquals(240,image.getLeft());
  assertFalse(panel.controlsVisible());
 }
 private String currentXml() {
  java.text.SimpleDateFormat format=new java.text.SimpleDateFormat("yyyyMMddHHmmss",java.util.Locale.ROOT);format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));long now=System.currentTimeMillis();
  return "<tv><channel id='dr1.dk'><display-name>DR 1</display-name></channel><programme channel='dr1.dk' start='"+format.format(new java.util.Date(now-3600000))+" +0000' stop='"+format.format(new java.util.Date(now+3600000))+" +0000'><title>Nyheder lige nu</title></programme></tv>";
 }
 private void useGuideDispatcher() {
  server.setDispatcher(new Dispatcher() {@Override public MockResponse dispatch(RecordedRequest request) {
   String path=request.getPath();if(path.contains("bad")) return new MockResponse().setResponseCode(503);
   if(path.contains("epg")) return new MockResponse().setBody(currentXml());
   return new MockResponse().setBody("#EXTM3U\n#EXTINF:-1 tvg-id=\"dr1.dk\",DR 1\nhttps://example.com/live/1.m3u8\n");
  }});
 }
 private void awaitGuides() throws Exception {
  ExecutorService executor=ReflectionHelpers.getField(activity,"epgWorker");executor.submit(()->{}).get(30,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
 }
 @Test public void namedGuideCanBeAssignedAndCurrentProgrammeAppearsInGrayOnFavorite() throws Exception {
  useGuideDispatcher();clickText(activity.getWindow().getDecorView(),R.string.tv_guide);
  dialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();List<EditText> guideFields=views(dialog().getWindow().getDecorView(),EditText.class);guideFields.get(0).setText("Dansk guide");guideFields.get(1).setText(server.url("/epg.xml").toString());dialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();awaitGuides();
  assertEquals("Dansk guide",storage.saved.guides.get(0).name);
  sourceDialog(false);List<EditText> fields=views(dialog().getWindow().getDecorView(),EditText.class);fields.get(0).setText("Dansk TV");fields.get(1).setText(server.url("/list.m3u").toString());views(dialog().getWindow().getDecorView(),Spinner.class).get(0).setSelection(1);dialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();awaitLoadAndRender();awaitGuides();
  Library library=ReflectionHelpers.getField(activity,"library");assertEquals(library.guides.get(0).id,library.sources.get(0).epgId);
  ListView list=ReflectionHelpers.getField(activity,"list");View row=list.getAdapter().getView(0,null,list);views(row,ImageButton.class).get(0).performClick();clickText(activity.getWindow().getDecorView(),R.string.favorites);
  View favorite=list.getAdapter().getView(0,null,list);List<TextView> labels=views(favorite,TextView.class);assertEquals("Nyheder lige nu",labels.get(1).getText().toString());assertEquals(View.VISIBLE,labels.get(1).getVisibility());assertEquals(android.graphics.Color.rgb(170,170,170),labels.get(1).getCurrentTextColor());
 }
 @Test public void updateAllRefreshesEverySourceAndEveryGuideAndContinuesAfterFailures() throws Exception {
  useGuideDispatcher();Library library=ReflectionHelpers.getField(activity,"library");
  library.addSource(Library.Source.m3u("Good source",server.url("/list.m3u").toString()));library.addSource(Library.Source.m3u("Bad source",server.url("/bad-source").toString()));
  Library.Guide good=Library.Guide.create("Good guide",server.url("/epg.xml").toString());library.putGuide(good);library.putGuide(Library.Guide.create("Bad guide",server.url("/bad-epg").toString()));
  clickText(activity.getWindow().getDecorView(),R.string.update_all);awaitLoadAndRender();
  assertEquals(4,server.getRequestCount());Map<String,EpgData> schedules=ReflectionHelpers.getField(activity,"epgData");assertNotNull(schedules.get(good.id));
  TextView result=dialog().findViewById(android.R.id.message);assertTrue(result.getText().toString().contains("Bad source"));assertTrue(result.getText().toString().contains("Bad guide"));assertEquals(2,storage.saved.sources.size());
 }
 @Test public void resetRequiresYesAndNoLeavesDataUntouched() {
  android.app.ActivityManager manager=(android.app.ActivityManager)activity.getSystemService(android.content.Context.ACTIVITY_SERVICE);
  org.robolectric.shadows.ShadowActivityManager shadow=Shadows.shadowOf(manager);
  clickText(activity.getWindow().getDecorView(),R.string.reset_app);assertFalse(shadow.isApplicationUserDataCleared());
  assertEquals(activity.getString(R.string.no),dialog().getButton(AlertDialog.BUTTON_NEGATIVE).getText().toString());dialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertFalse(shadow.isApplicationUserDataCleared());
  clickText(activity.getWindow().getDecorView(),R.string.reset_app);dialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(shadow.isApplicationUserDataCleared());
 }
 @Test public void sourceCanBeEditedAndDeletedFromSourceMenu() throws Exception {
  useGuideDispatcher();Library library=ReflectionHelpers.getField(activity,"library");Library.Source source=library.addSource(Library.Source.m3u("Old name",server.url("/list.m3u").toString()));
  clickText(activity.getWindow().getDecorView(),R.string.sources);
  ListView list=dialog().getListView();list.getOnItemLongClickListener().onItemLongClick(list,null,0,0);dialog().getListView().performItemClick(null,0,0);
  List<EditText> fields=views(dialog().getWindow().getDecorView(),EditText.class);fields.get(0).setText("Updated name");dialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();awaitLoadAndRender();assertEquals("Updated name",storage.saved.findSource(source.id).name);assertEquals(1,storage.saved.sources.size());
  clickText(activity.getWindow().getDecorView(),R.string.sources);list=dialog().getListView();list.getOnItemLongClickListener().onItemLongClick(list,null,0,0);dialog().getListView().performItemClick(null,1,1);dialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(storage.saved.sources.isEmpty());
 }
 @Test public void guideCanBeRenamedAndDeletedWithoutDeletingItsSource() throws Exception {
  useGuideDispatcher();Library library=ReflectionHelpers.getField(activity,"library");Library.Guide guide=Library.Guide.create("Old guide",server.url("/epg.xml").toString());library.putGuide(guide);Library.Source source=library.addSource(Library.Source.m3u("TV",server.url("/list.m3u").toString()).withEpg(guide.id));
  clickText(activity.getWindow().getDecorView(),R.string.tv_guide);dialog().getListView().performItemClick(null,0,0);
  views(dialog().getWindow().getDecorView(),EditText.class).get(0).setText("New guide");dialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();awaitGuides();assertEquals("New guide",storage.saved.findGuide(guide.id).name);
  clickText(activity.getWindow().getDecorView(),R.string.tv_guide);dialog().getListView().performItemClick(null,0,0);dialog().getButton(AlertDialog.BUTTON_NEUTRAL).performClick();dialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertNull(storage.saved.findGuide(guide.id));assertNotNull(storage.saved.findSource(source.id));assertEquals("",storage.saved.findSource(source.id).epgId);
 }
 @Test public void drawerContainsAllFormerSettingsAndSourceCancelIsBelowXtream() {
  View root=activity.getWindow().getDecorView();
  for(int label:new int[]{R.string.sources,R.string.tv_guide,R.string.update_all,R.string.reset_app}) assertTrue(views(root,Button.class).stream().anyMatch(button->button.getText().toString().equals(activity.getString(label))));
  assertTrue(views(root,SwitchCompat.class).get(0).getText().toString().equals(activity.getString(R.string.start_favorites)));
  clickText(root,R.string.sources);View dialogRoot=dialog().getWindow().getDecorView();
  List<Button> actions=new ArrayList<>();for(Button button:views(dialogRoot,Button.class)) if(button.getVisibility()==View.VISIBLE && !button.getText().toString().isEmpty()) actions.add(button);
  assertEquals(3,actions.size());assertEquals(activity.getString(R.string.add_m3u),actions.get(0).getText().toString());assertEquals(activity.getString(R.string.xtream_login),actions.get(1).getText().toString());assertEquals(activity.getString(android.R.string.cancel),actions.get(2).getText().toString());
  assertSame(actions.get(1).getParent(),actions.get(2).getParent());assertEquals(LinearLayout.VERTICAL,((LinearLayout)actions.get(2).getParent()).getOrientation());
  actions.get(2).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse(dialog().isShowing());
 }
 @Test public void headingsHaveNoGuideTextAndCreditIsAtBottom() {
  TextView status=ReflectionHelpers.getField(activity,"status");assertEquals(View.GONE,status.getVisibility());assertEquals("",status.getText().toString());
  LinearLayout content=ReflectionHelpers.getField(activity,"content");TextView footer=(TextView)content.getChildAt(content.getChildCount()-1);
  assertEquals("Udviklet af Lars Gade",footer.getText().toString());assertEquals(android.view.Gravity.CENTER,footer.getGravity());
  clickText(activity.getWindow().getDecorView(),R.string.favorites);assertEquals(View.GONE,status.getVisibility());
 }
 private FavoriteListView sampleFavorites() {
  Library library=ReflectionHelpers.getField(activity,"library");var source=library.addSource(Library.Source.m3u("TV","https://example.com/list"));
  for(String name:new String[]{"One","Two","Three"}) library.toggleFavorite(new Library.Entry(source.id,new Playlist.Channel(name,"https://example.com/"+name,"DK")));
  clickText(activity.getWindow().getDecorView(),R.string.favorites);
  FavoriteListView list=ReflectionHelpers.getField(activity,"list");list.measure(View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY));list.layout(0,0,600,900);return list;
 }
 private void listTouch(FavoriteListView list,int action,float y) {
  long now=android.os.SystemClock.uptimeMillis();android.view.MotionEvent event=android.view.MotionEvent.obtain(now,now,action,40,y,0);list.onTouchEvent(event);event.recycle();
 }
 @Test public void favoritesHaveNoStarOrHeadingAndLongPressOffersRemoval() {
  FavoriteListView list=sampleFavorites();TextView title=ReflectionHelpers.getField(activity,"pageTitle");assertEquals(View.GONE,title.getVisibility());assertEquals("",title.getText().toString());
  View row=list.getAdapter().getView(0,null,list);assertEquals(View.GONE,views(row,ImageButton.class).get(0).getVisibility());
  list.getOnItemLongClickListener().onItemLongClick(list,list.getChildAt(0),0,0);
  PopupMenu menu=org.robolectric.shadows.ShadowPopupMenu.getLatestPopupMenu();assertNotNull(menu);assertEquals("Fjern fra favoritter",menu.getMenu().getItem(0).getTitle().toString());menu.getMenu().performIdentifierAction(menu.getMenu().getItem(0).getItemId(),0);
  assertEquals(2,storage.saved.favorites.size());assertEquals("Two",storage.saved.favorites.get(0).channel.name);
 }
 @Test public void holdingThenDraggingReordersFavoritesAndPersistsOnRelease() throws Exception {
  FavoriteListView list=sampleFavorites();float first=list.getChildAt(0).getTop()+list.getChildAt(0).getHeight()/2f,last=list.getChildAt(2).getTop()+list.getChildAt(2).getHeight()/2f;
  listTouch(list,android.view.MotionEvent.ACTION_DOWN,first);list.getOnItemLongClickListener().onItemLongClick(list,list.getChildAt(0),0,0);listTouch(list,android.view.MotionEvent.ACTION_MOVE,last);
  assertTrue(storage.saved.favorites.isEmpty());
  listTouch(list,android.view.MotionEvent.ACTION_UP,last);
  assertEquals("Two",storage.saved.favorites.get(0).channel.name);assertEquals("Three",storage.saved.favorites.get(1).channel.name);assertEquals("One",storage.saved.favorites.get(2).channel.name);
  assertEquals("One",Library.fromJson(storage.saved.toJson()).favorites.get(2).channel.name);assertNull(ReflectionHelpers.getField(activity,"player"));
 }
 @Test public void cancelledDragKeepsOriginalFavoriteOrder() {
  FavoriteListView list=sampleFavorites();float first=list.getChildAt(0).getHeight()/2f,last=list.getChildAt(2).getTop()+list.getChildAt(2).getHeight()/2f;
  listTouch(list,android.view.MotionEvent.ACTION_DOWN,first);list.getOnItemLongClickListener().onItemLongClick(list,list.getChildAt(0),0,0);listTouch(list,android.view.MotionEvent.ACTION_MOVE,last);listTouch(list,android.view.MotionEvent.ACTION_CANCEL,last);
  Library library=ReflectionHelpers.getField(activity,"library");assertEquals("One",library.favorites.get(0).channel.name);
  java.util.List<Library.Entry> visible=ReflectionHelpers.getField(activity,"visible");assertEquals("One",visible.get(0).channel.name);
 }
 @Test public void stationaryHoldAndReleaseOpensActionsWithoutStartingPlayback() {
  FavoriteListView list=sampleFavorites();float y=list.getChildAt(0).getTop()+list.getChildAt(0).getHeight()/2f;
  listTouch(list,android.view.MotionEvent.ACTION_DOWN,y);list.getOnItemLongClickListener().onItemLongClick(list,list.getChildAt(0),0,0);listTouch(list,android.view.MotionEvent.ACTION_UP,y);
  PopupMenu menu=org.robolectric.shadows.ShadowPopupMenu.getLatestPopupMenu();assertNotNull(menu);assertEquals("Fjern fra favoritter",menu.getMenu().getItem(0).getTitle().toString());assertNull(ReflectionHelpers.getField(activity,"player"));
 }
}
