package dk.stream.android;

import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.*;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.*;
import com.bumptech.glide.signature.ObjectKey;
import java.io.*;

/** Authenticated cloud images use opaque account references, not URL passwords. */
public final class PcloudImageLoader implements ModelLoader<Uri,InputStream> {
 private final Context context;
 private PcloudImageLoader(Context context){this.context=context.getApplicationContext();}
 @Override public boolean handles(Uri uri){return PcloudDocument.is(uri.toString());}
 @Override public LoadData<InputStream> buildLoadData(Uri uri,int width,int height,Options options){return new LoadData<>(new ObjectKey(uri),new Fetcher(context,uri));}
 public static final class Factory implements ModelLoaderFactory<Uri,InputStream>{private final Context context;public Factory(Context context){this.context=context;}public ModelLoader<Uri,InputStream> build(MultiModelLoaderFactory factory){return new PcloudImageLoader(context);}public void teardown(){}}
 private static final class Fetcher implements DataFetcher<InputStream>{
  private final Context context;private final Uri uri;private volatile WebDavClient client;private InputStream input;private volatile boolean cancelled;
  Fetcher(Context context,Uri uri){this.context=context;this.uri=uri;}
  public void loadData(Priority priority,DataCallback<? super InputStream> callback){try{PcloudAccount account=new LibraryStore(context).load().pcloud;okhttp3.HttpUrl url=PcloudDocument.url(account,uri.toString());client=new WebDavClient(account);if(cancelled){client.cancel();return;}input=client.open(url,0);if(!cancelled)callback.onDataReady(input);else cleanup();}catch(Exception error){if(!cancelled)callback.onLoadFailed(new IOException("Billedet kunne ikke hentes fra Pcloud."));}}
  public void cleanup(){if(input!=null)try{input.close();}catch(IOException ignored){}input=null;}
  public void cancel(){cancelled=true;WebDavClient request=client;if(request!=null)request.cancel();}
  public Class<InputStream> getDataClass(){return InputStream.class;}public DataSource getDataSource(){return DataSource.REMOTE;}
 }
}
