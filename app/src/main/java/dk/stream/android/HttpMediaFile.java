package dk.stream.android;
import android.content.Context;
import android.os.*;
import java.io.*;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
/** A seekable read-only HTTP descriptor for external players and document consumers. */
final class HttpMediaFile {
 private static final java.util.Map<String,Long> sizes=java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<String,Long>(32,0.75f,true){@Override protected boolean removeEldestEntry(java.util.Map.Entry<String,Long> entry){return size()>256;}});
 private static long remember(HttpUrl url,long length){if(length>=0)sizes.put(url.toString(),length);return length;}
 static long size(OkHttpClient client,HttpUrl url){Long known=sizes.get(url.toString());if(known!=null)return known;OkHttpClient probe=client.newBuilder().callTimeout(15,TimeUnit.SECONDS).build();try(Response response=probe.newCall(new Request.Builder().url(url).head().build()).execute()){if(response.isSuccessful()){String length=response.header("Content-Length");if(length!=null)return remember(url,Long.parseLong(length));}}catch(Exception ignored){}try(Response response=probe.newCall(new Request.Builder().url(url).header("Range","bytes=0-0").build()).execute()){if(response.code()==206){String range=response.header("Content-Range");if(range!=null)return remember(url,Long.parseLong(range.substring(range.lastIndexOf('/')+1)));}if(response.code()==200&&response.body()!=null)return remember(url,response.body().contentLength());}catch(Exception ignored){}return -1;}
 static ParcelFileDescriptor open(Context context,String address)throws IOException{HttpUrl url=HttpUrl.parse(address);if(url==null)throw new IOException("Invalid stream URL");OkHttpClient client=SaavnApi.STREAM.newBuilder().dispatcher(new Dispatcher()).build();long length=size(client,url);
  if(Build.VERSION.SDK_INT>=26&&length>=0){HandlerThread thread=new HandlerThread("music-document");thread.start();try{return ((android.os.storage.StorageManager)context.getSystemService(Context.STORAGE_SERVICE)).openProxyFileDescriptor(ParcelFileDescriptor.MODE_READ_ONLY,new ProxyFileDescriptorCallback(){InputStream input;Response response;long offset;
    @Override public long onGetSize(){return length;}
    @Override public int onRead(long position,int size,byte[] data)throws android.system.ErrnoException{try{if(position>=length)return 0;if(input==null||offset!=position){if(response!=null)response.close();Request.Builder request=new Request.Builder().url(url);if(position>0)request.header("Range","bytes="+position+"-");response=client.newCall(request.build()).execute();if(!response.isSuccessful()||response.body()==null)throw new IOException();input=response.body().byteStream();if(position>0&&response.code()==200){long remaining=position;while(remaining>0){long skipped=input.skip(remaining);if(skipped==0){if(input.read()<0)throw new EOFException();skipped=1;}remaining-=skipped;}}offset=position;}int count=input.read(data,0,(int)Math.min(size,length-position));if(count<0)return 0;offset+=count;return count;}catch(IOException failure){throw new android.system.ErrnoException("read",android.system.OsConstants.EIO);}}
    @Override public void onRelease(){if(response!=null)response.close();client.dispatcher().cancelAll();thread.quitSafely();}
   },new Handler(thread.getLooper()));}catch(IOException failure){thread.quitSafely();client.dispatcher().cancelAll();throw failure;}}
  ParcelFileDescriptor[] pipe=ParcelFileDescriptor.createReliablePipe();new Thread(()->{try(Response response=client.newCall(new Request.Builder().url(url).build()).execute();OutputStream output=new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])){if(!response.isSuccessful()||response.body()==null)throw new IOException();try(InputStream input=response.body().byteStream()){byte[] buffer=new byte[32768];int count;while((count=input.read(buffer))!=-1)output.write(buffer,0,count);}}catch(IOException failure){try{pipe[1].closeWithError("Musikken kunne ikke læses.");}catch(IOException ignored){}}finally{client.dispatcher().cancelAll();}},"music-document-pipe").start();return pipe[0];
 }
 private HttpMediaFile(){}
}
