package dk.stream.android;

import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.*;

/** Full audio controls for the same player shown in the bottom bar. */
@androidx.media3.common.util.UnstableApi
public final class AudioPlayerActivity extends AppCompatActivity {
 private TextView title,time;
 private SeekBar progress;
 private Button toggle;
 private boolean local,tracking;
 private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
 private final Runnable tick=new Runnable() {public void run() {refresh();handler.postDelayed(this,500);}};
 private Player player() {return local?LocalAudioService.player():RadioPlaybackService.audioPlayer();}
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);local=getIntent().getBooleanExtra("local",false);setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(24,24,24,24);root.setBackgroundColor(0xFF101010);setContentView(root);
  androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{androidx.core.graphics.Insets bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());view.setPadding(bars.left+24,bars.top+24,bars.right+24,bars.bottom+24);return insets;});
  Button back=new Button(this);back.setText("Tilbage");root.addView(back);back.setOnClickListener(v->finish());
  title=new TextView(this);title.setTextColor(Color.WHITE);title.setTextSize(24);title.setGravity(Gravity.CENTER);root.addView(title,new LinearLayout.LayoutParams(-1,0,1));
  time=new TextView(this);time.setTextColor(Color.LTGRAY);root.addView(time);
  progress=new SeekBar(this);progress.setMax(1000);progress.setContentDescription(getString(R.string.seek_position));root.addView(progress,new LinearLayout.LayoutParams(-1,-2));
  progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
   public void onProgressChanged(SeekBar bar,int value,boolean user) {if(user) {Player p=player();if(p!=null && p.getDuration()>0 && p.isCurrentMediaItemSeekable()) p.seekTo(p.getDuration()*value/1000);}}
   public void onStartTrackingTouch(SeekBar bar) {tracking=true;}
   public void onStopTrackingTouch(SeekBar bar) {tracking=false;}
  });
  LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);root.addView(controls);
  toggle=new Button(this);controls.addView(toggle);toggle.setOnClickListener(v->{Player p=player();if(p!=null) {if(p.getPlaybackState()==Player.STATE_ENDED) {p.seekTo(0);p.play();}else if(p.getPlayWhenReady()) p.pause();else p.play();refresh();}});
  Button stop=new Button(this);stop.setText("Stop");controls.addView(stop);stop.setOnClickListener(v->{if(local) startService(LocalAudioService.command(this,LocalAudioService.STOP));else startService(RadioPlaybackService.internalIntent(this,RadioPlaybackService.STOP));finish();});refresh();
 }
 private String format(long ms) {long seconds=Math.max(0,ms/1000);return String.format(java.util.Locale.ROOT,"%d:%02d",seconds/60,seconds%60);}
 private void refresh() {
  Player p=player();if(p==null || p.getPlaybackState()==Player.STATE_IDLE) {finish();return;}
  CharSequence name=p.getMediaMetadata().displayTitle;if(name==null) name=p.getMediaMetadata().title;title.setText(name);
  long duration=p.getDuration(),position=p.getCurrentPosition();boolean seekable=duration>0&&duration!=C.TIME_UNSET&&p.isCurrentMediaItemSeekable();progress.setEnabled(seekable);
  if(!tracking) progress.setProgress(seekable?(int)Math.min(1000,position*1000/duration):0);
  toggle.setCompoundDrawablesWithIntrinsicBounds(p.getPlayWhenReady()&&p.getPlaybackState()!=Player.STATE_ENDED?R.drawable.ic_pause:R.drawable.ic_play,0,0,0);time.setText(format(position)+(duration>0?" / "+format(duration):""));toggle.setText(p.getPlayWhenReady()&&p.getPlaybackState()!=Player.STATE_ENDED?"Pause":"Afspil");
 }
 @Override protected void onStart() {super.onStart();handler.post(tick);}
 @Override protected void onStop() {handler.removeCallbacks(tick);super.onStop();}
}
