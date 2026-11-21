package dk.stream.android;
import android.content.Context;
import androidx.appcompat.app.AlertDialog;
final class FavoritePicker {
 interface Save {boolean apply(Runnable change);}
 static void show(Context context,Library library,Save save,FavoriteLists.Item item,Runnable changed){if(item.stream!=null&&item.stream.channel.kind==Playlist.Kind.TV){int[] selected={TvFavorites.mask(library,item.stream)};if(!library.isFavorite(item.stream))selected[0]=1;boolean[] checked=new boolean[4];for(int i=0;i<4;i++)checked[i]=(selected[0]&(1<<i))!=0;new AlertDialog.Builder(context).setTitle("Vælg TV-favoritlister").setMultiChoiceItems(new String[]{"TV 1","TV 2","TV 3","TV 4"},checked,(dialog,index,on)->{if(on)selected[0]|=1<<index;else selected[0]&=~(1<<index);}).setPositiveButton("Gem",(dialog,which)->{if(save.apply(()->TvFavorites.set(library,item.stream,selected[0])))changed.run();}).setNegativeButton("Annuller",null).show();return;}java.util.List<String> available=new java.util.ArrayList<>();for(String name:library.favoriteHomeOrder)if(FavoriteLists.homeAllowed(item,name))available.add(name);String[] names=available.toArray(new String[0]);new AlertDialog.Builder(context).setTitle("Vælg favoritliste").setItems(names,(dialog,position)->{if(save.apply(()->FavoriteLists.assignHome(library,item,names[position])))changed.run();}).setNegativeButton("Annuller",null).show();}
 private FavoritePicker(){}
}
