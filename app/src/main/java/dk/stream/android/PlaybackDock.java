package dk.stream.android;
import android.content.Context;
import android.view.*;
import android.widget.FrameLayout;

/** Keep playback above the navigation footer on every page. */
@android.annotation.SuppressLint("ViewConstructor")
final class PlaybackDock extends FrameLayout {
 private final View playback;private final int footerHeight;
 PlaybackDock(Context context,PageFooter footer,View playback){super(context);this.playback=playback;footerHeight=Math.round(48*getResources().getDisplayMetrics().density);addView(footer,new LayoutParams(-1,footerHeight,Gravity.BOTTOM));LayoutParams playerPosition=new LayoutParams(-1,-2,Gravity.BOTTOM);playerPosition.bottomMargin=footerHeight;addView(playback,playerPosition);}
 void setOnHome(boolean home){LayoutParams params=(LayoutParams)playback.getLayoutParams();int margin=footerHeight;if(params.bottomMargin!=margin){params.bottomMargin=margin;playback.setLayoutParams(params);}}
}
