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
  return create(context,channel,session,toggle,stop,false);
 }
 @SuppressWarnings("deprecation")
 public static Notification create(Context context,String channel,MediaSession session,PendingIntent toggle,PendingIntent stop,boolean liveRadio) {
  return create(context,channel,session,toggle,stop,liveRadio,null,false);
 }
 public static Notification radio(Context context,MediaSession session,PendingIntent toggle,PendingIntent stop,PendingIntent live,boolean atLive){return create(context,"stream_radio",session,toggle,stop,false,live,atLive);}
 @SuppressWarnings("deprecation")
 private static Notification create(Context context,String channel,MediaSession session,PendingIntent toggle,PendingIntent stop,boolean liveRadio,PendingIntent live,boolean atLive){
  if(Build.VERSION.SDK_INT>=26) {
   NotificationChannel settings=new NotificationChannel(channel,PlaybackNotices.channelName(channel),NotificationManager.IMPORTANCE_LOW);settings.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
   ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(settings);
  }
  Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,channel):new Notification.Builder(context);
  Player player=session.getPlayer();androidx.media3.common.MediaMetadata metadata=player.getMediaMetadata();boolean playing=player.getPlayWhenReady();
  builder.setColor(0xFF3686FF).setSmallIcon(R.drawable.brand_icon).setContentTitle(metadata.title==null?context.getString(R.string.app_name):metadata.title)
   .setContentText(PlaybackText.SOURCE).setContentIntent(PlaybackNotices.open(context,2000)).setVisibility(Notification.VISIBILITY_PUBLIC).setOnlyAlertOnce(true).setOngoing(playing).setDeleteIntent(stop).setShowWhen(false);
  boolean radio="stream_radio".equals(channel),local="stream_local".equals(channel),car="stream_car_audio".equals(channel);PendingIntent previous=null,next=null;
  if(local){if(player.hasPreviousMediaItem())previous=PendingIntent.getService(context,34,LocalAudioService.command(context,LocalAudioService.PREVIOUS),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);if(player.hasNextMediaItem())next=PendingIntent.getService(context,35,LocalAudioService.command(context,LocalAudioService.NEXT),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
  if(car){if(player.hasPreviousMediaItem())previous=PendingIntent.getService(context,2034,RadioPlaybackService.internalIntent(context,RadioPlaybackService.PREVIOUS),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);if(player.hasNextMediaItem())next=PendingIntent.getService(context,2035,RadioPlaybackService.internalIntent(context,RadioPlaybackService.NEXT),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
  java.util.List<Integer> compact=new java.util.ArrayList<>();int index=0;
  if(previous!=null){builder.addAction(R.drawable.ic_previous,"Forrige",previous);compact.add(index++);}
  builder.addAction(playing&&!radio?(liveRadio?R.drawable.ic_stop:R.drawable.ic_pause):R.drawable.ic_play,context.getString(playing&&!radio?(liveRadio?R.string.stop_action:R.string.pause_action):R.string.play_action),toggle);compact.add(index++);
  if(next!=null){builder.addAction(R.drawable.ic_next,"Næste",next);compact.add(index++);}
  if(!liveRadio){builder.addAction(R.drawable.ic_stop,context.getString(R.string.stop_action),stop);if(compact.size()<3)compact.add(index);index++;}
  int[] visible=new int[compact.size()];for(int i=0;i<visible.length;i++)visible[i]=compact.get(i);builder.setStyle(new Notification.MediaStyle().setMediaSession(session.getPlatformToken()).setShowActionsInCompactView(visible));
  if(Build.VERSION.SDK_INT>=26) builder.setColorized(true);
  if(metadata.artworkData!=null) {android.graphics.Bitmap artwork=BitmapFactory.decodeByteArray(metadata.artworkData,0,metadata.artworkData.length);if(artwork!=null) builder.setLargeIcon(artwork);}
  return builder.build();
 }
}
