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
 private AlertDialog dialog() {return (AlertDialog)ShadowDialog.getLatestDialog();}
 private void sourceDialog(boolean xtream) {
  clickText(activity.getWindow().getDecorView(),R.string.settings);
  clickText(dialog().getWindow().getDecorView(),R.string.sources);
  dialog().getButton(xtream?AlertDialog.BUTTON_NEUTRAL:AlertDialog.BUTTON_POSITIVE).performClick();
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
  clickText(activity.getWindow().getDecorView(),R.string.settings);views(dialog().getWindow().getDecorView(),SwitchCompat.class).get(0).setChecked(true);assertTrue(storage.saved.favoritesOnStart);
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
  assertEquals(0,panel.video.getControllerShowTimeoutMs());
  for(int label:new int[]{R.string.pause_action,R.string.stop_action,R.string.fullscreen_action,R.string.volume}) assertEquals(View.VISIBLE,control(panel,label).getVisibility());
  control(panel,R.string.pause_action).performClick();
  androidx.media3.exoplayer.ExoPlayer player=ReflectionHelpers.getField(activity,"player");assertFalse(player.getPlayWhenReady());
  control(panel,R.string.play_action).performClick();assertTrue(player.getPlayWhenReady());
  control(panel,R.string.stop_action).performClick();assertEquals(View.GONE,panel.getVisibility());
 }
 @Test public void fullscreenAndBackKeepTheSamePlayer() {
  startTestPlayback();PlaybackPanel panel=ReflectionHelpers.getField(activity,"playbackPanel");Object player=ReflectionHelpers.getField(activity,"player");
  control(panel,R.string.fullscreen_action).performClick();
  assertTrue(ReflectionHelpers.<Boolean>getField(activity,"fullscreen"));
  FrameLayout host=ReflectionHelpers.getField(activity,"fullscreenHost");assertSame(host,panel.getParent());
  assertEquals(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,activity.getRequestedOrientation());
  activity.getOnBackPressedDispatcher().onBackPressed();
  assertFalse(ReflectionHelpers.<Boolean>getField(activity,"fullscreen"));assertSame(player,ReflectionHelpers.getField(activity,"player"));
  assertSame(ReflectionHelpers.getField(activity,"content"),panel.getParent());
 }
 @Test public void volumeSliderAndMuteChangeMusicVolume() {
  startTestPlayback();PlaybackPanel panel=ReflectionHelpers.getField(activity,"playbackPanel");
  android.media.AudioManager audio=(android.media.AudioManager)activity.getSystemService(android.content.Context.AUDIO_SERVICE);
  panel.setVolume(3);assertEquals(3,audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC));assertEquals(3,panel.volume.getProgress());
  control(panel,R.string.mute_action).performClick();assertEquals(0,audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC));
  control(panel,R.string.unmute_action).performClick();assertEquals(3,audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC));
 }
}
