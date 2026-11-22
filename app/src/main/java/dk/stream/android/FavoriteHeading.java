package dk.stream.android;

import android.graphics.drawable.Drawable;
import android.widget.TextView;

/** A read-only favorite marker shared by list headings. */
final class FavoriteHeading {
 static void show(TextView heading,boolean favorites){
  android.view.ViewGroup.LayoutParams layout=heading.getLayoutParams();if(layout instanceof android.widget.LinearLayout.LayoutParams){android.widget.LinearLayout.LayoutParams row=(android.widget.LinearLayout.LayoutParams)layout;row.width=android.view.ViewGroup.LayoutParams.WRAP_CONTENT;row.weight=0;heading.setLayoutParams(row);}
  Drawable[] current=heading.getCompoundDrawablesRelative();Drawable star=null;
  if(favorites){star=heading.getContext().getDrawable(R.drawable.ic_favorite);int size=Math.max(1,Math.round(heading.getPaint().getFontMetrics().descent-heading.getPaint().getFontMetrics().ascent));star.setBounds(0,0,size,size);}
  heading.setCompoundDrawablePadding(Math.round(8*heading.getResources().getDisplayMetrics().density));heading.setCompoundDrawablesRelative(current[0],current[1],star,current[3]);
  heading.setContentDescription(UiText.text(heading.getText()+(favorites?", favoritter":"")));
 }
 private FavoriteHeading(){}
}
