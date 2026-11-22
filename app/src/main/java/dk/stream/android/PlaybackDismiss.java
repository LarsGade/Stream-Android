package dk.stream.android;
import android.content.Context;
@androidx.media3.common.util.UnstableApi
final class PlaybackDismiss {
 static boolean stop(Context context){if(TvRecordingService.active()){UiText.toast(context,"Stop optagelsen, før du skifter afspilning eller forlader siden.",android.widget.Toast.LENGTH_SHORT).show();return false;}BluetoothPlayback.clear();CastPlayback.stop(context);LocalAudioService.dismissPlayback();RadioPlaybackService.dismissPlayback();return true;}
 private PlaybackDismiss(){}
}
