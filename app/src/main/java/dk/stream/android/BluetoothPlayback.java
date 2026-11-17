package dk.stream.android;
import android.content.*;
import android.media.*;
import android.os.*;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import java.util.*;

/** Tracks Bluetooth output changes for all app players; no media type has priority. */
@androidx.media3.common.util.UnstableApi
final class BluetoothPlayback {
 private static final BluetoothResumeState<ExoPlayer> state=new BluetoothResumeState<>();
 private static final Map<ExoPlayer,Registration> players=new IdentityHashMap<>();
 private static final Handler handler=new Handler(Looper.getMainLooper());
 private static AudioManager manager;private static Context application;private static boolean connected,automaticPause;
 private static boolean bluetooth(AudioDeviceInfo device){int type=device.getType();return type==AudioDeviceInfo.TYPE_BLUETOOTH_A2DP||type==26||type==27||type==30;}
 private static boolean connected(){for(AudioDeviceInfo device:manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS))if(bluetooth(device))return true;return false;}
 private static boolean phoneSelected(){return application.getSharedPreferences("audio_output",0).getInt("type",-1)==AudioDeviceInfo.TYPE_BUILTIN_SPEAKER;}
 private static final Runnable reconnect=()->{if(manager==null||!connected||phoneSelected()||CastPlayback.connected(application))return;ExoPlayer player=state.take();if(player!=null)resume(player);};
 private static final AudioDeviceCallback devices=new AudioDeviceCallback(){
  @Override public void onAudioDevicesRemoved(AudioDeviceInfo[] removed){boolean was=connected;connected=connected();if(was&&!connected&&!phoneSelected()){ExoPlayer owner=state.owner();if(owner!=null&&state.eligible()){state.interrupted(owner);automaticPause=true;try{owner.pause();}finally{automaticPause=false;}}}}
  @Override public void onAudioDevicesAdded(AudioDeviceInfo[] added){boolean was=connected;connected=connected();if(!was&&connected){handler.removeCallbacks(reconnect);handler.postDelayed(reconnect,500);}}
 };
 static void register(Context context,ExoPlayer player,Runnable resume){if(players.containsKey(player))return;if(manager==null){application=context.getApplicationContext();manager=(AudioManager)application.getSystemService(Context.AUDIO_SERVICE);connected=connected();manager.registerAudioDeviceCallback(devices,handler);}Registration entry=new Registration(player,resume);players.put(player,entry);player.addListener(entry);if(player.getPlayWhenReady())state.started(player);}
 static void unregister(ExoPlayer player){Registration entry=players.remove(player);if(entry!=null)player.removeListener(entry);state.stopped(player);if(players.isEmpty()&&manager!=null){manager.unregisterAudioDeviceCallback(devices);handler.removeCallbacks(reconnect);manager=null;application=null;connected=false;}}
 static void clear(){state.clear();handler.removeCallbacks(reconnect);}
 static void outputChanged(){state.cancelPending();handler.removeCallbacks(reconnect);}
 private static void resume(ExoPlayer player){Registration entry=players.get(player);if(entry==null||player.getMediaItemCount()==0||player.getPlaybackState()==Player.STATE_ENDED)return;try{player.setPreferredAudioDevice(BluetoothOutputButton.preferred(application));entry.resume.run();}catch(RuntimeException unavailable){state.stopped(player);}}
 static boolean resumeOther(ExoPlayer requester){ExoPlayer owner=state.owner();if(owner==null||owner==requester||!players.containsKey(owner)||owner.getMediaItemCount()==0)return false;if(CastPlayback.connected(application)||state.pending()==owner&&!connected)return true;resume(owner);return true;}
 private static final class Registration implements Player.Listener {
  final ExoPlayer player;final Runnable resume;Registration(ExoPlayer player,Runnable resume){this.player=player;this.resume=resume;}
  @Override public void onPlayWhenReadyChanged(boolean ready,int reason){if(ready)state.started(player);else if(reason==Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY&&(connected||state.pending()==player)&&!phoneSelected())state.interrupted(player);else if(reason==Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST&&!automaticPause)state.paused(player);}
  @Override public void onPlaybackStateChanged(int value){if(value==Player.STATE_IDLE||value==Player.STATE_ENDED)state.stopped(player);}
 }
 private BluetoothPlayback(){}
}
