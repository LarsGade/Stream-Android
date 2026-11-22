package dk.stream.android;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.FrameLayout;

final class ProgressRows {
 private static final int LINE_ID=View.generateViewId();
 static View wrap(View row,String key,boolean eligible){FrameLayout frame=new FrameLayout(row.getContext());ViewGroup.LayoutParams original=row.getLayoutParams();if(original!=null)frame.setLayoutParams(original);int height=original==null?ViewGroup.LayoutParams.WRAP_CONTENT:original.height;frame.addView(row,new FrameLayout.LayoutParams(-1,height));Line line=new Line(row.getContext());line.setId(LINE_ID);frame.addView(line,new FrameLayout.LayoutParams(-1,Math.max(1,Math.round(2*row.getResources().getDisplayMetrics().density)),Gravity.BOTTOM));frame.setTag(row.getTag());bind(frame,key,eligible);return frame;}
 static void bind(View row,String key,boolean eligible){Line line=row.findViewById(LINE_ID);if(line!=null){line.key=key;line.eligible=PlaybackStatus.historyAllowed(key,eligible);line.dot=row.findViewById(NowPlayingIndicator.ID);line.updateDot();line.removeCallbacks(line.tick);if(line.eligible&&line.isAttachedToWindow())line.post(line.tick);line.invalidate();}}
 private static final class Line extends View {
  String key="";boolean eligible;View dot;private final Paint paint=new Paint();
  private final Runnable tick=new Runnable(){public void run(){updateDot();invalidate();postDelayed(this,1000);}};
  void updateDot(){if(dot!=null)NowPlayingIndicator.history(dot,eligible?PlaybackHistory.get(getContext(),key):new PlaybackMark(0,0,false));}
  Line(Context context){super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
  @Override protected void onDraw(Canvas canvas){if(!eligible)return;PlaybackMark mark=PlaybackHistory.get(getContext(),key);if(mark.duration<=0)return;paint.setColor(0xFF343434);canvas.drawRect(0,0,getWidth(),getHeight(),paint);paint.setColor(0xFF3686FF);canvas.drawRect(0,0,getWidth()*mark.progress()/1000f,getHeight(),paint);}
  @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(eligible)post(tick);}
  @Override protected void onDetachedFromWindow(){removeCallbacks(tick);super.onDetachedFromWindow();}
 }
 private ProgressRows(){}
}
