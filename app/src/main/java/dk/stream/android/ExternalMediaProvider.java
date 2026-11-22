package dk.stream.android;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Relays one selected document with temporary read access; account credentials remain private. */
@androidx.media3.common.util.UnstableApi
public final class ExternalMediaProvider extends ContentProvider {
 static Uri share(Context context,LocalMedia media)throws Exception{String id=UUID.randomUUID().toString();File directory=new File(context.getCacheDir(),"external-media");if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot share media");for(File file:directory.listFiles()==null?new File[0]:directory.listFiles())if(file.lastModified()<System.currentTimeMillis()-7L*24*60*60*1000)file.delete();try(FileOutputStream output=new FileOutputStream(new File(directory,id))){output.write(media.json().toString().getBytes(StandardCharsets.UTF_8));}return new Uri.Builder().scheme("content").authority(context.getPackageName()+".externalmedia").appendPath(id).appendPath(media.name.substring(media.name.lastIndexOf('/')+1).trim()).build();}
 private LocalMedia media(Uri uri)throws FileNotFoundException{String id=uri.getPathSegments().isEmpty()?null:uri.getPathSegments().get(0);if(id==null||!id.matches("[0-9a-f-]{36}")||uri.getPathSegments().size()>2)throw new FileNotFoundException();try(FileInputStream input=new FileInputStream(new File(getContext().getCacheDir(),"external-media/"+id));ByteArrayOutputStream output=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int count;while((count=input.read(buffer))!=-1){if(output.size()+count>32768)throw new IOException();output.write(buffer,0,count);}return LocalMedia.from(new org.json.JSONObject(output.toString("UTF-8")));}catch(Exception failure){throw new FileNotFoundException("Media unavailable");}}
 @Override public boolean onCreate(){return true;}
 @Override public String getType(Uri uri){try{LocalMedia file=media(uri);return ExternalPlaybackType.mime(file.uri,file.name,file.mime);}catch(Exception error){return "application/octet-stream";}}
 @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){try{LocalMedia file=media(uri);String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor cursor=new MatrixCursor(columns);Object[] row=new Object[columns.length];for(int i=0;i<columns.length;i++)row[i]=OpenableColumns.DISPLAY_NAME.equals(columns[i])?file.name.substring(file.name.lastIndexOf('/')+1).trim():OpenableColumns.SIZE.equals(columns[i])&&PcloudDocument.size(file.uri)>=0?PcloudDocument.size(file.uri):null;cursor.addRow(row);return cursor;}catch(Exception failure){return null;}}
 @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
  if(!"r".equals(mode))throw new FileNotFoundException("Read only");LocalMedia file=media(uri);try{if(!PcloudDocument.is(file.uri)){ParcelFileDescriptor descriptor=getContext().getContentResolver().openFileDescriptor(Uri.parse(file.uri),"r");if(descriptor==null)throw new IOException("File unavailable");return descriptor;}PcloudAccount account=new LibraryStore(getContext()).load().pcloud;if(account==null)throw new IOException();WebDavClient cloud=new WebDavClient(account);okhttp3.HttpUrl url=PcloudDocument.url(account,file.uri);long sourceLength=PcloudDocument.size(file.uri);if(sourceLength<0&&Build.VERSION.SDK_INT>=26)sourceLength=length(cloud,url);final long length=sourceLength;
   if(Build.VERSION.SDK_INT>=26&&length>=0){HandlerThread thread=new HandlerThread("external-pcloud");thread.start();return ((android.os.storage.StorageManager)getContext().getSystemService(Context.STORAGE_SERVICE)).openProxyFileDescriptor(ParcelFileDescriptor.MODE_READ_ONLY,new ProxyFileDescriptorCallback(){InputStream stream;long offset;
     @Override public long onGetSize(){return length;}
     @Override public int onRead(long position,int size,byte[] data)throws android.system.ErrnoException{try{if(position>=length)return 0;if(stream==null||position!=offset){if(stream!=null)stream.close();stream=cloud.open(url,position);offset=position;}int count=stream.read(data,0,(int)Math.min(size,length-position));if(count<0)return 0;offset+=count;return count;}catch(IOException error){throw new android.system.ErrnoException("read",android.system.OsConstants.EIO);}}
     @Override public void onRelease(){try{if(stream!=null)stream.close();}catch(IOException ignored){}cloud.cancel();thread.quitSafely();}
    },new Handler(thread.getLooper()));}
   ParcelFileDescriptor[] pipe=ParcelFileDescriptor.createReliablePipe();new Thread(()->{try(InputStream input=cloud.open(url,0);OutputStream output=new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])){byte[] buffer=new byte[32768];int count;while((count=input.read(buffer))!=-1)output.write(buffer,0,count);}catch(IOException failure){try{pipe[1].closeWithError("Media read failed");}catch(IOException ignored){}}finally{cloud.cancel();}},"external-pcloud-pipe").start();return pipe[0];
  }catch(Exception failure){throw new FileNotFoundException("Pcloud media unavailable");}
 }
 private static long length(WebDavClient cloud,okhttp3.HttpUrl url){okhttp3.OkHttpClient client=cloud.streamClient().newBuilder().callTimeout(15,java.util.concurrent.TimeUnit.SECONDS).build();try(okhttp3.Response response=client.newCall(new okhttp3.Request.Builder().url(url).head().build()).execute()){if(response.isSuccessful()){String size=response.header("Content-Length");if(size!=null)return Long.parseLong(size);}}catch(Exception ignored){}try(okhttp3.Response response=client.newCall(new okhttp3.Request.Builder().url(url).header("Range","bytes=0-0").build()).execute()){if(response.code()==206){String range=response.header("Content-Range");if(range!=null)return Long.parseLong(range.substring(range.lastIndexOf('/')+1));}if(response.code()==200&&response.body()!=null)return response.body().contentLength();}catch(Exception ignored){}return -1;}
 @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
 @Override public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}
 @Override public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
}
