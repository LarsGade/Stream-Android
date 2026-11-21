package dk.stream.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import java.io.*;

/** Publish actual file artwork as a URL the receiver can fetch, without a generic fallback. */
@androidx.media3.common.util.UnstableApi
final class CastFileArtwork {
 static byte[] available(Context context,LocalMedia file){if(!file.audio())return null;byte[] bundled=LocalFileArtwork.bytes(context,file);return bundled!=null?bundled:CastPlayback.cachedArtwork(file);}
 static byte[] read(Context context,LocalMedia file,String mediaUrl){
  if(!file.audio())return null;
  byte[] data=available(context,file);if(data!=null&&data.length>0)return data;
  if(SaavnMusic.is(file.uri))return SaavnMusic.artwork(context,file);
  MediaMetadataRetriever retriever=new MediaMetadataRetriever();
  try{if(PcloudDocument.is(file.uri))retriever.setDataSource(mediaUrl,new java.util.HashMap<>());else retriever.setDataSource(context,Uri.parse(file.uri));return retriever.getEmbeddedPicture();}
  catch(Exception unavailable){return null;}finally{try{retriever.release();}catch(Exception ignored){}}
 }
 static String publish(LocalCastHttpServer server,byte[] data){
  if(data==null||data.length==0)return null;
  BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,options);if(options.outWidth<=0||options.outHeight<=0)return null;
  options.inJustDecodeBounds=false;options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>1024)options.inSampleSize*=2;
  Bitmap bitmap=BitmapFactory.decodeByteArray(data,0,data.length,options);if(bitmap==null)return null;
  try{ByteArrayOutputStream output=new ByteArrayOutputStream();if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,output))return null;byte[] png=output.toByteArray();return server.add(()->new ByteArrayInputStream(png),png.length,"image/png");}finally{bitmap.recycle();}
 }
 private CastFileArtwork(){}
}
