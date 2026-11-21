package dk.stream.android;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.text.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;

/** Shared live search field. Remote searches can wait briefly for typing to settle. */
@android.annotation.SuppressLint("ViewConstructor")
final class MusicSearchField extends LinearLayout {
 final EditText input;
 private final Handler handler=new Handler(Looper.getMainLooper());private final Runnable pending;private boolean silent;
 MusicSearchField(Context context,Runnable search){this(context,search,0,null);}
 MusicSearchField(Context context,Runnable search,int delay,Runnable changed){
  super(context);setOrientation(HORIZONTAL);setGravity(Gravity.CENTER_VERTICAL);pending=search;GradientDrawable background=new GradientDrawable();background.setColor(0xFF252525);background.setCornerRadius(dp(10));setBackground(background);
  input=new ClearableSearchEditText(context);input.setSingleLine();input.setTextColor(Color.WHITE);input.setHintTextColor(0xFFAAAAAA);input.setTextSize(16);input.setHint("Søg");input.setBackgroundColor(Color.TRANSPARENT);input.setPadding(dp(12),dp(8),dp(12),dp(8));input.setInputType(InputType.TYPE_CLASS_TEXT);input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);addView(input,new LayoutParams(-1,dp(52)));
  input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence text,int start,int count,int after){}public void afterTextChanged(Editable text){}public void onTextChanged(CharSequence text,int start,int before,int count){if(silent)return;cancelPending();if(changed!=null)changed.run();if(delay==0||text.length()==0)search.run();else handler.postDelayed(pending,delay);}});
  input.setOnEditorActionListener((view,action,event)->{if(action==EditorInfo.IME_ACTION_SEARCH||action==EditorInfo.IME_ACTION_DONE||event!=null&&event.getKeyCode()==KeyEvent.KEYCODE_ENTER&&event.getAction()==KeyEvent.ACTION_UP){cancelPending();InputMethodManager keyboard=(InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);if(keyboard!=null)keyboard.hideSoftInputFromWindow(input.getWindowToken(),0);search.run();return true;}return false;});
 }
 void setTextSilently(String text){cancelPending();silent=true;try{input.setText(text);}finally{silent=false;}}
 void cancelPending(){handler.removeCallbacks(pending);}
 @Override protected void onDetachedFromWindow(){cancelPending();super.onDetachedFromWindow();}
 private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
