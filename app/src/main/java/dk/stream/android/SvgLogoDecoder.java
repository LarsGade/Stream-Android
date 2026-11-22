package dk.stream.android;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Bounded SVG decoding into the same bitmap pipeline used for PNG/JPEG/WebP logos. */
public final class SvgLogoDecoder implements ResourceDecoder<InputStream,Bitmap> {
 private final BitmapPool pool;
 public SvgLogoDecoder(BitmapPool pool) {this.pool=pool;SVG.setInternalEntitiesEnabled(false);}
 @Override public boolean handles(InputStream input,Options options) throws IOException {
  if(!input.markSupported()) return false;
  input.mark(8192);
  byte[] prefix=new byte[4096];int length=0,count;
  try {while(length<prefix.length && (count=input.read(prefix,length,prefix.length-length))>0) length+=count;}
  finally {input.reset();}
  return new String(prefix,0,length,StandardCharsets.UTF_8).toLowerCase(Locale.ROOT).contains("<svg");
 }
 @Override public Resource<Bitmap> decode(InputStream input,int width,int height,Options options) throws IOException {
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;
  while((count=input.read(buffer))!=-1) {if(bytes.size()+count>2*1024*1024) throw new IOException("SVG logo too large");bytes.write(buffer,0,count);}
  try {
   SVG svg=SVG.getFromInputStream(new ByteArrayInputStream(bytes.toByteArray()));
   int w=width>0?Math.min(width,1024):128,h=height>0?Math.min(height,1024):128;
   if(svg.getDocumentViewBox()==null) {
    float originalWidth=svg.getDocumentWidth(),originalHeight=svg.getDocumentHeight();
    svg.setDocumentViewBox(0,0,originalWidth>0?originalWidth:w,originalHeight>0?originalHeight:h);
   }
   svg.setDocumentWidth(w);svg.setDocumentHeight(h);
   Bitmap bitmap=pool.get(w,h,Bitmap.Config.ARGB_8888);bitmap.eraseColor(android.graphics.Color.TRANSPARENT);
   svg.renderToCanvas(new Canvas(bitmap));return BitmapResource.obtain(bitmap,pool);
  } catch(SVGParseException|IllegalArgumentException e) {throw new IOException("Invalid SVG logo",e);}
 }
}
