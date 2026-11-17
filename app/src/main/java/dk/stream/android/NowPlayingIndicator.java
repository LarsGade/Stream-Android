package dk.stream.android;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;

/** A reserved, accessible marker at the right edge of a playable row. */
public final class NowPlayingIndicator {
 static final int ID=View.generateViewId();
 private NowPlayingIndicator() { }
 public static View addTo(LinearLayout row) {
  Context context=row.getContext();float density=context.getResources().getDisplayMetrics().density;int size=Math.round(16*density),margin=Math.round(8*density);
  View dot=new View(context);dot.setId(ID);dot.setContentDescription(context.getString(R.string.now_playing));
  GradientDrawable shape=new GradientDrawable();shape.setShape(GradientDrawable.OVAL);shape.setColor(Color.WHITE);dot.setBackground(shape);
  LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(size,size);params.setMargins(margin,0,margin,0);row.addView(dot,params);show(dot,false);return dot;
 }
 public static void show(View dot,boolean playing) {dot.setActivated(playing);render(dot,dot.getTag() instanceof PlaybackMark?(PlaybackMark)dot.getTag():new PlaybackMark(0,0,false));}
 static void history(View dot,PlaybackMark mark){dot.setTag(mark);render(dot,mark);}
 private static void render(View dot,PlaybackMark mark){boolean playing=dot.isActivated();int color=PlaybackStatus.color(playing,mark);((GradientDrawable)dot.getBackground()).setColor(color);dot.setVisibility(color==0?View.INVISIBLE:View.VISIBLE);dot.setContentDescription(playing?dot.getContext().getString(R.string.now_playing):mark.complete?"Afspillet færdig":"Delvist afspillet");}
}
