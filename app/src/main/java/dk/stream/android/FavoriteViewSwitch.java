package dk.stream.android;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
/** Two persistent, equally sized choices directly below each destination's search field. */
final class FavoriteViewSwitch extends LinearLayout {
 interface Listener{void selected(boolean favorites);}
 private final Button all,favorite;private boolean favorites;
 FavoriteViewSwitch(Context context,Listener listener){super(context);setOrientation(HORIZONTAL);all=create("Alle",false,listener);favorite=create("Favoritter",true,listener);addView(all,new LayoutParams(0,-1,1));View divider=new View(context);divider.setBackgroundColor(0xFF101010);addView(divider,new LayoutParams(dp(1),-1));addView(favorite,new LayoutParams(0,-1,1));setFavorites(false);}
 private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
 private Button create(String title,boolean target,Listener listener){Button button=new Button(getContext());button.setText(UiText.text(title));button.setAllCaps(false);button.setTextSize(16);button.setMinWidth(0);button.setMinimumWidth(0);button.setMinHeight(0);button.setMinimumHeight(0);button.setPadding(dp(6),0,dp(6),0);button.setStateListAnimator(null);button.setOnClickListener(view->{if(target!=favorites)listener.selected(target);});return button;}
 void setFavorites(boolean selected){favorites=selected;style(all,!selected,false);style(favorite,selected,true);}
 private void style(Button button,boolean selected,boolean right){button.setSelected(selected);button.setTextColor(selected?Color.WHITE:0xFFAAAAAA);GradientDrawable background=new GradientDrawable();background.setColor(selected?0xFF253850:0xFF333333);float radius=dp(3);background.setCornerRadii(right?new float[]{0,0,radius,radius,radius,radius,0,0}:new float[]{radius,radius,0,0,0,0,radius,radius});button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF),background,null));}
}
