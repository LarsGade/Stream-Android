package dk.stream.android;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import java.io.*;
import java.util.*;
import org.json.*;

final class DownloadDestination {
 static Uri create(Context context,JSONObject plan)throws Exception{String name=DownloadNames.clean(plan.getString("name"));if(Build.VERSION.SDK_INT>=29){ContentValues values=new ContentValues();values.put(MediaStore.Downloads.DISPLAY_NAME,name);values.put(MediaStore.Downloads.MIME_TYPE,plan.optString("mime","application/octet-stream"));values.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/");values.put(MediaStore.Downloads.IS_PENDING,1);Uri uri=context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);if(uri==null)throw new IOException("Cannot create download");return uri;}File directory=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Downloads folder unavailable");Set<String> tried=new HashSet<>();while(true){File file=new File(directory,DownloadNames.unique(name,tried));if(file.createNewFile())return androidx.core.content.FileProvider.getUriForFile(context,context.getPackageName()+".downloads",file);}}
 static void publish(Context context,Uri uri)throws IOException{if(Build.VERSION.SDK_INT>=29&&MediaStore.AUTHORITY.equals(uri.getAuthority())){ContentValues values=new ContentValues();values.put(MediaStore.Downloads.IS_PENDING,0);if(context.getContentResolver().update(uri,values,null,null)<=0)throw new IOException("Cannot publish download");}else if((context.getPackageName()+".downloads").equals(uri.getAuthority())){File file=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),uri.getLastPathSegment());android.media.MediaScannerConnection.scanFile(context,new String[]{file.getAbsolutePath()},null,null);}}
 static void remove(Context context,Uri uri){try{if(android.provider.DocumentsContract.isDocumentUri(context,uri))android.provider.DocumentsContract.deleteDocument(context.getContentResolver(),uri);else context.getContentResolver().delete(uri,null,null);}catch(Exception ignored){}}
 private DownloadDestination(){}
}
