package dk.stream.android;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.*;

@androidx.media3.common.util.UnstableApi
public final class RadioWidgetConfigureActivity extends AppCompatActivity {
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);setResult(RESULT_CANCELED);
  int widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
  if(widgetId==AppWidgetManager.INVALID_APPWIDGET_ID) {finish();return;}
  try {
   Library library=new LibraryStore(this).load();
   if(library.radioFavorites.isEmpty()) {new AlertDialog.Builder(this).setTitle(R.string.radio_widget).setMessage(R.string.radio_favorites_empty).setPositiveButton(android.R.string.ok,(d,w)->finish()).setOnCancelListener(d->finish()).show();return;}
   String[] names=new String[library.radioFavorites.size()];for(int i=0;i<names.length;i++) names[i]=library.radioFavorites.get(i).displayName();
   new AlertDialog.Builder(this).setTitle(R.string.radio_widget_choose).setItems(names,(d,which)->{
    RadioWidgetProvider.bind(this,widgetId,library.radioFavorites.get(which).id);RadioWidgetProvider.update(this,AppWidgetManager.getInstance(this),widgetId);
    setResult(RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId));finish();
   }).setNegativeButton(android.R.string.cancel,(d,w)->finish()).setOnCancelListener(d->finish()).show();
  } catch(Exception failure) {new AlertDialog.Builder(this).setMessage(R.string.storage_failed).setPositiveButton(android.R.string.ok,(d,w)->finish()).show();}
 }
}
