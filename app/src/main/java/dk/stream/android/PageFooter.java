package dk.stream.android;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
/** Shared bottom branding and navigation, with the original centered menu on home. */
@android.annotation.SuppressLint("ViewConstructor")
final class PageFooter extends FrameLayout {
 private final ImageButton homeButton,menuButton;private final ImageView brand;
 PageFooter(Context context,Runnable home,Runnable menu){super(context);setBackgroundColor(0xFF101010);brand=new ImageView(context);brand.setImageResource(R.drawable.brand_banner);brand.setContentDescription(context.getString(R.string.app_name));brand.setScaleType(ImageView.ScaleType.FIT_CENTER);FrameLayout.LayoutParams logo=new FrameLayout.LayoutParams(dp(140),dp(40),Gravity.CENTER);addView(brand,logo);homeButton=button(R.drawable.ic_home,UiText.text("Tilbage til forsiden"),home);addView(homeButton,new LayoutParams(dp(48),dp(48),Gravity.START|Gravity.CENTER_VERTICAL));menuButton=button(R.drawable.ic_menu,context.getString(R.string.open_menu),menu==null?home:menu);addView(menuButton,new LayoutParams(dp(48),dp(48),Gravity.END|Gravity.CENTER_VERTICAL));setOnHome(false);}
 private ImageButton button(int icon,String label,Runnable action){ImageButton button=new ImageButton(getContext());button.setImageResource(icon);button.setContentDescription(label);button.setImageTintList(ColorStateList.valueOf(0xFF252525));button.setBackgroundColor(Color.TRANSPARENT);button.setPadding(dp(12),dp(12),dp(12),dp(12));button.setOnClickListener(view->action.run());return button;}
 void setOnHome(boolean home){brand.setVisibility(home?GONE:VISIBLE);homeButton.setVisibility(home?GONE:VISIBLE);LayoutParams params=(LayoutParams)menuButton.getLayoutParams();params.gravity=home?Gravity.CENTER:Gravity.END|Gravity.CENTER_VERTICAL;menuButton.setLayoutParams(params);}
 private int dp(int size){return Math.round(size*getResources().getDisplayMetrics().density);}
}
