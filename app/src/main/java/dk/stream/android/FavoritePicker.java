package dk.stream.android;
import android.content.Context;
import androidx.appcompat.app.AlertDialog;
final class FavoritePicker {
 interface Save {boolean apply(Runnable change);}
 static void show(Context context,Library library,Save save,FavoriteLists.Item item,Runnable changed){new AlertDialog.Builder(context).setTitle("Vælg favoritliste").setItems(FavoriteLists.NAMES,(dialog,position)->{if(position==0)new AlertDialog.Builder(context).setTitle("Vælg streaming-favoritter").setItems(new String[]{"TV","Film","Serier"},(choice,kind)->{if(save.apply(()->FavoriteLists.assignStreaming(library,item,kind)))changed.run();}).setNegativeButton("Annuller",null).show();else if(save.apply(()->FavoriteLists.assign(library,item,FavoriteLists.NAMES[position])))changed.run();}).setNegativeButton("Annuller",null).show();}
 private FavoritePicker(){}
}
