package dk.stream.android;

import android.graphics.drawable.Drawable;
import android.widget.TextView;

/** A read-only favorite marker shared by list headings. */
final class FavoriteHeading {
 static void show(TextView heading,boolean favorites){
  Drawable[] current=heading.getCompoundDrawablesRelative();Drawable star=null;
  if(favorites){star=heading.getContext().getDrawable(R.drawable.ic_favorite);int size=Math.round(24*heading.getResources().getDisplayMetrics().density);star.setBounds(0,0,size,size);}
  heading.setCompoundDrawablePadding(MediaListStyle.iconGap(heading));heading.setCompoundDrawablesRelative(current[0],current[1],star,current[3]);
  heading.setContentDescription(heading.getText()+(favorites?", favoritter":""));
 }
 private FavoriteHeading(){}
}
