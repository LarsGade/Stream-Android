package dk.stream.android;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Grants one selected pCloud document, without exposing the account's credentials. */
@androidx.media3.common.util.UnstableApi
public final class ExternalMediaProvider extends ContentProvider {
 static Uri share(Context context,LocalMedia media)throws Exception{String id=UUID.randomUUID().toString();File directory=new File(context.getCacheDir(),"external-media");if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot share media");for(File file:directory.listFiles()==null?new File[0]:directory.listFiles())if(file.lastModified()<System.currentTimeMillis()-7L*24*60*60*1000)file.delete();try(FileOutputStream output=new FileOutputStream(new File(directory,id))){output.write(media.json().toString().getBytes(StandardCharsets.UTF_8));}return new Uri.Builder().scheme("content").authority(context.getPackageName()+".externalmedia").appendPath(id).build();}
 private LocalMedia media(Uri uri)throws FileNotFoundException{String id=uri.getLastPathSegment();if(id==null||!id.matches("[0-9a-f-]{36}")||uri.getPathSegments().size()!=1)throw new FileNotFoundException();try(FileInputStream input=new FileInputStream(new File(getContext().getCacheDir(),"external-media/"+id));ByteArrayOutputStream output=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int count;while((count=input.read(buffer))!=-1){if(output.size()+count>32768)throw new IOException();output.write(buffer,0,count);}return LocalMedia.from(new org.json.JSONObject(output.toString("UTF-8")));}catch(Exception failure){throw new FileNotFoundException("Media unavailable");}}
 @Override public boolean onCreate(){return true;}
 @Override public String getType(Uri uri){try{return CastPlayback.mime(media(uri));}catch(Exception error){return "application/octet-stream";}}
 @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){try{LocalMedia file=media(uri);String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor cursor=new MatrixCursor(columns);Object[] row=new Object[columns.length];for(int i=0;i<columns.length;i++)row[i]=OpenableColumns.DISPLAY_NAME.equals(columns[i])?file.name:OpenableColumns.SIZE.equals(columns[i])&&PcloudDocument.size(file.uri)>=0?PcloudDocument.size(file.uri):null;cursor.addRow(row);return cursor;}catch(Exception failure){return null;}}
 @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
  if(!"r".equals(mode))throw new FileNotFoundException("Read only");LocalMedia file=media(uri);try{PcloudAccount account=new LibraryStore(getContext()).load().pcloud;if(account==null)throw new IOException();WebDavClient cloud=new WebDavClient(account);okhttp3.HttpUrl url=PcloudDocument.url(account,file.uri);long length=PcloudDocument.size(file.uri);
   if(Build.VERSION.SDK_INT>=26&&length>=0){HandlerThread thread=new HandlerThread("external-pcloud");thread.start();return ((android.os.storage.StorageManager)getContext().getSystemService(Context.STORAGE_SERVICE)).openProxyFileDescriptor(ParcelFileDescriptor.MODE_READ_ONLY,new ProxyFileDescriptorCallback(){InputStream stream;long offset;
     @Override public long onGetSize(){return length;}
     @Override public int onRead(long position,int size,byte[] data)throws android.system.ErrnoException{try{if(position>=length)return 0;if(stream==null||position!=offset){if(stream!=null)stream.close();stream=cloud.open(url,position);offset=position;}int count=stream.read(data,0,(int)Math.min(size,length-position));if(count<0)return 0;offset+=count;return count;}catch(IOException error){throw new android.system.ErrnoException("read",android.system.OsConstants.EIO);}}
     @Override public void onRelease(){try{if(stream!=null)stream.close();}catch(IOException ignored){}cloud.cancel();thread.quitSafely();}
    },new Handler(thread.getLooper()));}
   ParcelFileDescriptor[] pipe=ParcelFileDescriptor.createReliablePipe();new Thread(()->{try(InputStream input=cloud.open(url,0);OutputStream output=new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])){byte[] buffer=new byte[32768];int count;while((count=input.read(buffer))!=-1)output.write(buffer,0,count);}catch(IOException failure){try{pipe[1].closeWithError("Media read failed");}catch(IOException ignored){}}finally{cloud.cancel();}},"external-pcloud-pipe").start();return pipe[0];
  }catch(Exception failure){throw new FileNotFoundException("Pcloud media unavailable");}
 }
 @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
 @Override public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}
 @Override public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
}
