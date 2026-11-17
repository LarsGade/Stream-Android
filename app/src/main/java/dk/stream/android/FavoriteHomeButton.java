package dk.stream.android;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.util.TypedValue;
import android.view.Gravity;

/** Scalable favorite category card with white/blue icons on a neutral dark surface. */
@android.annotation.SuppressLint("ViewConstructor")
final class FavoriteHomeButton extends androidx.appcompat.widget.AppCompatButton {
 FavoriteHomeButton(Context context,String label,int icon){
  super(context);setText(label);setAllCaps(false);setTextColor(Color.WHITE);setTypeface(Typeface.create("sans-serif",Typeface.BOLD));setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);setMaxLines(1);setEllipsize(android.text.TextUtils.TruncateAt.END);setMinHeight(0);setMinimumHeight(0);setMinWidth(0);setMinimumWidth(0);setStateListAnimator(null);setElevation(0);
  GradientDrawable mask=new GradientDrawable();mask.setColor(Color.WHITE);mask.setCornerRadius(dp(18));setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF),new Card(icon),mask));androidx.core.view.ViewCompat.setBackgroundTintList(this,null);androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this,18,28,1,TypedValue.COMPLEX_UNIT_SP);
 }
 private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
 @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight){super.onSizeChanged(width,height,oldWidth,oldHeight);setPadding(Math.round(width*0.33f)+dp(20),0,dp(16),0);}
 @Override protected void onMeasure(int widthSpec,int heightSpec){int width=MeasureSpec.getSize(widthSpec);int height=Math.max(dp(80),Math.round(width/4.32f));super.onMeasure(widthSpec,MeasureSpec.getMode(heightSpec)==MeasureSpec.EXACTLY?heightSpec:MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY));}
 private final class Card extends Drawable {
  private final int icon;private final Drawable brand;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path clip=new Path();private final RectF rect=new RectF();private int alpha=255;private ColorFilter filter;
  Card(int icon){this.icon=icon;this.brand=icon==0?getContext().getDrawable(R.drawable.brand_icon).mutate():null;}
  @Override public void draw(Canvas canvas){
   Rect bounds=getBounds();float width=bounds.width(),height=bounds.height(),radius=dp(18),stroke=dp(0.8f);canvas.save();canvas.translate(bounds.left,bounds.top);rect.set(stroke/2,stroke/2,width-stroke/2,height-stroke/2);clip.reset();clip.addRoundRect(rect,radius,radius,Path.Direction.CW);canvas.clipPath(clip);paint.setAlpha(alpha);paint.setColorFilter(filter);paint.setStyle(Paint.Style.FILL);
   paint.setShader(new LinearGradient(0,0,width,height,new int[]{0xFF252525,0xFF1E1E1E},null,Shader.TileMode.CLAMP));canvas.drawRect(0,0,width,height,paint);
   paint.setShader(null);
   paint.setColor(0xFF626262);paint.setStrokeWidth(dp(0.8f));canvas.drawLine(width*0.33f,height*0.20f,width*0.33f,height*0.80f,paint);
   float size=height*0.70f;canvas.save();canvas.translate(width*0.18f-size/2,(height-size)/2);canvas.scale(size/24,size/24);drawIcon(canvas);canvas.restore();
   paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(stroke);paint.setShader(new LinearGradient(0,0,width,height,new int[]{0xFF494949,0xFF353535,0xFF414141},new float[]{0,0.58f,1},Shader.TileMode.CLAMP));canvas.drawRoundRect(rect,radius,radius,paint);paint.setShader(null);paint.setStyle(Paint.Style.FILL);canvas.restore();
  }
  private void drawIcon(Canvas canvas){
   paint.setShader(new LinearGradient(0,1,14,23,new int[]{0xFFF4FAFF,0xFFB9D4FF,0xFF1478F8},null,Shader.TileMode.CLAMP));paint.setStrokeWidth(1.65f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStyle(Paint.Style.FILL);Path path=new Path();
   if(icon==0){brand.setBounds(2,2,22,22);brand.setAlpha(alpha);brand.setColorFilter(filter);brand.draw(canvas);}
   else if(icon==1){paint.setStyle(Paint.Style.STROKE);canvas.drawRoundRect(new RectF(2,8,22,21),2.3f,2.3f,paint);canvas.drawLine(6,7,18,2,paint);canvas.drawLine(5.5f,12.5f,10.3f,12.5f,paint);canvas.drawLine(5.5f,16,10.3f,16,paint);paint.setStyle(Paint.Style.FILL);canvas.drawCircle(16.5f,14.5f,2.6f,paint);canvas.drawCircle(18,2,1.1f,paint);}
   else if(icon==2){canvas.drawRoundRect(new RectF(8,1,16,16),4,4,paint);paint.setStyle(Paint.Style.STROKE);path.moveTo(5,11);path.lineTo(5,12);path.cubicTo(5,21.3f,19,21.3f,19,12);path.lineTo(19,11);canvas.drawPath(path,paint);canvas.drawLine(12,19,12,23,paint);canvas.drawLine(9,23,15,23,paint);}
   else {path.moveTo(8,19.5f);path.lineTo(8,5.5f);path.quadTo(8,4.5f,9,4);path.lineTo(20,1);path.quadTo(21,0.5f,21,2);path.lineTo(21,17);path.lineTo(18.6f,17);path.lineTo(18.6f,5.5f);path.lineTo(10.5f,7.7f);path.lineTo(10.5f,19.5f);path.close();canvas.drawPath(path,paint);canvas.drawOval(new RectF(4,17,10.5f,22),paint);canvas.drawOval(new RectF(14.6f,14.5f,21,19.5f),paint);}
   paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setStrokeCap(Paint.Cap.BUTT);
  }
  @Override public void setAlpha(int value){alpha=value;invalidateSelf();}
  @Override public void setColorFilter(ColorFilter value){filter=value;invalidateSelf();}
  @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
 }
}
