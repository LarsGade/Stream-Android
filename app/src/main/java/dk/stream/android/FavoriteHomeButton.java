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
  GradientDrawable mask=new GradientDrawable();mask.setColor(Color.WHITE);mask.setCornerRadius(dp(5));setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF),new Card(icon),mask));androidx.core.view.ViewCompat.setBackgroundTintList(this,null);androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this,18,28,1,TypedValue.COMPLEX_UNIT_SP);
 }
 private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
 @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight){super.onSizeChanged(width,height,oldWidth,oldHeight);setPadding(Math.round(width*0.33f)+dp(20),0,dp(16),0);}

 private final class Card extends Drawable {
  private final int icon;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path clip=new Path();private final RectF rect=new RectF();private int alpha=255;private ColorFilter filter;
  Card(int icon){this.icon=icon;}
  @Override public void draw(Canvas canvas){
   Rect bounds=getBounds();float width=bounds.width(),height=bounds.height(),radius=dp(5),stroke=dp(0.8f);canvas.save();canvas.translate(bounds.left,bounds.top);rect.set(stroke/2,stroke/2,width-stroke/2,height-stroke/2);clip.reset();clip.addRoundRect(rect,radius,radius,Path.Direction.CW);canvas.clipPath(clip);paint.setAlpha(alpha);paint.setColorFilter(filter);paint.setStyle(Paint.Style.FILL);
   paint.setShader(new LinearGradient(0,0,width,height,new int[]{0xFF252525,0xFF1E1E1E},null,Shader.TileMode.CLAMP));canvas.drawRect(0,0,width,height,paint);
   paint.setShader(null);
   paint.setColor(0xFF626262);paint.setStrokeWidth(dp(0.8f));canvas.drawLine(width*0.33f,height*0.20f,width*0.33f,height*0.80f,paint);
   float size=Math.min(height*0.70f,width*0.24f);canvas.save();canvas.translate(width*0.18f-size/2,(height-size)/2);canvas.scale(size/24,size/24);drawIcon(canvas);canvas.restore();
   paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(stroke);paint.setShader(new LinearGradient(0,0,width,height,new int[]{0xFF494949,0xFF353535,0xFF414141},new float[]{0,0.58f,1},Shader.TileMode.CLAMP));canvas.drawRoundRect(rect,radius,radius,paint);paint.setShader(null);paint.setStyle(Paint.Style.FILL);canvas.restore();
  }
  private void drawIcon(Canvas canvas){
   paint.setShader(new LinearGradient(2,1,22,23,new int[]{0xFFF4F9FF,0xFFB5DCFF,0xFF69B5FF,0xFF2797FF,0xFF087AFF},new float[]{0,0.22f,0.48f,0.74f,1},Shader.TileMode.CLAMP));paint.setStrokeWidth(1.65f);paint.setStrokeCap(Paint.Cap.SQUARE);paint.setStrokeJoin(Paint.Join.BEVEL);paint.setStyle(Paint.Style.FILL);Path path=new Path();
   if(icon==0){paint.setStyle(Paint.Style.STROKE);canvas.drawRoundRect(new RectF(2,4,22,18),1.1f,1.1f,paint);canvas.drawLine(12,18,12,22,paint);canvas.drawLine(7.5f,22,16.5f,22,paint);}
   else if(icon==4){paint.setStyle(Paint.Style.STROKE);canvas.drawRoundRect(new RectF(2,9,22,21),0.9f,0.9f,paint);canvas.save();canvas.rotate(-15,2,9);canvas.drawRoundRect(new RectF(2,4,22,9),0.5f,0.5f,paint);canvas.drawLine(7,4,5,9,paint);canvas.drawLine(13,4,11,9,paint);canvas.drawLine(19,4,17,9,paint);canvas.restore();canvas.drawLine(2,13,22,13,paint);}
   else if(icon==5){paint.setStyle(Paint.Style.STROKE);canvas.drawLine(7,2,18,2,paint);canvas.drawLine(5,5,20,5,paint);canvas.drawRoundRect(new RectF(2,8,22,22),1,1,paint);paint.setStyle(Paint.Style.FILL);path.moveTo(10,11.5f);path.quadTo(9,11,9,12);path.lineTo(9,18);path.quadTo(9,19,10,18.5f);path.lineTo(15,15.5f);path.quadTo(16,15,15,14.5f);path.close();canvas.drawPath(path,paint);}
   else if(icon==1){paint.setStyle(Paint.Style.STROKE);canvas.drawRoundRect(new RectF(2,8,22,21),1.2f,1.2f,paint);canvas.drawLine(6,7,18,2,paint);canvas.drawLine(5.5f,12.5f,10.3f,12.5f,paint);canvas.drawLine(5.5f,16,10.3f,16,paint);paint.setStyle(Paint.Style.FILL);canvas.drawCircle(16.5f,14.5f,2.6f,paint);canvas.drawCircle(18,2,1.1f,paint);}
   else if(icon==2){canvas.drawRoundRect(new RectF(8,1,16,16),2,2,paint);paint.setStyle(Paint.Style.STROKE);path.moveTo(5,11);path.lineTo(5,12);path.lineTo(5,16);path.quadTo(5,19,8,19);path.lineTo(16,19);path.quadTo(19,19,19,16);path.lineTo(19,12);path.lineTo(19,11);canvas.drawPath(path,paint);canvas.drawLine(12,19,12,23,paint);canvas.drawLine(9,23,15,23,paint);}
   else if(icon==6){paint.setStyle(Paint.Style.STROKE);path.moveTo(2,6);path.lineTo(9,6);path.lineTo(11,9);path.lineTo(22,9);path.lineTo(22,21);path.lineTo(2,21);path.close();canvas.drawPath(path,paint);}
   else if(icon==7){paint.setStyle(Paint.Style.STROKE);path.moveTo(6,19);path.cubicTo(0,19,0,11,6,10);path.cubicTo(6,2,18,2,19,10);path.cubicTo(25,11,24,19,19,19);path.close();canvas.drawPath(path,paint);}
   else {path.moveTo(8,19.5f);path.lineTo(8,5.5f);path.quadTo(8,4.5f,9,4);path.lineTo(20,1);path.quadTo(21,0.5f,21,2);path.lineTo(21,17);path.lineTo(18.6f,17);path.lineTo(18.6f,5.5f);path.lineTo(10.5f,7.7f);path.lineTo(10.5f,19.5f);path.close();canvas.drawPath(path,paint);canvas.drawRoundRect(new RectF(4,17,10.5f,22),1.5f,1.5f,paint);canvas.drawRoundRect(new RectF(14.6f,14.5f,21,19.5f),1.5f,1.5f,paint);}
   paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setStrokeCap(Paint.Cap.BUTT);
  }
  @Override public void setAlpha(int value){alpha=value;invalidateSelf();}
  @Override public void setColorFilter(ColorFilter value){filter=value;invalidateSelf();}
  @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
 }
}
