package dk.stream.android;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;

/** Search input with an accessible clear action and a generous touch target. */
@android.annotation.SuppressLint("ViewConstructor")
final class ClearableSearchEditText extends androidx.appcompat.widget.AppCompatEditText {
 private final Drawable clearIcon;private boolean clearing;
 ClearableSearchEditText(Context context){
  super(context);clearIcon=context.getDrawable(R.drawable.ic_clear_search);int size=dp(24);clearIcon.setBounds(0,0,size,size);setCompoundDrawablePadding(dp(8));
  addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence text,int start,int count,int after){}public void onTextChanged(CharSequence text,int start,int before,int count){updateIcon();}public void afterTextChanged(Editable text){}});updateIcon();
 }
 private boolean hasQuery(){return getText()!=null&&getText().length()>0;}
 private void updateIcon(){Drawable[] icons=getCompoundDrawablesRelative();setCompoundDrawablesRelative(icons[0],icons[1],hasQuery()?clearIcon:null,icons[3]);}
 private boolean inClearArea(MotionEvent event){return hasQuery()&&event.getX()>=0&&event.getX()<=getWidth()&&event.getY()>=0&&event.getY()<=getHeight()&&(getLayoutDirection()==View.LAYOUT_DIRECTION_RTL?event.getX()<=getPaddingLeft()+dp(48):event.getX()>=getWidth()-getPaddingRight()-dp(48));}
 @Override public boolean onTouchEvent(MotionEvent event){
  if(event.getActionMasked()==MotionEvent.ACTION_DOWN&&inClearArea(event)){clearing=true;return true;}
  if(clearing){if(event.getActionMasked()==MotionEvent.ACTION_UP){clearing=false;if(inClearArea(event)){setText("");performClick();}return true;}if(event.getActionMasked()==MotionEvent.ACTION_CANCEL)clearing=false;return true;}
  return super.onTouchEvent(event);
 }
 @Override public boolean performClick(){return super.performClick();}
 @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);if(hasQuery())info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.action_clear_search,UiText.text("Ryd søgning")));}
 @Override public boolean performAccessibilityAction(int action,Bundle args){if(action==R.id.action_clear_search&&hasQuery()){setText("");return true;}return super.performAccessibilityAction(action,args);}
 private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
