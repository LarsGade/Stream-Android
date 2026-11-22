package dk.stream.android;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.*;
import java.util.*;

@androidx.media3.common.util.UnstableApi
public final class PageWidgetConfigureActivity extends AppCompatActivity {
 @Override public void onCreate(Bundle state){super.onCreate(state);setResult(RESULT_CANCELED);int id=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);if(id==AppWidgetManager.INVALID_APPWIDGET_ID||!PageWidgetProvider.owns(this,id)){finish();return;}
  try{List<String> pages=PageShortcut.choices(new LibraryStore(this).load());String saved=PageWidgetProvider.page(this,id);int[] selected={Math.max(0,pages.indexOf(saved))};if(state!=null){String restored=state.getString("page");if(pages.contains(restored))selected[0]=pages.indexOf(restored);}if(pages.isEmpty()){finish();return;}
   AlertDialog dialog=new AlertDialog.Builder(this).setTitle(R.string.page_widget_choose).setSingleChoiceItems(UiText.labels(pages.toArray(new String[0])),selected[0],(picker,index)->selected[0]=index).setNegativeButton(android.R.string.cancel,(picker,which)->finish()).setPositiveButton(R.string.save,null).setOnCancelListener(picker->finish()).create();
   dialog.setOnShowListener(shown->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view->{if(!PageWidgetProvider.bind(this,id,pages.get(selected[0]))){UiText.toast(this,R.string.storage_failed,android.widget.Toast.LENGTH_LONG).show();return;}PageWidgetProvider.update(this,AppWidgetManager.getInstance(this),id);setResult(RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id));finish();}));selection=()->pages.get(selected[0]);dialog.show();
  }catch(Exception failure){new AlertDialog.Builder(this).setMessage(R.string.storage_failed).setPositiveButton(android.R.string.ok,(dialog,which)->finish()).setOnCancelListener(dialog->finish()).show();}
 }
 private interface Selection {String page();}private Selection selection;
 @Override protected void onSaveInstanceState(Bundle state){if(selection!=null)state.putString("page",selection.page());super.onSaveInstanceState(state);}
}
