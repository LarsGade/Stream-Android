package dk.stream.android;

import android.app.*;
import android.os.*;
import androidx.media3.common.Player;
import androidx.media3.session.*;


/** Recreate a paused card without the foreground flag cached by an already open shade. */
@androidx.media3.common.util.UnstableApi
final class PausedNotificationRefresh {
 interface Factory {Notification get();}
 static void update(MediaSessionService service,MediaSession session,int id,Factory notification){
  if(session==null||!paused(session.getPlayer()))return;
  new Handler(Looper.getMainLooper()).post(()->{
   if(!paused(session.getPlayer()))return;
   androidx.core.app.ServiceCompat.stopForeground(service,androidx.core.app.ServiceCompat.STOP_FOREGROUND_REMOVE);
   NotificationManager manager=(NotificationManager)service.getSystemService(android.content.Context.NOTIFICATION_SERVICE);manager.cancel(id);
   service.onUpdateNotification(session,false);
   manager.notify(id,notification.get());
  });
 }
 private static boolean paused(Player player){return !player.getPlayWhenReady()&&player.getPlaybackState()!=Player.STATE_IDLE&&player.getMediaItemCount()>0;}
 private PausedNotificationRefresh(){}
}
