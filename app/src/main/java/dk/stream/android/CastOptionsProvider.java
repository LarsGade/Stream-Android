package dk.stream.android;
import android.content.Context;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import java.util.List;
public final class CastOptionsProvider implements OptionsProvider {
 public CastOptions getCastOptions(Context context) { return new CastOptions.Builder().setReceiverApplicationId(BuildConfig.CAST_RECEIVER_APP_ID).setCastMediaOptions(new com.google.android.gms.cast.framework.media.CastMediaOptions.Builder().setMediaSessionEnabled(true).build()).build(); }
 public List<SessionProvider> getAdditionalSessionProviders(Context context) { return null; }
}
