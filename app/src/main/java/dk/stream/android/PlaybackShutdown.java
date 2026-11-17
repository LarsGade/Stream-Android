package dk.stream.android;

import android.content.*;

/** Explicit app closure stops owned players, Cast and foreground notifications. */
@androidx.media3.common.util.UnstableApi
public final class PlaybackShutdown {
 public static void stop(Context context){
  BluetoothPlayback.clear();
  if(LocalAudioService.player()!=null){LocalAudioService.player().pause();LocalAudioService.player().stop();LocalAudioService.player().clearMediaItems();}
  if(RadioPlaybackService.audioPlayer()!=null){RadioPlaybackService.audioPlayer().pause();RadioPlaybackService.audioPlayer().stop();RadioPlaybackService.audioPlayer().clearMediaItems();}
  CastPlayback.closeWithApp(context);
  context.stopService(new Intent(context,LocalAudioService.class));
  context.stopService(new Intent(context,RadioPlaybackService.class));
  context.stopService(new Intent(context,LocalCastService.class));
  android.app.NotificationManager notifications=(android.app.NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
  for(int id:new int[]{2001,2002,2003,2004})notifications.cancel(id);
 }
 private PlaybackShutdown(){}
}
