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
  library.addRadio(new RadioStation("b","Second","https://example.com/second",""));panel.open(true);
  row=list.getAdapter().getView(0,null,list);assertEquals(2,((ViewGroup)row).getChildCount());
  list.getOnItemLongClickListener().onItemLongClick(list,row,0,0);PopupMenu popup=ShadowPopupMenu.getLatestPopupMenu();assertEquals(2,popup.getMenu().size());
  // Popup listeners live on each menu item, matching a real click.
  popup.getMenu().performIdentifierAction(popup.getMenu().getItem(0).getItemId(),0);
  Shadows.shadowOf(Looper.getMainLooper()).idle();AlertDialog dialog=(AlertDialog)ShadowDialog.getLatestDialog();assertNotNull(dialog);
  EditText name=null;for(TextView text:texts(dialog.getWindow().getDecorView())) if(text instanceof EditText) name=(EditText)text;
  assertNotNull(name);name.setText("P1");dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertEquals("P1",library.findRadio("a").displayName());assertNotNull(LocalChannelLogos.load(activity).find(library.findRadio("a").displayName()));
  FavoriteListView.Listener listener=ReflectionHelpers.getField(list,"listener");listener.move(0,1);listener.commit();assertEquals("b",library.radioFavorites.get(0).id);assertEquals("P1",library.radioFavorites.get(1).displayName());
  listener.showActions(1,list);popup=ShadowPopupMenu.getLatestPopupMenu();popup.getMenu().performIdentifierAction(popup.getMenu().getItem(1).getItemId(),0);assertNull(library.findRadio("a"));
 }
 @Test public void radioWidgetUsesAliasAndUniqueFavoriteBindingAndCleansUp() {
  RadioStation station=new RadioStation("a","Original","https://example.com/radio","", "P1");
  RadioWidgetProvider.bind(activity,21,station.id);RadioWidgetProvider.bind(activity,22,"b");assertEquals("a",RadioWidgetProvider.favoriteId(activity,21));assertEquals("b",RadioWidgetProvider.favoriteId(activity,22));
  View widget=RadioWidgetProvider.views(activity,21,station).apply(activity.getApplicationContext(),new FrameLayout(activity.getApplicationContext()));assertEquals("P1",((TextView)widget.findViewById(R.id.widget_name)).getText().toString());assertEquals(activity.getString(R.string.radio_start),((TextView)widget.findViewById(R.id.widget_play)).getText().toString());
  widget.findViewById(R.id.widget_play).performClick();Intent start=Shadows.shadowOf(activity.getApplication()).getNextStartedService();assertNotNull(start);assertEquals(RadioPlaybackService.TOGGLE,start.getAction());assertEquals(station.id,start.getStringExtra("favoriteId"));
  widget=RadioWidgetProvider.views(activity,21,null).apply(activity.getApplicationContext(),new FrameLayout(activity.getApplicationContext()));assertEquals(activity.getString(R.string.radio_widget_choose),((TextView)widget.findViewById(R.id.widget_play)).getText().toString());
  new RadioWidgetProvider().onDeleted(activity,new int[]{21});assertEquals("",RadioWidgetProvider.favoriteId(activity,21));assertEquals("b",RadioWidgetProvider.favoriteId(activity,22));
 }
 @Test public void drawerPlacesRadioAndRadioFavoritesImmediatelyAfterTvFavorites() {
  List<TextView> labels=texts(activity.getWindow().getDecorView());int tv=-1,radio=-1,favorites=-1;
  for(int i=0;i<labels.size();i++) {String text=labels.get(i).getText().toString();if(text.equals("TV-Favoritter")) tv=i;if(text.equals("Radio")) radio=i;if(text.equals("Radio-Favoritter")) favorites=i;}
  assertTrue(tv>=0);assertEquals(tv+1,radio);assertEquals(radio+1,favorites);
 }
}
