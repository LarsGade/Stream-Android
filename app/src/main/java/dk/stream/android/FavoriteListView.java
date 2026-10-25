package dk.stream.android;

import android.content.Context;
import android.view.*;
import android.widget.ListView;

/** A stationary long press opens actions on release; moving after the hold reorders rows. */
public final class FavoriteListView extends ListView {
 public interface Listener {
  boolean enabled();void showActions(int position,View anchor);void move(int from,int to);void commit();void cancel();
 }
 private Listener listener;
 private boolean armed,dragging,touching;
 private int picked=-1;
 private float lastY,heldY;
 private final int slop,edge,scrollStep;
 private final Runnable autoScroll=new Runnable() {
  @Override public void run() {
   if(!armed||!dragging) return;
   int direction=lastY<edge?-1:lastY>getHeight()-edge?1:0;
   if(direction!=0) {scrollListBy(direction*scrollStep);moveToPointer();postDelayed(this,32);}
  }
 };
 public FavoriteListView(Context context) {
  super(context);slop=ViewConfiguration.get(context).getScaledTouchSlop();edge=Math.round(48*getResources().getDisplayMetrics().density);scrollStep=Math.round(12*getResources().getDisplayMetrics().density);
  setOnItemLongClickListener((parent,view,position,id)->{
   if(listener==null||!listener.enabled()) return false;
   if(!touching) {listener.showActions(position,view==null?this:view);return true;}
   armed=true;dragging=false;picked=position;heldY=lastY;
   if(getParent()!=null) getParent().requestDisallowInterceptTouchEvent(true);
   performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);if(view!=null) view.setAlpha(0.6f);return true;
  });
 }
 public void setFavoriteListener(Listener listener) {this.listener=listener;}
 public boolean isPicked(int position) {return armed && picked==position;}
 public void cancelFavoriteGesture() {
  if(!armed) return;clearGesture();if(listener!=null) listener.cancel();
 }
 private void clearGesture() {
  removeCallbacks(autoScroll);armed=false;dragging=false;touching=false;picked=-1;
  for(int i=0;i<getChildCount();i++) getChildAt(i).setAlpha(1f);
  if(getParent()!=null) getParent().requestDisallowInterceptTouchEvent(false);
 }
 private void moveToPointer() {
  if(getCount()==0) return;
  int target=pointToPosition(getWidth()/2,Math.max(0,Math.min(getHeight()-1,(int)lastY)));
  if(target==INVALID_POSITION) target=lastY<getHeight()/2?getFirstVisiblePosition():getLastVisiblePosition();
  if(target>=0 && target<getCount() && target!=picked) {int from=picked;picked=target;listener.move(from,target);}
 }
 @Override public boolean onTouchEvent(MotionEvent event) {
  lastY=event.getY();if(event.getActionMasked()==MotionEvent.ACTION_DOWN) touching=true;
  if(!armed) {if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL) touching=false;return super.onTouchEvent(event);}
  switch(event.getActionMasked()) {
   case MotionEvent.ACTION_MOVE:
    if(!dragging && Math.abs(lastY-heldY)>slop) dragging=true;
    if(dragging) {moveToPointer();removeCallbacks(autoScroll);if(lastY<edge || lastY>getHeight()-edge) post(autoScroll);}
    return true;
   case MotionEvent.ACTION_UP:
    boolean moved=dragging;int position=picked;View anchor=getChildAt(position-getFirstVisiblePosition());
    MotionEvent cancel=MotionEvent.obtain(event);cancel.setAction(MotionEvent.ACTION_CANCEL);super.onTouchEvent(cancel);cancel.recycle();clearGesture();
    if(moved) listener.commit();else listener.showActions(position,anchor==null?this:anchor);performClick();return true;
   case MotionEvent.ACTION_CANCEL:
    super.onTouchEvent(event);cancelFavoriteGesture();return true;
   default:return true;
  }
 }
 @Override public boolean performClick() {super.performClick();return true;}
 @Override protected void onDetachedFromWindow() {removeCallbacks(autoScroll);if(armed) cancelFavoriteGesture();super.onDetachedFromWindow();}
}
