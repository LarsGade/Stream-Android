package dk.stream.android;
import android.content.Context;
import android.view.*;
import android.widget.*;

/** Intercepts intentional horizontal swipes across child controls, except seek bars. */
@android.annotation.SuppressLint("ViewConstructor")
class SwipePlaybackBar extends LinearLayout {
 interface Dismiss {boolean stop();}
 private Dismiss dismiss;private float startX,startY;private boolean dragging,ignore;
 SwipePlaybackBar(Context context){super(context);setClickable(true);}
 void setDismissAction(Dismiss action){dismiss=action;}
 private boolean seekBarAt(View view,float x,float y){if(view.getVisibility()!=VISIBLE)return false;int[] point=new int[2];view.getLocationOnScreen(point);if(x<point[0]||x>point[0]+view.getWidth()||y<point[1]||y>point[1]+view.getHeight())return false;if(view instanceof SeekBar)return true;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)if(seekBarAt(group.getChildAt(i),x,y))return true;}return false;}
 @Override public boolean dispatchTouchEvent(android.view.MotionEvent event){if(event.getActionMasked()==MotionEvent.ACTION_DOWN){startX=event.getRawX();startY=event.getRawY();dragging=false;ignore=seekBarAt(this,startX,startY);if(!ignore&&getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);}boolean handled=super.dispatchTouchEvent(event);if((event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL)&&getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);return handled;}
 @Override public boolean onInterceptTouchEvent(MotionEvent event){if(!ignore&&dismiss!=null&&event.getActionMasked()==MotionEvent.ACTION_MOVE&&DismissSwipe.horizontal(event.getRawX()-startX,event.getRawY()-startY,ViewConfiguration.get(getContext()).getScaledTouchSlop()))dragging=true;return dragging||super.onInterceptTouchEvent(event);}
 @Override public boolean onTouchEvent(MotionEvent event){if(!dragging&&!ignore&&dismiss!=null&&event.getActionMasked()==MotionEvent.ACTION_MOVE&&DismissSwipe.horizontal(event.getRawX()-startX,event.getRawY()-startY,ViewConfiguration.get(getContext()).getScaledTouchSlop())){dragging=true;cancelLongPress();setPressed(false);}if(!dragging)return super.onTouchEvent(event);float dx=event.getRawX()-startX,dy=event.getRawY()-startY;if(event.getActionMasked()==MotionEvent.ACTION_MOVE){setTranslationX(dx);setAlpha(Math.max(.3f,1-Math.abs(dx)/Math.max(1,getWidth())));}else if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL){boolean stop=event.getActionMasked()==MotionEvent.ACTION_UP&&DismissSwipe.dismiss(dx,dy,getWidth(),getResources().getDisplayMetrics().density);dragging=false;setTranslationX(0);setAlpha(1);if(stop&&dismiss!=null&&dismiss.stop())setVisibility(GONE);}return true;}
}
