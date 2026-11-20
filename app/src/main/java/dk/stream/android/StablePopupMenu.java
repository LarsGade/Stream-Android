package dk.stream.android;

import android.content.Context;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.PopupMenu;

/** A list refresh may replace its rows; anchor menus to a fixed point in the window instead. */
final class StablePopupMenu extends PopupMenu {
 private final Pin pin;
 private OnDismissListener dismissed;
 StablePopupMenu(Context context,View row){this(context,new Pin(row));}
 private StablePopupMenu(Context context,Pin pin){super(context,pin.view);this.pin=pin;super.setOnDismissListener(menu->{pin.remove();if(dismissed!=null)dismissed.onDismiss(menu);});}
 @Override public void setOnDismissListener(OnDismissListener listener){dismissed=listener;}
 @Override public void show(){pin.attach();try{super.show();}catch(RuntimeException error){pin.remove();throw error;}}
 private static final class Pin {
  final View view;final FrameLayout root;final int left,top,width,height;
  Pin(View row){View window=row.getRootView();if(window instanceof FrameLayout){root=(FrameLayout)window;int[] anchor=new int[2],origin=new int[2];row.getLocationOnScreen(anchor);root.getLocationOnScreen(origin);left=anchor[0]-origin[0];top=anchor[1]-origin[1];width=Math.max(1,row.getWidth());height=Math.max(1,row.getHeight());view=new View(row.getContext());view.setAlpha(0);view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);view.setClickable(false);view.setFocusable(false);}else{root=null;view=row;left=top=width=height=0;}}
  void attach(){if(root==null||view.getParent()!=null)return;FrameLayout.LayoutParams position=new FrameLayout.LayoutParams(width,height,Gravity.TOP|Gravity.LEFT);position.leftMargin=left-root.getPaddingLeft();position.topMargin=top-root.getPaddingTop();root.addView(view,position);view.layout(left,top,left+width,top+height);}
  void remove(){if(root!=null&&view.getParent()==root)root.removeView(view);}
 }
}
