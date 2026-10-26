package dk.stream.android;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.RemoteViews;

@androidx.media3.common.util.UnstableApi
public final class RadioWidgetProvider extends AppWidgetProvider {
 static String favoriteId(Context context,int widgetId) {return context.getSharedPreferences("radio_widgets",0).getString("station_"+widgetId,"");}
 static void bind(Context context,int widgetId,String id) {context.getSharedPreferences("radio_widgets",0).edit().putString("station_"+widgetId,id).apply();}
 public static void refresh(Context context) {
  AppWidgetManager manager=AppWidgetManager.getInstance(context);int[] ids=manager.getAppWidgetIds(new ComponentName(context,RadioWidgetProvider.class));
  for(int id:ids) update(context,manager,id);
 }
 static void update(Context context,AppWidgetManager manager,int widgetId) {
  RadioStation station=null;try {station=new LibraryStore(context).load().findRadio(favoriteId(context,widgetId));} catch(Exception ignored) { }
  manager.updateAppWidget(widgetId,views(context,widgetId,station));
 }
 static RemoteViews views(Context context,int widgetId,RadioStation station) {
  RemoteViews views=new RemoteViews(context.getPackageName(),R.layout.radio_widget);
  views.setTextViewText(R.id.widget_name,station==null?context.getString(R.string.radio_widget_choose):station.displayName());
  views.setImageViewResource(R.id.widget_logo,R.drawable.ic_channel);
  if(station!=null) {
   String local=LocalChannelLogos.load(context).find(station.displayName());
   if(local!=null) try(java.io.InputStream in=context.getAssets().open(local.substring("file:///android_asset/".length()))) {views.setImageViewBitmap(R.id.widget_logo,BitmapFactory.decodeStream(in));} catch(java.io.IOException ignored) { }
  }
  Intent configure=new Intent(context,RadioWidgetConfigureActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId).setData(Uri.parse("stream://radio-widget/config/"+widgetId));
  PendingIntent config=PendingIntent.getActivity(context,widgetId,configure,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);views.setOnClickPendingIntent(R.id.widget_name,config);
  if(station!=null) {
   boolean playing=RadioPlaybackService.playing(station.id);views.setImageViewResource(R.id.widget_play,playing?R.drawable.ic_stop:R.drawable.ic_play);views.setContentDescription(R.id.widget_play,context.getString(playing?R.string.radio_stop:R.string.radio_start));
   Intent toggle=new Intent(context,RadioPlaybackService.class).setAction(RadioPlaybackService.WIDGET_TOGGLE).setData(Uri.parse("stream://radio-widget/play/"+widgetId)).putExtra("favoriteId",station.id);
   PendingIntent action=android.os.Build.VERSION.SDK_INT>=26?PendingIntent.getForegroundService(context,widgetId,toggle,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE):PendingIntent.getService(context,widgetId,toggle,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   views.setOnClickPendingIntent(R.id.widget_play,action);
  } else {views.setImageViewResource(R.id.widget_play,R.drawable.ic_play);views.setContentDescription(R.id.widget_play,context.getString(R.string.radio_widget_choose));views.setOnClickPendingIntent(R.id.widget_play,config);}
  return views;
 }
 @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids) {for(int id:ids) update(context,manager,id);}
 @Override public void onDeleted(Context context,int[] ids) {android.content.SharedPreferences.Editor edit=context.getSharedPreferences("radio_widgets",0).edit();for(int id:ids) edit.remove("station_"+id);edit.apply();}
}
