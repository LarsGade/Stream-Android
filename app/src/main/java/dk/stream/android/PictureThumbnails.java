package dk.stream.android;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.*;
import android.util.Size;
import android.widget.ImageView;
import com.bumptech.glide.*;
import com.bumptech.glide.load.*;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.*;
import com.bumptech.glide.signature.ObjectKey;
import java.io.*;
/** Prefer the actual image; try a document-provider preview before using the white icon. */
final class PictureThumbnails {
 static void load(ImageView view,String uri,int size){RequestManager manager=Glide.with(view);manager.load(Uri.parse(uri)).override(size,size).fitCenter().placeholder(R.drawable.ic_image).error(manager.load(new Preview(Uri.parse(uri),size)).override(size,size).fitCenter().error(R.drawable.ic_image)).into(view);}
 static final class Preview {final Uri uri;final int size;Preview(Uri uri,int size){this.uri=uri;this.size=size;}}
 static final class Factory implements ModelLoaderFactory<Preview,Bitmap>{private final Context context;Factory(Context context){this.context=context.getApplicationContext();}public void teardown(){}public ModelLoader<Preview,Bitmap> build(MultiModelLoaderFactory loaders){return new ModelLoader<Preview,Bitmap>(){
  public boolean handles(Preview value){return true;}
  public LoadData<Bitmap> buildLoadData(Preview value,int width,int height,Options options){return new LoadData<>(new ObjectKey("picture-preview-v1:"+value.uri+":"+value.size),new DataFetcher<Bitmap>(){
   private final CancellationSignal cancellation=new CancellationSignal();
   public void loadData(Priority priority,DataCallback<? super Bitmap> callback){try{if(!"content".equals(value.uri.getScheme())||PcloudDocument.is(value.uri.toString()))throw new FileNotFoundException("No provider preview");Bitmap bitmap;if(Build.VERSION.SDK_INT>=29)bitmap=context.getContentResolver().loadThumbnail(value.uri,new Size(value.size,value.size),cancellation);else if(android.provider.DocumentsContract.isDocumentUri(context,value.uri))bitmap=android.provider.DocumentsContract.getDocumentThumbnail(context.getContentResolver(),value.uri,new android.graphics.Point(value.size,value.size),cancellation);else throw new FileNotFoundException("No document preview");if(bitmap==null)throw new FileNotFoundException("No picture preview");if(cancellation.isCanceled()){bitmap.recycle();return;}callback.onDataReady(bitmap);}catch(Exception failure){if(!cancellation.isCanceled())callback.onLoadFailed(new FileNotFoundException("Picture preview unavailable"));}}
   public void cleanup(){}public void cancel(){cancellation.cancel();}public Class<Bitmap> getDataClass(){return Bitmap.class;}public DataSource getDataSource(){return DataSource.LOCAL;}
  });}
 };}}
 private PictureThumbnails(){}
}
