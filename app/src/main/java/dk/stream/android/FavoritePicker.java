package dk.stream.android;
import android.content.Context;
import androidx.appcompat.app.AlertDialog;
final class FavoritePicker {
 interface Save {boolean apply(Runnable change);}
 static void show(Context context,Library library,Save save,FavoriteLists.Item item,Runnable changed){String[] names=library.favoriteHomeOrder.toArray(new String[0]);new AlertDialog.Builder(context).setTitle("Vælg favoritliste").setItems(names,(dialog,position)->{if(save.apply(()->FavoriteLists.assignHome(library,item,names[position])))changed.run();}).setNegativeButton("Annuller",null).show();}
 private FavoritePicker(){}
}
