package dk.stream.android;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
/** Divide the actual home viewport equally among its currently visible cards. */
final class FavoriteHomeLayout extends LinearLayout {
 FavoriteHomeLayout(Context context){super(context);setOrientation(VERTICAL);}
 @Override protected void onMeasure(int widthSpec,int heightSpec){
  int count=0,gaps=0;for(int i=0;i<getChildCount();i++){View child=getChildAt(i);if(child.getVisibility()==GONE)continue;count++;LayoutParams params=(LayoutParams)child.getLayoutParams();gaps+=params.topMargin+params.bottomMargin;}
  if(count>0&&MeasureSpec.getMode(heightSpec)!=MeasureSpec.UNSPECIFIED){int available=Math.max(0,MeasureSpec.getSize(heightSpec)-getPaddingTop()-getPaddingBottom()-gaps),index=0;for(int i=0;i<getChildCount();i++){View child=getChildAt(i);if(child.getVisibility()==GONE)continue;LayoutParams params=(LayoutParams)child.getLayoutParams();params.height=available/count+(index++<available%count?1:0);params.weight=0;}}
  super.onMeasure(widthSpec,heightSpec);
 }
}
