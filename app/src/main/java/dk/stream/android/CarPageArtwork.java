package dk.stream.android;

import android.graphics.*;
import java.io.ByteArrayOutputStream;

/** Large, legible PNGs for car hosts; no emoji or font-dependent country flags. */
final class CarPageArtwork {
 private static final java.util.Map<String,byte[]> cache=new java.util.concurrent.ConcurrentHashMap<>();
 static byte[] png(String id){byte[] cached=cache.get(id);if(cached!=null)return cached;Bitmap image=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xFF182C45);canvas.drawRoundRect(16,16,496,496,72,72,paint);
  if("radio-danish".equals(id)){paint.setColor(0xFFC8102E);canvas.drawRect(64,112,448,400,paint);paint.setColor(Color.WHITE);canvas.drawRect(176,112,224,400,paint);canvas.drawRect(64,232,448,280,paint);}
  else if("radio-england".equals(id)){paint.setColor(Color.WHITE);canvas.drawRect(64,112,448,400,paint);paint.setColor(0xFFCE1124);canvas.drawRect(232,112,280,400,paint);canvas.drawRect(64,232,448,280,paint);}
  else if("radio-usa".equals(id)){paint.setColor(Color.WHITE);canvas.drawRect(64,112,448,398,paint);paint.setColor(0xFFB22234);for(int i=0;i<13;i+=2)canvas.drawRect(64,112+i*22,448,134+i*22,paint);paint.setColor(0xFF3C3B6E);canvas.drawRect(64,112,248,266,paint);paint.setColor(Color.WHITE);for(int y=0;y<5;y++)for(int x=0;x<6;x++)canvas.drawCircle(80+x*29,128+y*29,5,paint);}
  else if(AndroidAutoPages.FAVORITES.equals(id)){paint.setShader(new LinearGradient(112,80,400,420,0xFFEAF4FF,0xFF3686FF,Shader.TileMode.CLAMP));Path star=new Path();for(int i=0;i<10;i++){double angle=-Math.PI/2+i*Math.PI/5;float radius=i%2==0?176:78;float x=256+(float)Math.cos(angle)*radius,y=256+(float)Math.sin(angle)*radius;if(i==0)star.moveTo(x,y);else star.lineTo(x,y);}star.close();canvas.drawPath(star,paint);}
  else if(AndroidAutoPages.ROOT.equals(id)||AndroidAutoPages.START.equals(id)){paint.setColor(0xFF3686FF);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(28);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStrokeCap(Paint.Cap.ROUND);Path home=new Path();home.moveTo(96,230);home.lineTo(256,98);home.lineTo(416,230);home.moveTo(128,208);home.lineTo(128,408);home.lineTo(220,408);home.lineTo(220,302);home.lineTo(292,302);home.lineTo(292,408);home.lineTo(384,408);home.lineTo(384,208);canvas.drawPath(home,paint);}
  else{paint.setColor(0xFF3686FF);canvas.drawCircle(256,256,190,paint);paint.setColor(Color.WHITE);paint.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));paint.setTextSize(100);paint.setTextAlign(Paint.Align.CENTER);canvas.drawText("Radio",256,256-(paint.ascent()+paint.descent())/2,paint);}
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();image.compress(Bitmap.CompressFormat.PNG,100,bytes);image.recycle();byte[] data=bytes.toByteArray();cache.put(id,data);return data;}
 private CarPageArtwork(){}
}
