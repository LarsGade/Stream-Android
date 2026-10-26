package dk.stream.android;

import android.content.Context;
import android.graphics.Bitmap;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import java.io.*;

/** Local logos are immediately available to Android's media session; remote artwork loads off-thread. */
public final class PlaybackArtwork {
 private PlaybackArtwork() { }
 // openRawResource also exposes the original bytes of a packaged PNG drawable.
 @android.annotation.SuppressLint("ResourceType")
 public static byte[] local(Context context,String name) {
  String asset=LocalChannelLogos.load(context).find(name);
  try(InputStream in=asset==null?context.getResources().openRawResource(R.drawable.brand_icon):context.getAssets().open(asset.substring("file:///android_asset/".length()));ByteArrayOutputStream out=new ByteArrayOutputStream()) {
   byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1) out.write(buffer,0,n);return out.toByteArray();
  } catch(IOException e) {return null;}
 }
 public static boolean bundled(Context context,String name) {return LocalChannelLogos.load(context).find(name)!=null;}
 public static byte[] remote(Context context,String url) {
  if(okhttp3.HttpUrl.parse(url)==null) return null;
  FutureTarget<Bitmap> target=Glide.with(context.getApplicationContext()).asBitmap().load(LogoRequests.model(url,null)).submit(384,384);
  try {ByteArrayOutputStream out=new ByteArrayOutputStream();target.get().compress(Bitmap.CompressFormat.PNG,100,out);return out.toByteArray();}
  catch(Exception failure) {return null;} finally {Glide.with(context.getApplicationContext()).clear(target);}
 }
}
