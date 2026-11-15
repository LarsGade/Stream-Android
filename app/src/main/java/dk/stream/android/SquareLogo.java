package dk.stream.android;
import android.graphics.Bitmap;
final class SquareLogo {
 static Bitmap bitmap(Bitmap source){
  if(source==null)return null;int size=Math.min(source.getWidth(),source.getHeight());return Bitmap.createBitmap(source,(source.getWidth()-size)/2,(source.getHeight()-size)/2,size,size);
 }
 private SquareLogo(){}
}
