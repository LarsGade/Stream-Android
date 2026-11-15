package dk.stream.android;

import android.content.Context;
import android.view.View;
import android.widget.RadioGroup;
import java.util.*;

/** Radio buttons remain direct children so checked state spans every wrapped row. */
public final class FavoriteTabGroup extends RadioGroup {
 public FavoriteTabGroup(Context context){super(context);setOrientation(HORIZONTAL);}
 @Override protected void onMeasure(int widthSpec,int heightSpec){
  int width=MeasureSpec.getSize(widthSpec),available=Math.max(0,width-getPaddingLeft()-getPaddingRight());int lineWidth=0,lineHeight=0,height=getPaddingTop()+getPaddingBottom();
  for(int i=0;i<getChildCount();i++){View child=getChildAt(i);if(child.getVisibility()==GONE)continue;child.measure(MeasureSpec.makeMeasureSpec(available,MeasureSpec.AT_MOST),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));int w=child.getMeasuredWidth(),h=child.getMeasuredHeight();if(lineWidth>0&&lineWidth+w>available){height+=lineHeight;lineWidth=0;lineHeight=0;}lineWidth+=w;lineHeight=Math.max(lineHeight,h);}
  setMeasuredDimension(resolveSize(width,widthSpec),resolveSize(height+lineHeight,heightSpec));
 }
 @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
  int available=right-left-getPaddingLeft()-getPaddingRight(),y=getPaddingTop(),width=0,height=0;List<View> row=new ArrayList<>();
  for(int i=0;i<getChildCount();i++){View child=getChildAt(i);if(child.getVisibility()==GONE)continue;int w=child.getMeasuredWidth();if(!row.isEmpty()&&width+w>available){layoutRow(row,width,height,available,y);y+=height;row.clear();width=height=0;}row.add(child);width+=w;height=Math.max(height,child.getMeasuredHeight());}layoutRow(row,width,height,available,y);
 }
 private void layoutRow(List<View> row,int width,int height,int available,int y){int x=getPaddingLeft()+(available-width)/2;for(View child:row){int cy=y+(height-child.getMeasuredHeight())/2;child.layout(x,cy,x+child.getMeasuredWidth(),cy+child.getMeasuredHeight());x+=child.getMeasuredWidth();}}
}
