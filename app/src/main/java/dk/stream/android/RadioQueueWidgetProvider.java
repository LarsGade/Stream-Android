package dk.stream.android;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.RemoteViews;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import java.util.*;

/** A 4 by 1 widget with a private selection of favorite radio stations. */
@androidx.media3.common.util.UnstableApi
public final class RadioQueueWidgetProvider extends AppWidgetProvider {
 private static final android.util.LruCache<String,Bitmap> images=new android.util.LruCache<String,Bitmap>(2*1024*1024){protected int sizeOf(String key,Bitmap image){return image.getAllocationByteCount();}};
 private static final Map<Integer,CustomTarget<Bitmap>> requests=new HashMap<>();
 private static final Map<Integer,String> requestKeys=new HashMap<>();
 private static final Map<String,Long> retryAfter=new HashMap<>();
 static boolean owns(Context context,int id){AppWidgetProviderInfo info=AppWidgetManager.getInstance(context).getAppWidgetInfo(id);return info!=null&&new ComponentName(context,RadioQueueWidgetProvider.class).equals(info.provider);}
 private static String imageKey(RadioStation station){return station.id+"|"+station.name+"|"+station.alias+"|"+station.logo;}
 static void refresh(Context context){AppWidgetManager manager=AppWidgetManager.getInstance(context);for(int id:manager.getAppWidgetIds(new ComponentName(context,RadioQueueWidgetProvider.class)))safeUpdate(context,manager,id);}
 static void safeUpdate(Context context,AppWidgetManager manager,int id){try{update(context,manager,id);}catch(RuntimeException ignored){}}
 static void update(Context context,AppWidgetManager manager,int id){
  RadioStation station=null;List<RadioStation> stations=Collections.emptyList();try{stations=RadioQueueWidgetPreferences.stations(context,new LibraryStore(context).load(),id);station=RadioQueueWidgetPreferences.current(context,stations,id);}catch(Exception ignored){}
  Bitmap image=station==null?null:images.get(imageKey(station));manager.updateAppWidget(id,views(context,id,station,image,stations.size()));
  if(station==null){clearRequest(context,id);return;}String key=imageKey(station);if(requests.containsKey(id)&&!key.equals(requestKeys.get(id)))clearRequest(context,id);if(image!=null||requests.containsKey(id))return;
  Long retry=retryAfter.get(key);if(retry!=null&&android.os.SystemClock.elapsedRealtime()<retry)return;
  Object model=PlaybackArtwork.radioModel(context,station);if(model==null)return;
  final String stationId=station.id;Context app=context.getApplicationContext();
  CustomTarget<Bitmap> target=new CustomTarget<Bitmap>(192,192){
   public void onResourceReady(Bitmap resource,Transition<? super Bitmap> transition){
    Bitmap copy=resource.copy(Bitmap.Config.ARGB_8888,false);if(copy!=null)images.put(key,copy);retryAfter.remove(key);
    if(copy!=null&&owns(app,id)&&stationId.equals(currentId(app,id))){RemoteViews patch=new RemoteViews(app.getPackageName(),R.layout.radio_widget_queue);patch.setImageViewBitmap(R.id.widget_logo,copy);try{AppWidgetManager.getInstance(app).partiallyUpdateAppWidget(id,patch);}catch(RuntimeException ignored){}}
    finishRequest();
   }
   public void onLoadFailed(android.graphics.drawable.Drawable error){retryAfter.put(key,android.os.SystemClock.elapsedRealtime()+60000);finishRequest();}
   public void onLoadCleared(android.graphics.drawable.Drawable placeholder){if(requests.get(id)==this){requests.remove(id);requestKeys.remove(id);}}
   private void finishRequest(){if(requests.get(id)==this){requests.remove(id);requestKeys.remove(id);}new android.os.Handler(android.os.Looper.getMainLooper()).post(()->Glide.with(app).clear(this));}
  };
  requests.put(id,target);requestKeys.put(id,key);Glide.with(app).asBitmap().load(model).fitCenter().into(target);
 }
 private static void clearRequest(Context context,int id){CustomTarget<Bitmap> target=requests.remove(id);requestKeys.remove(id);if(target!=null)Glide.with(context.getApplicationContext()).clear(target);}
 private static String currentId(Context context,int id){try{List<RadioStation> stations=RadioQueueWidgetPreferences.stations(context,new LibraryStore(context).load(),id);RadioStation current=RadioQueueWidgetPreferences.current(context,stations,id);return current==null?"":current.id;}catch(Exception ignored){return "";}}
 private static PendingIntent command(Context context,int widget,String action){Intent intent=RadioPlaybackService.internalIntent(context,action).setData(Uri.parse("stream://radio-queue/"+widget+"/"+action)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widget);return android.os.Build.VERSION.SDK_INT>=26?PendingIntent.getForegroundService(context,widget,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE):PendingIntent.getService(context,widget,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static RemoteViews views(Context context,int id,RadioStation station,Bitmap image,int count){
  RemoteViews views=new RemoteViews(context.getPackageName(),R.layout.radio_widget_queue);views.setImageViewResource(R.id.widget_logo,R.drawable.ic_channel);if(image!=null)views.setImageViewBitmap(R.id.widget_logo,image);boolean playing=station!=null&&RadioPlaybackService.playing(station.id);views.setImageViewResource(R.id.widget_play,playing?R.drawable.ic_widget_stop:R.drawable.ic_widget_play);views.setContentDescription(R.id.widget_logo,station==null?context.getString(R.string.radio_widget_queue_choose):station.displayName());views.setContentDescription(R.id.widget_play,context.getString(playing?R.string.stop_action:R.string.play_action));
  PendingIntent toggle,previous=null,next=null;if(station==null){Intent configure=new Intent(context,RadioQueueWidgetConfigureActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).setData(Uri.parse("stream://radio-queue/config/"+id));toggle=PendingIntent.getActivity(context,id,configure,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}else{toggle=command(context,id,RadioPlaybackService.QUEUE_WIDGET_TOGGLE);if(count>1){previous=command(context,id,RadioPlaybackService.QUEUE_WIDGET_PREVIOUS);next=command(context,id,RadioPlaybackService.QUEUE_WIDGET_NEXT);}}
  views.setOnClickPendingIntent(R.id.widget_root,toggle);views.setOnClickPendingIntent(R.id.widget_logo,toggle);views.setOnClickPendingIntent(R.id.widget_play,toggle);views.setOnClickPendingIntent(R.id.widget_play_area,toggle);views.setOnClickPendingIntent(R.id.widget_previous,previous);views.setOnClickPendingIntent(R.id.widget_previous_area,previous);views.setOnClickPendingIntent(R.id.widget_next,next);views.setOnClickPendingIntent(R.id.widget_next_area,next);views.setBoolean(R.id.widget_previous_area,"setEnabled",count>1);views.setBoolean(R.id.widget_next_area,"setEnabled",count>1);views.setInt(R.id.widget_previous,"setImageAlpha",count>1?255:90);views.setInt(R.id.widget_next,"setImageAlpha",count>1?255:90);return views;
 }
 @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){for(int id:ids)safeUpdate(context,manager,id);}
 @Override public void onAppWidgetOptionsChanged(Context context,AppWidgetManager manager,int id,android.os.Bundle options){safeUpdate(context,manager,id);}
 @Override public void onRestored(Context context,int[] oldIds,int[] newIds){android.content.SharedPreferences prefs=context.getSharedPreferences("radio_widgets",0);android.content.SharedPreferences.Editor edit=prefs.edit();for(int old:oldIds)edit.remove("queue_"+old).remove("station_"+old);for(int i=0;i<Math.min(oldIds.length,newIds.length);i++){String queue=prefs.getString("queue_"+oldIds[i],null),station=prefs.getString("station_"+oldIds[i],null);if(queue!=null)edit.putString("queue_"+newIds[i],queue);if(station!=null)edit.putString("station_"+newIds[i],station);}edit.apply();onUpdate(context,AppWidgetManager.getInstance(context),newIds);}
 @Override public void onDeleted(Context context,int[] ids){for(int id:ids)clearRequest(context,id);android.content.SharedPreferences.Editor edit=context.getSharedPreferences("radio_widgets",0).edit();for(int id:ids)edit.remove("station_"+id).remove("queue_"+id);edit.apply();}
}
