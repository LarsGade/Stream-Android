package dk.stream.android;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;

/** A fixed navigation area below page contents and the mini player. */
@android.annotation.SuppressLint("ViewConstructor")
final class PageFooter extends FrameLayout {
 private final ImageButton button;private boolean onHome;
 PageFooter(Context context,Runnable home,Runnable menu){super(context);setBackgroundColor(0xFF101010);button=new ImageButton(context);button.setImageTintList(ColorStateList.valueOf(0xFF252525));button.setBackgroundColor(Color.TRANSPARENT);int padding=dp(12);button.setPadding(padding,padding,padding,padding);addView(button,new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.CENTER));button.setOnClickListener(view->{if(onHome&&menu!=null)menu.run();else home.run();});setOnHome(false);}
 void setOnHome(boolean value){onHome=value;button.setImageResource(value?R.drawable.ic_menu:R.drawable.ic_home);button.setContentDescription(value?getContext().getString(R.string.open_menu):UiText.text("Tilbage til forsiden"));}
 private int dp(int size){return Math.round(size*getResources().getDisplayMetrics().density);}
}
