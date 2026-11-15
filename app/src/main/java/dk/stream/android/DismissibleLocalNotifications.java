package dk.stream.android;

import android.app.*;
import android.os.Build;
import androidx.media3.session.*;
import com.google.common.collect.ImmutableList;

/** Preserve Media3's controls and artwork while routing a swipe to the owned stop command. */
@androidx.media3.common.util.UnstableApi
final class DismissibleLocalNotifications implements MediaNotification.Provider {
 private final LocalAudioService service;private final DefaultMediaNotificationProvider delegate;
 DismissibleLocalNotifications(LocalAudioService service){this.service=service;delegate=new DefaultMediaNotificationProvider.Builder(service).setNotificationId(2003).setChannelId("stream_local").build();}
 @Override public MediaNotification createNotification(MediaSession session,ImmutableList<CommandButton> buttons,MediaNotification.ActionFactory factory,MediaNotification.Provider.Callback callback){
  return dismissible(session,delegate.createNotification(session,buttons,factory,notification->callback.onNotificationChanged(dismissible(session,notification))));
 }
 private MediaNotification dismissible(MediaSession session,MediaNotification original){
  PendingIntent stop=PendingIntent.getService(service,32,LocalAudioService.command(service,LocalAudioService.STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification notification;
  if(Build.VERSION.SDK_INT>=24)notification=Notification.Builder.recoverBuilder(service,original.notification).setOngoing(session.getPlayer().getPlayWhenReady()).setDeleteIntent(stop).build();
  else {PendingIntent toggle=PendingIntent.getService(service,31,LocalAudioService.command(service,LocalAudioService.TOGGLE),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);notification=PlaybackNotification.create(service,"stream_local",session,toggle,stop);}
  return new MediaNotification(original.notificationId,notification);
 }
 @Override public boolean handleCustomCommand(MediaSession session,String action,android.os.Bundle extras){return delegate.handleCustomCommand(session,action,extras);}
}
