package dk.stream.android;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;

/** A reserved, accessible marker at the right edge of a playable row. */
public final class NowPlayingIndicator {
 private NowPlayingIndicator() { }
 public static View addTo(LinearLayout row) {
  Context context=row.getContext();float density=context.getResources().getDisplayMetrics().density;int size=Math.round(16*density),margin=Math.round(8*density);
  View dot=new View(context);dot.setTag("now_playing_dot");dot.setContentDescription(context.getString(R.string.now_playing));
  GradientDrawable shape=new GradientDrawable();shape.setShape(GradientDrawable.OVAL);shape.setColor(Color.WHITE);dot.setBackground(shape);
  LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(size,size);params.setMargins(margin,0,margin,0);row.addView(dot,params);show(dot,false);return dot;
 }
 public static void show(View dot,boolean playing) {dot.setVisibility(playing?View.VISIBLE:View.INVISIBLE);}
}
