package dk.stream.android;

import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.*;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

/** Full controls for local audio, radio and the current Cast receiver. */
@androidx.media3.common.util.UnstableApi
public final class AudioPlayerActivity extends AppCompatActivity {
 private TextView title,time;
 private SeekBar progress;
 private ImageButton toggle;
 private androidx.mediarouter.app.MediaRouteButton castButton;
 private boolean local,tracking;
 private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
 private final Runnable tick=new Runnable(){public void run(){refresh();handler.postDelayed(this,500);}};
 private Player player(){return local?LocalAudioService.player():RadioPlaybackService.audioPlayer();}
 private RemoteMediaClient remote(){return CastPlayback.hasMedia(this)?CastPlayback.remote(this):null;}
 @Override public void onCreate(Bundle state){
  super.onCreate(state);CastPlayback.init(this);local=getIntent().getBooleanExtra("local",false);setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(24,24,24,24);root.setBackgroundColor(0xFF101010);setContentView(root);
  androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{androidx.core.graphics.Insets bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());view.setPadding(bars.left+24,bars.top+24,bars.right+24,bars.bottom+24);return insets;});
  LinearLayout top=new LinearLayout(this);root.addView(top,new LinearLayout.LayoutParams(-1,-2));Button back=new Button(this);back.setText("Tilbage");top.addView(back,new LinearLayout.LayoutParams(0,-2,1));back.setOnClickListener(view->finish());
  androidx.mediarouter.app.MediaRouteButton cast=new androidx.mediarouter.app.MediaRouteButton(this){@Override public boolean performClick(){if(local&&android.os.Build.VERSION.SDK_INT>=37&&checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK")!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{"android.permission.ACCESS_LOCAL_NETWORK"},7301);return true;}return super.performClick();}};castButton=cast;top.addView(cast,new LinearLayout.LayoutParams(56,56));try{com.google.android.gms.cast.framework.CastButtonFactory.setUpMediaRouteButton(this,cast);}catch(RuntimeException ignored){}
  title=new TextView(this);title.setTextColor(Color.WHITE);title.setTextSize(24);title.setGravity(Gravity.CENTER);root.addView(title,new LinearLayout.LayoutParams(-1,0,1));
  time=new TextView(this);time.setTextColor(Color.LTGRAY);root.addView(time);
  progress=new SeekBar(this);progress.setMax(1000);progress.setContentDescription(getString(R.string.seek_position));root.addView(progress,new LinearLayout.LayoutParams(-1,-2));
  progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
   public void onProgressChanged(SeekBar bar,int value,boolean user){if(!user)return;RemoteMediaClient remote=remote();if(remote!=null){long duration=remote.getStreamDuration();if(duration>0)remote.seek(new com.google.android.gms.cast.MediaSeekOptions.Builder().setPosition(duration*value/1000).build());}else {Player player=player();if(player!=null&&player.getDuration()>0&&player.isCurrentMediaItemSeekable())player.seekTo(player.getDuration()*value/1000);}}
   public void onStartTrackingTouch(SeekBar bar){tracking=true;}public void onStopTrackingTouch(SeekBar bar){tracking=false;}
  });
  LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);root.addView(controls);
  toggle=icon(controls,R.drawable.ic_play,"Afspil");toggle.setOnClickListener(view->{RemoteMediaClient remote=remote();if(remote!=null)CastPlayback.toggle(this);else {Player player=player();if(player!=null){if(player.getPlaybackState()==Player.STATE_ENDED){player.seekTo(0);player.play();}else if(player.getPlayWhenReady())player.pause();else {if(player.getPlaybackState()==Player.STATE_IDLE)player.prepare();player.play();}}}refresh();});
  ImageButton stop=icon(controls,R.drawable.ic_stop,"Stop");stop.setOnClickListener(view->{if(remote()!=null)CastPlayback.stop(this);else if(local)startService(LocalAudioService.command(this,LocalAudioService.STOP));else startService(RadioPlaybackService.internalIntent(this,RadioPlaybackService.STOP));finish();});refresh();
 }
 private ImageButton icon(LinearLayout parent,int resource,String description){ImageButton button=new ImageButton(this);button.setImageResource(resource);button.setContentDescription(description);button.setBackgroundColor(Color.TRANSPARENT);int size=Math.round(64*getResources().getDisplayMetrics().density);parent.addView(button,new LinearLayout.LayoutParams(size,size));return button;}
 private String format(long ms){long seconds=Math.max(0,ms/1000);return String.format(java.util.Locale.ROOT,"%d:%02d",seconds/60,seconds%60);}
 private void refresh(){
  RemoteMediaClient remote=remote();Player player=player();long duration,position;boolean seekable,running;
  if(remote!=null){title.setText(CastPlayback.title(this));duration=remote.getStreamDuration();position=remote.getApproximateStreamPosition();seekable=duration>0&&remote.getMediaInfo().getStreamType()!=com.google.android.gms.cast.MediaInfo.STREAM_TYPE_LIVE;running=remote.isPlaying()||remote.isBuffering();}
  else {if(player==null||player.getPlaybackState()==Player.STATE_IDLE){finish();return;}CharSequence name=player.getMediaMetadata().title;title.setText(name);duration=player.getDuration();position=player.getCurrentPosition();seekable=duration>0&&duration!=C.TIME_UNSET&&player.isCurrentMediaItemSeekable();running=player.getPlayWhenReady()&&player.getPlaybackState()!=Player.STATE_ENDED;}
  progress.setEnabled(seekable);if(!tracking)progress.setProgress(seekable?(int)Math.min(1000,position*1000/duration):0);time.setText(format(position)+(duration>0?" / "+format(duration):""));toggle.setImageResource(running?R.drawable.ic_pause:R.drawable.ic_play);toggle.setContentDescription(running?"Pause":"Afspil");
 }
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==7301&&results.length>0&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)castButton.performClick();}
 @Override protected void onStart(){super.onStart();handler.post(tick);}
 @Override protected void onStop(){handler.removeCallbacks(tick);super.onStop();}
}
