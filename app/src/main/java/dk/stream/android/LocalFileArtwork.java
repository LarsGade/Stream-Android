package dk.stream.android;

import android.content.Context;
import java.io.*;
import java.text.Normalizer;
import java.util.Locale;

/** Named artwork for local and Pcloud programmes, independent of embedded cover art. */
final class LocalFileArtwork {
 static String asset(LocalMedia file){
  if(file==null||file.folder||file.image()||SaavnMusic.is(file.uri))return null;
  String name=Normalizer.normalize(file.name,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
  String logo=name.contains("anders og anders")?"anders.png":name.contains("bæltestedet")||name.contains("baeltestedet")?"balte.png":name.contains("r8dio")?"roder.png":name.contains("den korte radioavis")?"korte.png":null;
  return logo==null?null:"file:///android_asset/local_logos/"+logo;
 }
 static byte[] bytes(Context context,LocalMedia file){
  String asset=asset(file);if(asset==null)return null;
  try(InputStream input=context.getAssets().open(asset.substring("file:///android_asset/".length()));ByteArrayOutputStream output=new ByteArrayOutputStream()){
   byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1)output.write(buffer,0,count);return output.toByteArray();
  }catch(IOException failure){return null;}
 }
 private LocalFileArtwork(){}
}
