package dk.stream.android;
import android.content.Context;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import java.util.List;
@androidx.media3.common.util.UnstableApi
public final class CastOptionsProvider implements OptionsProvider {
 public CastOptions getCastOptions(Context context) { return new CastOptions.Builder().setReceiverApplicationId(BuildConfig.CAST_RECEIVER_APP_ID).setCastMediaOptions(new com.google.android.gms.cast.framework.media.CastMediaOptions.Builder().setMediaSessionEnabled(true).setNotificationOptions(new com.google.android.gms.cast.framework.media.NotificationOptions.Builder().setSmallIconDrawableResId(R.drawable.brand_icon).setTargetActivityClassName(AudioPlayerActivity.class.getName()).build()).build()).build(); }
 public List<SessionProvider> getAdditionalSessionProviders(Context context) { return null; }
}
