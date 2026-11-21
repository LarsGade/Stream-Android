package dk.stream.android;
import android.content.Context;
import java.util.*;
@androidx.media3.common.util.UnstableApi
final class RadioQueueWidgetPreferences {
 static List<String> ids(Context context,int widget){try{return RadioWidgetQueue.read(context.getSharedPreferences("radio_widgets",0).getString("queue_"+widget,"[]"));}catch(Exception invalid){return Collections.emptyList();}}
 static List<RadioStation> stations(Context context,Library library,int widget){return RadioWidgetQueue.available(library.radioFavorites,ids(context,widget));}
 static RadioStation current(Context context,List<RadioStation> stations,int widget){return RadioWidgetQueue.current(stations,RadioWidgetProvider.favoriteId(context,widget));}
 static void bind(Context context,int widget,List<String> ids)throws Exception{String previous=RadioWidgetProvider.favoriteId(context,widget);context.getSharedPreferences("radio_widgets",0).edit().putString("queue_"+widget,RadioWidgetQueue.json(ids)).putString("station_"+widget,ids.contains(previous)?previous:ids.get(0)).apply();}
 private RadioQueueWidgetPreferences(){}
}
