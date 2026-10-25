package dk.stream.android;
import android.content.Context;
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
}
