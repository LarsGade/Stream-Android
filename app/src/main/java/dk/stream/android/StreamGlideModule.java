package dk.stream.android;
import android.content.Context;
import android.graphics.Bitmap;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
@GlideModule
public final class StreamGlideModule extends AppGlideModule {
 @Override public void applyOptions(Context context,GlideBuilder builder) {
  // Suppress normal request-failure warnings, which can include account URLs.
  // Glide accepts VERBOSE through ERROR; ASSERT throws during first initialization.
  builder.setLogLevel(android.util.Log.ERROR);
 }
 @Override public void registerComponents(Context context,Glide glide,Registry registry) {
  OkHttpClient client=new OkHttpClient.Builder().connectTimeout(15,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).callTimeout(30,TimeUnit.SECONDS).build();
  registry.replace(GlideUrl.class,InputStream.class,new OkHttpUrlLoader.Factory(client));
  registry.prepend(InputStream.class,Bitmap.class,new SvgLogoDecoder(glide.getBitmapPool()));
 }
}
