package dk.stream.android;

import android.app.*;
import android.os.*;
import androidx.media3.common.Player;
import androidx.media3.session.*;


/** Update the same paused card without removing it from the open notification shade. */
@androidx.media3.common.util.UnstableApi
final class PausedNotificationRefresh {
 interface Factory {Notification get();}
 static void update(MediaSessionService service,MediaSession session,int id,Factory notification){
  if(session==null||!paused(session.getPlayer()))return;
  new Handler(Looper.getMainLooper()).post(()->{
   if(!paused(session.getPlayer()))return;
   androidx.core.app.ServiceCompat.stopForeground(service,androidx.core.app.ServiceCompat.STOP_FOREGROUND_DETACH);
   NotificationManager manager=(NotificationManager)service.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
   service.onUpdateNotification(session,false);
   Notification paused=notification.get();paused.flags&=~(Notification.FLAG_ONGOING_EVENT|Notification.FLAG_FOREGROUND_SERVICE|Notification.FLAG_NO_CLEAR);manager.notify(id,paused);
  });
 }
 private static boolean paused(Player player){return !player.getPlayWhenReady()&&player.getPlaybackState()!=Player.STATE_IDLE&&player.getMediaItemCount()>0;}
 private PausedNotificationRefresh(){}
}
