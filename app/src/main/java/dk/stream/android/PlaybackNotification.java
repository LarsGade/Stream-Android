package dk.stream.android;

import android.app.*;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Build;
import androidx.media3.common.Player;
import androidx.media3.session.MediaSession;

/** A real MediaStyle notification, with artwork and a session token for lock-screen transport controls. */
@androidx.media3.common.util.UnstableApi
public final class PlaybackNotification {
 private PlaybackNotification() { }
 @SuppressWarnings("deprecation")
 public static Notification create(Context context,String channel,MediaSession session,PendingIntent toggle,PendingIntent stop) {
  if(Build.VERSION.SDK_INT>=26) {
   NotificationChannel settings=new NotificationChannel(channel,"Stream",NotificationManager.IMPORTANCE_LOW);settings.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
   ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(settings);
  }
  Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,channel):new Notification.Builder(context);
  Player player=session.getPlayer();androidx.media3.common.MediaMetadata metadata=player.getMediaMetadata();boolean playing=player.getPlayWhenReady();
  builder.setColor(0xFF3686FF).setSmallIcon(R.drawable.ic_play).setContentTitle(metadata.title==null?context.getString(R.string.app_name):metadata.title)
   .setContentText(context.getString(R.string.app_name)).setContentIntent(session.getSessionActivity()).setVisibility(Notification.VISIBILITY_PUBLIC).setOnlyAlertOnce(true).setOngoing(playing).setShowWhen(false)
   .addAction(playing?R.drawable.ic_pause:R.drawable.ic_play,context.getString(playing?R.string.pause_action:R.string.play_action),toggle)
   .addAction(R.drawable.ic_stop,context.getString(R.string.stop_action),stop)
   .setStyle(new Notification.MediaStyle().setMediaSession(session.getPlatformToken()).setShowActionsInCompactView(0,1));
  if(Build.VERSION.SDK_INT>=26) builder.setColorized(true);
  if(metadata.artworkData!=null) {android.graphics.Bitmap artwork=BitmapFactory.decodeByteArray(metadata.artworkData,0,metadata.artworkData.length);if(artwork!=null) builder.setLargeIcon(artwork);}
  return builder.build();
 }
}
