package dk.stream.android;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

/** Shared search box used by Music and the other home destinations. */
@android.annotation.SuppressLint("ViewConstructor")
final class MusicSearchField extends LinearLayout {
 final EditText input;
 MusicSearchField(Context context,Runnable search){
  super(context);setOrientation(HORIZONTAL);setGravity(Gravity.CENTER_VERTICAL);GradientDrawable background=new GradientDrawable();background.setColor(0xFF252525);background.setCornerRadius(dp(10));setBackground(background);
  input=new EditText(context);input.setSingleLine();input.setTextColor(Color.WHITE);input.setHintTextColor(0xFFAAAAAA);input.setTextSize(16);input.setHint("Søg");input.setBackgroundColor(Color.TRANSPARENT);input.setPadding(dp(12),dp(8),0,dp(8));input.setInputType(InputType.TYPE_CLASS_TEXT);input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);addView(input,new LayoutParams(0,dp(52),1));
  Runnable submit=()->{InputMethodManager keyboard=(InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);if(keyboard!=null)keyboard.hideSoftInputFromWindow(input.getWindowToken(),0);search.run();};
  Button button=new Button(context);button.setText("Søg");button.setAllCaps(false);button.setTextColor(Color.WHITE);button.setBackgroundTintList(ColorStateList.valueOf(0xFF253850));button.setOnClickListener(view->submit.run());addView(button,new LayoutParams(dp(68),dp(48)));
  input.setOnEditorActionListener((view,action,event)->{if(action==EditorInfo.IME_ACTION_SEARCH){submit.run();return true;}return false;});
 }
 private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
