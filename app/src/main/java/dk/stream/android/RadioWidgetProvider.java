package dk.stream.android;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.RemoteViews;

@androidx.media3.common.util.UnstableApi
public final class RadioWidgetProvider extends AppWidgetProvider {
 private static final android.os.Handler feedbackHandler=new android.os.Handler(android.os.Looper.getMainLooper());
 private static final java.util.Map<Integer,Feedback> feedback=new java.util.HashMap<>();
 private static final class Feedback {final boolean play;final long until;Feedback(boolean play) {this.play=play;until=android.os.SystemClock.elapsedRealtime()+650;}}
 static void showFeedback(Context context,int widgetId,boolean play) {
  if(widgetId==AppWidgetManager.INVALID_APPWIDGET_ID) return;
  boolean alreadyShowing=feedbackVisible(widgetId);Feedback state=new Feedback(play);feedback.put(widgetId,state);Context app=context.getApplicationContext();
  RemoteViews pulse=new RemoteViews(app.getPackageName(),R.layout.radio_widget);pulse.setImageViewResource(R.id.widget_play,play?R.drawable.ic_play:R.drawable.ic_stop);
  if(!alreadyShowing) pulse.setDisplayedChild(R.id.widget_feedback,1);
  partialUpdate(app,widgetId,pulse);
  feedbackHandler.postDelayed(()->{
   if(feedback.get(widgetId)!=state) return;
   RemoteViews hide=new RemoteViews(app.getPackageName(),R.layout.radio_widget);hide.setDisplayedChild(R.id.widget_feedback,0);partialUpdate(app,widgetId,hide);
   // Wait for fade-out before replacing the full layout or reloading logo bitmaps.
   feedbackHandler.postDelayed(()->{if(feedback.get(widgetId)==state) {feedback.remove(widgetId);safeUpdate(app,widgetId);}},180);
  },650);
 }
 private static void partialUpdate(Context context,int widgetId,RemoteViews views) {try {AppWidgetManager.getInstance(context).partiallyUpdateAppWidget(widgetId,views);} catch(RuntimeException ignored) { }}
 // A removed or unavailable launcher widget must never interrupt audio playback.
 private static void safeUpdate(Context context,int widgetId) {try {update(context,AppWidgetManager.getInstance(context),widgetId);} catch(RuntimeException ignored) { }}
 static boolean feedbackVisible(int widgetId) {Feedback state=feedback.get(widgetId);return state!=null && android.os.SystemClock.elapsedRealtime()<state.until;}
 static String favoriteId(Context context,int widgetId) {return context.getSharedPreferences("radio_widgets",0).getString("station_"+widgetId,"");}
 static void bind(Context context,int widgetId,String id) {context.getSharedPreferences("radio_widgets",0).edit().putString("station_"+widgetId,id).apply();}
 public static void refresh(Context context) {
  AppWidgetManager manager=AppWidgetManager.getInstance(context);int[] ids=manager.getAppWidgetIds(new ComponentName(context,RadioWidgetProvider.class));
  for(int id:ids) update(context,manager,id);
 }
 static void update(Context context,AppWidgetManager manager,int widgetId) {
  if(feedback.containsKey(widgetId)) return; // Player events must not restart a running animation.
  RadioStation station=null;try {station=new LibraryStore(context).load().findRadio(favoriteId(context,widgetId));} catch(Exception ignored) { }
  manager.updateAppWidget(widgetId,views(context,widgetId,station));
 }
 static RemoteViews views(Context context,int widgetId,RadioStation station) {
  RemoteViews views=new RemoteViews(context.getPackageName(),R.layout.radio_widget);
  views.setContentDescription(R.id.widget_logo,station==null?context.getString(R.string.radio_widget_choose):station.displayName());
  views.setImageViewResource(R.id.widget_logo,R.drawable.ic_channel);
  if(station!=null) {
   String local=LocalChannelLogos.load(context).find(station.displayName());
   if(local!=null) try(java.io.InputStream in=context.getAssets().open(local.substring("file:///android_asset/".length()))) {views.setImageViewBitmap(R.id.widget_logo,BitmapFactory.decodeStream(in));} catch(java.io.IOException ignored) { }
  }
  Intent configure=new Intent(context,RadioWidgetConfigureActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId).setData(Uri.parse("stream://radio-widget/config/"+widgetId));
  PendingIntent config=PendingIntent.getActivity(context,widgetId,configure,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  if(station!=null) {
   boolean playing=RadioPlaybackService.playing(station.id);Feedback pulse=feedback.get(widgetId);boolean show=feedbackVisible(widgetId);views.setDisplayedChild(R.id.widget_feedback,show?1:0);views.setImageViewResource(R.id.widget_play,show && !pulse.play?R.drawable.ic_stop:R.drawable.ic_play);views.setContentDescription(R.id.widget_play,context.getString(playing?R.string.radio_stop:R.string.radio_start));
   Intent toggle=new Intent(context,RadioPlaybackService.class).setAction(RadioPlaybackService.WIDGET_TOGGLE).setData(Uri.parse("stream://radio-widget/play/"+widgetId)).putExtra("favoriteId",station.id).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId);
   PendingIntent action=android.os.Build.VERSION.SDK_INT>=26?PendingIntent.getForegroundService(context,widgetId,toggle,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE):PendingIntent.getService(context,widgetId,toggle,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   views.setOnClickPendingIntent(R.id.widget_root,action);views.setOnClickPendingIntent(R.id.widget_logo,action);views.setOnClickPendingIntent(R.id.widget_play,action);
  } else {views.setDisplayedChild(R.id.widget_feedback,0);views.setImageViewResource(R.id.widget_play,R.drawable.ic_play);views.setContentDescription(R.id.widget_play,context.getString(R.string.radio_widget_choose));views.setOnClickPendingIntent(R.id.widget_root,config);views.setOnClickPendingIntent(R.id.widget_logo,config);views.setOnClickPendingIntent(R.id.widget_play,config);}
  return views;
 }
 @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids) {for(int id:ids) update(context,manager,id);}
 @Override public void onDeleted(Context context,int[] ids) {android.content.SharedPreferences.Editor edit=context.getSharedPreferences("radio_widgets",0).edit();for(int id:ids) {edit.remove("station_"+id);feedback.remove(id);}edit.apply();}
}
