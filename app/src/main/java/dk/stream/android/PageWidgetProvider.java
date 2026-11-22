package dk.stream.android;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.widget.RemoteViews;

@androidx.media3.common.util.UnstableApi
public final class PageWidgetProvider extends AppWidgetProvider {
 static boolean owns(Context context,int id){AppWidgetProviderInfo info=AppWidgetManager.getInstance(context).getAppWidgetInfo(id);return info!=null&&new ComponentName(context,PageWidgetProvider.class).equals(info.provider);}
 static String page(Context context,int id){return context.getSharedPreferences("page_widgets",0).getString("page_"+id,"");}
 static boolean bind(Context context,int id,String page){if(!PageShortcut.valid(page))throw new IllegalArgumentException("Invalid page");return context.getSharedPreferences("page_widgets",0).edit().putString("page_"+id,page).commit();}
 static void refresh(Context context){AppWidgetManager manager=AppWidgetManager.getInstance(context);for(int id:manager.getAppWidgetIds(new ComponentName(context,PageWidgetProvider.class)))update(context,manager,id);}
 static void update(Context context,AppWidgetManager manager,int id){
  String page=page(context,id);boolean configured=PageShortcut.valid(page);RemoteViews views=new RemoteViews(context.getPackageName(),R.layout.page_widget);
  views.setTextViewText(R.id.page_widget_name,configured?UiText.text(page):context.getString(R.string.page_widget_choose));
  views.setContentDescription(R.id.page_widget_root,configured?UiText.text(page):context.getString(R.string.page_widget_choose));
  int pixels=Math.round(65.52f*context.getResources().getDisplayMetrics().density);views.setImageViewBitmap(R.id.page_widget_icon,HomePageIcons.circle(configured?page:"Podcast",pixels));
  Intent intent=configured?new Intent(context,MainActivity.class).putExtra(PageShortcut.EXTRA,page).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP):new Intent(context,PageWidgetConfigureActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
  intent.setData(Uri.parse("stream://page-widget/"+id));PendingIntent open=PendingIntent.getActivity(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);views.setOnClickPendingIntent(R.id.page_widget_root,open);manager.updateAppWidget(id,views);
 }
 @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){for(int id:ids)update(context,manager,id);}
 @Override public void onAppWidgetOptionsChanged(Context context,AppWidgetManager manager,int id,android.os.Bundle options){update(context,manager,id);}
 @Override public void onDeleted(Context context,int[] ids){SharedPreferences.Editor editor=context.getSharedPreferences("page_widgets",0).edit();for(int id:ids)editor.remove("page_"+id);editor.apply();}
 @Override public void onRestored(Context context,int[] oldIds,int[] newIds){SharedPreferences prefs=context.getSharedPreferences("page_widgets",0);SharedPreferences.Editor editor=prefs.edit();for(int id:oldIds)editor.remove("page_"+id);for(int i=0;i<Math.min(oldIds.length,newIds.length);i++){String page=prefs.getString("page_"+oldIds[i],"");if(PageShortcut.valid(page))editor.putString("page_"+newIds[i],page);}editor.apply();onUpdate(context,AppWidgetManager.getInstance(context),newIds);}
}
