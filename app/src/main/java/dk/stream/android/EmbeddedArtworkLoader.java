package dk.stream.android;
import android.content.Context;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.*;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.*;
import com.bumptech.glide.signature.ObjectKey;
import java.io.*;
/** Extract embedded covers on Glide's worker thread, with its normal memory/disk cache. */
final class EmbeddedArtworkLoader implements ModelLoader<LocalMedia,InputStream>{
 private final Context context;
 EmbeddedArtworkLoader(Context context){this.context=context.getApplicationContext();}
 public boolean handles(LocalMedia file){return !file.folder&&!file.image()&&!SaavnMusic.is(file.uri);}
 public LoadData<InputStream> buildLoadData(LocalMedia file,int width,int height,Options options){return new LoadData<>(new ObjectKey("embedded-v1:"+file.uri),new DataFetcher<InputStream>(){
  private volatile boolean cancelled;private InputStream input;
  public void loadData(Priority priority,DataCallback<? super InputStream> callback){byte[] cover=LocalFileArtwork.bytes(context,file);if(cancelled)return;if(cover==null){callback.onLoadFailed(new FileNotFoundException("No embedded artwork"));return;}input=new ByteArrayInputStream(cover);callback.onDataReady(input);}
  public void cleanup(){if(input!=null)try{input.close();}catch(IOException ignored){}}
  public void cancel(){cancelled=true;}
  public Class<InputStream> getDataClass(){return InputStream.class;}
  public DataSource getDataSource(){return DataSource.LOCAL;}
 });}
 static final class Factory implements ModelLoaderFactory<LocalMedia,InputStream>{private final Context context;Factory(Context context){this.context=context;}public ModelLoader<LocalMedia,InputStream> build(MultiModelLoaderFactory factory){return new EmbeddedArtworkLoader(context);}public void teardown(){}}
}
