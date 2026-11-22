package dk.stream.android;

import android.content.Context;
import android.view.View;
import android.widget.ListView;
import android.widget.PopupMenu;

/** Opens favorite actions as soon as the long press is recognized. */
public final class FavoriteListView extends ListView {
 public interface Listener {
  boolean enabled();default boolean canReorder(){return true;}void showActions(int position,View anchor);void move(int from,int to);void commit();void cancel();
 }
 private Listener listener;
 public FavoriteListView(Context context) {
  super(context);
  setOnItemLongClickListener((parent,view,position,id)->{
   if(listener==null||!listener.enabled())return false;
   listener.showActions(position,view==null?this:view);return true;
  });
 }
 public void setFavoriteListener(Listener listener){this.listener=listener;}
 public boolean isPicked(int position){return false;}
 public void cancelFavoriteGesture(){}
 public void addReorderActions(PopupMenu menu,int position){
  if(listener==null||!listener.canReorder())return;
  if(position>0)menu.getMenu().add(0,8101,10,"Flyt op").setOnMenuItemClickListener(item->{listener.move(position,position-1);listener.commit();return true;});
  if(position<getCount()-1)menu.getMenu().add(0,8102,11,"Flyt ned").setOnMenuItemClickListener(item->{listener.move(position,position+1);listener.commit();return true;});
 }
}
