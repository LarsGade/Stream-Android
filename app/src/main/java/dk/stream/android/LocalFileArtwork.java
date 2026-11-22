package dk.stream.android;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import java.util.*;
/** Artwork comes only from the original media file, never from its programme name. */
final class LocalFileArtwork {
 static String asset(LocalMedia file){return null;}
 static byte[] bytes(Context context,LocalMedia file){
  if(file==null||file.folder||file.image()||SaavnMusic.is(file.uri))return null;
  MediaMetadataRetriever retriever=new MediaMetadataRetriever();
  try{if(PcloudDocument.is(file.uri)){PcloudAccount account=new LibraryStore(context).load().pcloud;if(account==null)return null;String url=PcloudDocument.url(account,file.uri).toString();Map<String,String> headers=new HashMap<>();headers.put("Authorization",okhttp3.Credentials.basic(account.email,account.password));retriever.setDataSource(url,headers);}else retriever.setDataSource(context,Uri.parse(file.uri));byte[] data=retriever.getEmbeddedPicture();return data!=null&&data.length<=8*1024*1024?data:null;
  }catch(Exception unavailable){return null;}finally{try{retriever.release();}catch(Exception ignored){}}
 }
 private LocalFileArtwork(){}
}
