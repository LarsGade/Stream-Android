package dk.stream.android;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.*;
import java.util.*;
@androidx.media3.common.util.UnstableApi
public final class RadioQueueWidgetConfigureActivity extends AppCompatActivity {
 private final Set<String> selected=new LinkedHashSet<>();
 @Override public void onCreate(Bundle saved){super.onCreate(saved);setResult(RESULT_CANCELED);int widget=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);if(widget==AppWidgetManager.INVALID_APPWIDGET_ID||!RadioQueueWidgetProvider.owns(this,widget)){finish();return;}
  try{List<RadioStation> favorites=RadioFavoriteNavigation.stations(new LibraryStore(this).load());if(favorites.isEmpty()){new AlertDialog.Builder(this).setTitle(R.string.radio_widget_queue).setMessage(R.string.radio_favorites_empty).setPositiveButton(android.R.string.ok,(dialog,which)->finish()).setOnCancelListener(dialog->finish()).show();return;}
   if(saved!=null&&saved.getStringArrayList("selected")!=null)selected.addAll(saved.getStringArrayList("selected"));else selected.addAll(RadioQueueWidgetPreferences.ids(this,widget));String[] names=new String[favorites.size()];boolean[] checked=new boolean[names.length];for(int i=0;i<names.length;i++){names[i]=favorites.get(i).displayName();checked[i]=selected.contains(favorites.get(i).id);}
   AlertDialog dialog=new AlertDialog.Builder(this).setTitle(R.string.radio_widget_queue_choose).setMultiChoiceItems(UiText.labels(names),checked,(picker,index,on)->{String id=favorites.get(index).id;if(on)selected.add(id);else selected.remove(id);}).setNegativeButton(android.R.string.cancel,(picker,which)->finish()).setPositiveButton(UiText.text("Gem"),null).setOnCancelListener(picker->finish()).create();dialog.setOnShowListener(shown->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view->{List<String> ids=new ArrayList<>();for(RadioStation station:favorites)if(selected.contains(station.id))ids.add(station.id);if(ids.isEmpty()){UiText.toast(this,"Vælg mindst én radiokanal.",android.widget.Toast.LENGTH_SHORT).show();return;}try{RadioQueueWidgetPreferences.bind(this,widget,ids);RadioQueueWidgetProvider.update(this,AppWidgetManager.getInstance(this),widget);setResult(RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widget));finish();}catch(Exception failure){UiText.toast(this,R.string.storage_failed,android.widget.Toast.LENGTH_LONG).show();}}));dialog.show();
  }catch(Exception failure){new AlertDialog.Builder(this).setMessage(R.string.storage_failed).setPositiveButton(android.R.string.ok,(dialog,which)->finish()).show();}
 }
 @Override protected void onSaveInstanceState(Bundle state){state.putStringArrayList("selected",new ArrayList<>(selected));super.onSaveInstanceState(state);}
}
