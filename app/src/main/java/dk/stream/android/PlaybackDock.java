package dk.stream.android;
import android.content.Context;
import android.view.*;
import android.widget.FrameLayout;

/** Overlap the footer on browsing pages, but preserve both rows on the home page. */
@android.annotation.SuppressLint("ViewConstructor")
final class PlaybackDock extends FrameLayout {
 private final View playback;private final int footerHeight;
 PlaybackDock(Context context,PageFooter footer,View playback){super(context);this.playback=playback;footerHeight=Math.round(48*getResources().getDisplayMetrics().density);addView(footer,new LayoutParams(-1,footerHeight,Gravity.BOTTOM));addView(playback,new LayoutParams(-1,-2,Gravity.BOTTOM));}
 void setOnHome(boolean home){LayoutParams params=(LayoutParams)playback.getLayoutParams();int margin=home?footerHeight:0;if(params.bottomMargin!=margin){params.bottomMargin=margin;playback.setLayoutParams(params);}}
}
