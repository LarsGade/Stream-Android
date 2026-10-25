package dk.stream.android;
import android.content.Context;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
@GlideModule
public final class StreamGlideModule extends AppGlideModule {
 @Override public void applyOptions(Context context,GlideBuilder builder) {
  // Logo URLs may contain account tokens; never write them to logcat.
  builder.setLogLevel(android.util.Log.ASSERT);
 }
}
