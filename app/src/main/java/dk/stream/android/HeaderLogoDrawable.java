package dk.stream.android;

import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Reuses the launcher artwork verbatim and colors the original wordmark's alpha mask. */
final class HeaderLogoDrawable extends Drawable {
 private final Bitmap icon,banner;
 private final Rect iconSource;
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 HeaderLogoDrawable(Resources resources){icon=BitmapFactory.decodeResource(resources,R.drawable.brand_icon);banner=BitmapFactory.decodeResource(resources,R.drawable.brand_banner);int left=icon.getWidth(),top=icon.getHeight(),right=0,bottom=0;for(int y=0;y<icon.getHeight();y++)for(int x=0;x<icon.getWidth();x++)if(Color.alpha(icon.getPixel(x,y))>16){left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x+1);bottom=Math.max(bottom,y+1);}iconSource=new Rect(left,top,right,bottom);}
 @Override public int getIntrinsicWidth(){return 1005;}
 @Override public int getIntrinsicHeight(){return 378;}
 @Override public void draw(Canvas canvas){Rect bounds=getBounds();canvas.save();canvas.translate(bounds.left,bounds.top);canvas.scale(bounds.width()/1005f,bounds.height()/378f);
  canvas.drawBitmap(icon,iconSource,new RectF(62,67,279,312),paint);
  int layer=canvas.saveLayer(308,118,942,262,null);canvas.clipRect(308,118,942,262);canvas.drawBitmap(banner,null,new RectF(0,0,1005,378),paint);
  paint.setShader(new LinearGradient(308,118,942,262,new int[]{0xFFF4FAFF,0xFFC8DEFF,0xFF7FB3FF,0xFF3686FF,0xFF0055F5},new float[]{0,0.22f,0.48f,0.74f,1},Shader.TileMode.CLAMP));paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));canvas.drawRect(308,118,942,262,paint);paint.setXfermode(null);paint.setShader(null);canvas.restoreToCount(layer);canvas.restore();
 }
 @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
 @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
 @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
