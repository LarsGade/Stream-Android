package dk.stream.android;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import java.util.*;
import java.util.concurrent.*;
import okhttp3.mockwebserver.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.*;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class RadioUiTest {
 private ActivityController<MainActivity> controller;private MainActivity activity;private RadioPanel panel;private Library library;private MockWebServer server;
 @Before public void setup() throws Exception {
  Shadows.shadowOf((android.app.Application)org.robolectric.RuntimeEnvironment.getApplication()).grantPermissions("dk.stream.android.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION");
  server=new MockWebServer();server.start();controller=Robolectric.buildActivity(MainActivity.class).setup();activity=controller.get();library=new Library();
  panel=new RadioPanel(activity,()->library,change->{change.run();return true;},()->{},new RadioBrowser(Arrays.asList(server.url("/"))));
 }
 @After public void close() throws Exception {panel.close();controller.pause().stop().destroy();server.shutdown();}
 private List<TextView> texts(View view) {List<TextView> result=new ArrayList<>();if(view instanceof TextView) result.add((TextView)view);if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) result.addAll(texts(((ViewGroup)view).getChildAt(i)));return result;}
 @Test public void browseCanAddFavoriteAndFavoritesCanRenameReorderAndRemove() throws Exception {
  server.enqueue(new MockResponse().setBody("[{\"stationuuid\":\"a\",\"name\":\"Station\",\"countrycode\":\"DK\",\"url\":\"https://example.com/radio\"}]"));panel.open(false);
  ReflectionHelpers.<ExecutorService>getField(panel,"worker").submit(()->{}).get(10,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
  FavoriteListView list=ReflectionHelpers.getField(panel,"list");assertEquals(1,list.getCount());View row=list.getAdapter().getView(0,null,list);
  ((ImageButton)((ViewGroup)row).getChildAt(2)).performClick();assertEquals(1,library.radioFavorites.size());
  library.addRadio(new RadioStation("b","Second","https://example.com/second",""));panel.open(true);assertEquals(View.GONE,ReflectionHelpers.<TextView>getField(panel,"title").getVisibility());
  row=list.getAdapter().getView(0,null,list);assertEquals(3,((ViewGroup)row).getChildCount());
  list.getOnItemLongClickListener().onItemLongClick(list,row,0,0);PopupMenu popup=ShadowPopupMenu.getLatestPopupMenu();assertEquals(2,popup.getMenu().size());
  // Popup listeners live on each menu item, matching a real click.
  popup.getMenu().performIdentifierAction(popup.getMenu().getItem(0).getItemId(),0);
  Shadows.shadowOf(Looper.getMainLooper()).idle();AlertDialog dialog=(AlertDialog)ShadowDialog.getLatestDialog();assertNotNull(dialog);
  EditText name=null;for(TextView text:texts(dialog.getWindow().getDecorView())) if(text instanceof EditText) name=(EditText)text;
  assertNotNull(name);name.setText("DR P1");dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertEquals("DR P1",library.findRadio("a").displayName());assertNotNull(LocalChannelLogos.load(activity).find(library.findRadio("a").displayName()));
  FavoriteListView.Listener listener=ReflectionHelpers.getField(list,"listener");listener.move(0,1);listener.commit();assertEquals("b",library.radioFavorites.get(0).id);assertEquals("DR P1",library.radioFavorites.get(1).displayName());
  listener.showActions(1,list);popup=ShadowPopupMenu.getLatestPopupMenu();popup.getMenu().performIdentifierAction(popup.getMenu().getItem(1).getItemId(),0);assertNull(library.findRadio("a"));
 }
 @Test public void radioWidgetUsesAliasAndUniqueFavoriteBindingAndCleansUp() {
  RadioStation station=new RadioStation("a","Original","https://example.com/radio","", "DR P1");
  RadioWidgetProvider.bind(activity,21,station.id);RadioWidgetProvider.bind(activity,22,"b");assertEquals("a",RadioWidgetProvider.favoriteId(activity,21));assertEquals("b",RadioWidgetProvider.favoriteId(activity,22));
  View widget=RadioWidgetProvider.views(activity,21,station).apply(activity.getApplicationContext(),new FrameLayout(activity.getApplicationContext()));assertEquals("DR P1",widget.findViewById(R.id.widget_logo).getContentDescription().toString());assertEquals(activity.getString(R.string.radio_start),widget.findViewById(R.id.widget_play).getContentDescription().toString());
  assertTrue(texts(widget).isEmpty());
  for(int target:new int[]{R.id.widget_root,R.id.widget_logo,R.id.widget_play}) {
   widget.findViewById(target).performClick();Intent start=Shadows.shadowOf(activity.getApplication()).getNextStartedService();assertNotNull(start);assertEquals(RadioPlaybackService.WIDGET_TOGGLE,start.getAction());assertEquals(station.id,start.getStringExtra("favoriteId"));assertNull(Shadows.shadowOf(activity.getApplication()).getNextStartedActivity());
  }
  widget=RadioWidgetProvider.views(activity,21,null).apply(activity.getApplicationContext(),new FrameLayout(activity.getApplicationContext()));assertEquals(activity.getString(R.string.radio_widget_choose),widget.findViewById(R.id.widget_play).getContentDescription().toString());
  new RadioWidgetProvider().onDeleted(activity,new int[]{21});assertEquals("",RadioWidgetProvider.favoriteId(activity,21));assertEquals("b",RadioWidgetProvider.favoriteId(activity,22));
 }
 @Test public void replacementLogosAndRadioPlayingIndicatorUseTheCurrentStation() {
  assertEquals(47,LocalChannelLogos.load(activity).size());assertNull(LocalChannelLogos.load(activity).find("P1"));assertNotNull(LocalChannelLogos.load(activity).find("DR P1"));
  for(TextView text:texts(panel)) assertNotEquals("Radio Browser",text.getText().toString());
  library.addRadio(new RadioStation("a","DR P1","https://example.com/a",""));library.addRadio(new RadioStation("b","Nova","https://example.com/b",""));panel.open(true);
  org.robolectric.android.controller.ServiceController<RadioPlaybackService> serviceController=Robolectric.buildService(RadioPlaybackService.class).create();RadioPlaybackService service=serviceController.get();
  androidx.media3.exoplayer.ExoPlayer original=org.robolectric.util.ReflectionHelpers.getField(service,"player");
  java.util.concurrent.atomic.AtomicBoolean audible=new java.util.concurrent.atomic.AtomicBoolean(true);
  androidx.media3.exoplayer.ExoPlayer fake=(androidx.media3.exoplayer.ExoPlayer)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{androidx.media3.exoplayer.ExoPlayer.class},(proxy,method,args)->method.getName().equals("isPlaying")?audible.get():null);
  try {
   ReflectionHelpers.setField(service,"current",library.findRadio("a"));ReflectionHelpers.setField(service,"player",fake);
   FavoriteListView list=ReflectionHelpers.getField(panel,"list");View first=list.getAdapter().getView(0,null,list),other=list.getAdapter().getView(1,null,list);
   assertEquals(View.VISIBLE,first.findViewWithTag("now_playing_dot").getVisibility());assertEquals(View.INVISIBLE,other.findViewWithTag("now_playing_dot").getVisibility());
   audible.set(false);assertEquals(View.INVISIBLE,list.getAdapter().getView(0,null,list).findViewWithTag("now_playing_dot").getVisibility());
  } finally {ReflectionHelpers.setField(service,"player",original);serviceController.destroy();}
 }
 @Test public void drawerPlacesRadioAndRadioFavoritesImmediatelyAfterTvFavorites() {
  List<TextView> labels=texts(activity.getWindow().getDecorView());int tv=-1,radio=-1,favorites=-1;
  for(int i=0;i<labels.size();i++) {String text=labels.get(i).getText().toString();if(text.equals("TV-Favoritter")) tv=i;if(text.equals("Radio")) radio=i;if(text.equals("Radio-Favoritter")) favorites=i;}
  assertTrue(tv>=0);assertEquals(tv+1,radio);assertEquals(radio+1,favorites);
 }
}
