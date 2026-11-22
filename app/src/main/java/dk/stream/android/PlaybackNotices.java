package dk.stream.android;
import android.app.*;
import android.content.*;
import android.os.Build;
@androidx.media3.common.util.UnstableApi
final class PlaybackNotices {
 static PendingIntent open(Context context,int request){return PendingIntent.getActivity(context,request,new Intent(context,MainActivity.class).putExtra("openFullPlayer",true).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static String channelName(String id){return id.equals("stream_radio")?"Radio":id.equals("stream_local")?"Musik og filer":id.equals("stream_car_audio")?"Android Auto – lyd":"Afspilning";}
 static void rename(Context context){if(Build.VERSION.SDK_INT<26)return;NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);for(String id:new String[]{"stream_radio","stream_local","stream_car_audio"}){NotificationChannel channel=manager.getNotificationChannel(id);if(channel!=null){channel.setName(UiText.text(channelName(id)));manager.createNotificationChannel(channel);}}}
 private PlaybackNotices(){}
}
