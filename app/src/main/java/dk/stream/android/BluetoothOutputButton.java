package dk.stream.android;
import android.content.*;
import android.media.*;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import androidx.media3.exoplayer.ExoPlayer;
import java.util.*;

/** Routes this app's AudioTracks with the public preferred-device API (Android 6+). */
@androidx.media3.common.util.UnstableApi
@android.annotation.SuppressLint("ViewConstructor")
final class BluetoothOutputButton extends androidx.appcompat.widget.AppCompatImageButton {
 private static final Map<ExoPlayer,Context> players=new WeakHashMap<>();
 private final Runnable tick=new Runnable(){public void run(){refresh();postDelayed(this,1000);}};
 BluetoothOutputButton(Context context){super(context);setImageResource(R.drawable.ic_bluetooth);setBackgroundColor(Color.TRANSPARENT);int pad=Math.round(12*getResources().getDisplayMetrics().density);setPadding(pad,pad,pad,pad);setVisibility(GONE);setContentDescription("Vælg Bluetooth-enhed eller telefonhøjttaler");setOnClickListener(view->choose());}
 static boolean streamPlaying(){for(ExoPlayer player:new ArrayList<>(players.keySet()))try{if(player.getPlayWhenReady()&&player.getPlaybackSuppressionReason()==androidx.media3.common.Player.PLAYBACK_SUPPRESSION_REASON_NONE&&player.getPlaybackState()!=androidx.media3.common.Player.STATE_IDLE&&player.getPlaybackState()!=androidx.media3.common.Player.STATE_ENDED&&player.getPlayerError()==null)return true;}catch(IllegalStateException ignored){}return false;}
 private static boolean bluetooth(AudioDeviceInfo info){int type=info.getType();return type==AudioDeviceInfo.TYPE_BLUETOOTH_A2DP||type==AudioDeviceInfo.TYPE_BLUETOOTH_SCO||type==26||type==27||type==30;}
 static AudioDeviceInfo preferred(Context context){AudioManager manager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);android.content.SharedPreferences prefs=context.getSharedPreferences("audio_output",0);int id=prefs.getInt("device",-1),type=prefs.getInt("type",-1);String name=prefs.getString("name","");for(AudioDeviceInfo info:manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)){if(type==AudioDeviceInfo.TYPE_BUILTIN_SPEAKER&&info.getType()==type)return info;if(!name.isEmpty()){if(info.getType()==type&&name.equals(info.getProductName().toString()))return info;}else if(info.getId()==id&&(type<0||info.getType()==type))return info;}return null;}

 static void register(Context context,ExoPlayer player){register(context,player,player::play);}
 static void unregister(ExoPlayer player){players.remove(player);BluetoothPlayback.unregister(player);}
 static void register(Context context,ExoPlayer player,Runnable resume){BluetoothPlayback.register(context,player,resume);players.put(player,context.getApplicationContext());try{player.setPreferredAudioDevice(preferred(context));}catch(RuntimeException unsupported){}}
 private void refresh(){boolean available=getContext() instanceof MainActivity?((MainActivity)getContext()).externalPlaybackAvailable():ExternalPlaybackActivity.available(getContext());setVisibility(available?View.VISIBLE:View.GONE);AudioDeviceInfo selected=preferred(getContext());AudioManager manager=(AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);boolean active=false;try{active=selected!=null?bluetooth(selected):manager.isBluetoothA2dpOn();}catch(SecurityException unavailable){}if(CastPlayback.hasMedia(getContext()))active=false;setImageTintList(android.content.res.ColorStateList.valueOf(active?0xFF3686FF:Color.WHITE));}
 private void choose(){if(CastPlayback.connected(getContext())){Toast.makeText(getContext(),"Stop casting for at vælge lydudgang på telefonen.",Toast.LENGTH_LONG).show();return;}
  AudioManager manager=(AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);List<AudioDeviceInfo> devices=new ArrayList<>();List<String> names=new ArrayList<>();for(AudioDeviceInfo info:manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS))if(info.getType()==AudioDeviceInfo.TYPE_BUILTIN_SPEAKER||bluetooth(info)&&info.getType()!=AudioDeviceInfo.TYPE_BLUETOOTH_SCO){devices.add(info);names.add(info.getType()==AudioDeviceInfo.TYPE_BUILTIN_SPEAKER?"Telefonens højttaler":info.getProductName().toString());}
  AudioDeviceInfo selected=preferred(getContext());if(selected==null){boolean wireless=false;try{wireless=manager.isBluetoothA2dpOn();}catch(SecurityException ignored){}for(AudioDeviceInfo device:devices)if(wireless?bluetooth(device):device.getType()==AudioDeviceInfo.TYPE_BUILTIN_SPEAKER){selected=device;break;}}
  CharSequence[] labels=new CharSequence[names.size()];for(int i=0;i<labels.length;i++)labels[i]=selected!=null&&devices.get(i).getId()==selected.getId()?OutputDeviceLabel.active(names.get(i)):names.get(i);new androidx.appcompat.app.AlertDialog.Builder(getContext()).setTitle("Vælg lydudgang").setItems(labels,(dialog,position)->{AudioDeviceInfo device=devices.get(position);BluetoothPlayback.outputChanged();getContext().getSharedPreferences("audio_output",0).edit().putInt("device",device.getId()).putInt("type",device.getType()).putString("name",device.getProductName().toString()).apply();for(Map.Entry<ExoPlayer,Context> entry:new ArrayList<>(players.entrySet()))try{entry.getKey().setPreferredAudioDevice(device);}catch(IllegalStateException released){}refresh();}).setNeutralButton("Bluetooth-indstillinger",(dialog,which)->{try{getContext().startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));}catch(ActivityNotFoundException error){Toast.makeText(getContext(),"Bluetooth-indstillinger kunne ikke åbnes.",Toast.LENGTH_LONG).show();}}).setNegativeButton("Annuller",null).show();
 }
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();post(tick);}
 @Override protected void onDetachedFromWindow(){removeCallbacks(tick);super.onDetachedFromWindow();}
}
