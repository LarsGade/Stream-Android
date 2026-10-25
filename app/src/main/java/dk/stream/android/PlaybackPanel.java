package dk.stream.android;

import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.media3.ui.PlayerView;
import com.google.android.gms.cast.framework.CastSession;

/** Always-visible transport, fullscreen and volume controls alongside Media3's timeline. */
// Constructed only in code: a host callback is required, so XML inflation is unsupported.
@android.annotation.SuppressLint("ViewConstructor")
@androidx.media3.common.util.UnstableApi
public final class PlaybackPanel extends LinearLayout {
 public interface Actions {
  void togglePlayback();void stopPlayback();void toggleFullscreen();void volumeFailed();
  boolean playing();boolean fullscreen();CastSession castSession();
 }
 public final PlayerView video;
 private final Actions actions;
 private final AudioManager audio;
 private final ImageButton play,mute,fullscreen;
 public final SeekBar volume;
 private int restoreVolume;
 public PlaybackPanel(Context context,Actions actions) {
  super(context);this.actions=actions;audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
  setOrientation(VERTICAL);setBackgroundColor(Color.BLACK);
  video=new PlayerView(context);video.setUseController(true);video.setControllerShowTimeoutMs(0);video.setControllerHideOnTouch(false);
  video.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);video.setShowSubtitleButton(true);video.setShowNextButton(false);video.setShowPreviousButton(false);
  addView(video,new LayoutParams(-1,dp(180)));
  LinearLayout buttons=new LinearLayout(context);buttons.setGravity(Gravity.CENTER_VERTICAL);addView(buttons,new LayoutParams(-1,dp(48)));
  play=button(buttons,R.drawable.ic_play,R.string.play_action,actions::togglePlayback);
  mute=button(buttons,R.drawable.ic_volume,R.string.mute_action,this::toggleMute);
  button(buttons,R.drawable.ic_stop,R.string.stop_action,actions::stopPlayback);
  fullscreen=button(buttons,R.drawable.ic_fullscreen,R.string.fullscreen_action,actions::toggleFullscreen);
  LinearLayout sound=new LinearLayout(context);sound.setGravity(Gravity.CENTER_VERTICAL);sound.setPadding(dp(12),0,dp(12),0);addView(sound,new LayoutParams(-1,dp(48)));
  TextView label=new TextView(context);label.setText(R.string.volume);label.setTextColor(Color.LTGRAY);sound.addView(label);
  volume=new SeekBar(context);volume.setContentDescription(context.getString(R.string.volume));sound.addView(volume,new LayoutParams(0,-1,1));
  volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
   public void onProgressChanged(SeekBar bar,int value,boolean fromUser) {if(fromUser) setVolume(value);}
   public void onStartTrackingTouch(SeekBar bar) { }
   public void onStopTrackingTouch(SeekBar bar) { }
  });
  restoreVolume=Math.max(1,audio.getStreamVolume(AudioManager.STREAM_MUSIC));refresh();setVisibility(GONE);
 }
 private int dp(int value) {return Math.round(value*getResources().getDisplayMetrics().density);}
 private ImageButton button(LinearLayout row,int icon,int label,Runnable click) {
  ImageButton button=new ImageButton(getContext());button.setImageResource(icon);button.setContentDescription(getContext().getString(label));button.setPadding(dp(12),dp(12),dp(12),dp(12));button.setBackgroundResource(android.R.drawable.list_selector_background);
  row.addView(button,new LayoutParams(0,-1,1));button.setOnClickListener(view->click.run());return button;
 }
 public void fullscreenLayout(boolean enabled) {video.setLayoutParams(enabled?new LayoutParams(-1,0,1):new LayoutParams(-1,dp(180)));refresh();}
 public void refresh() {
  boolean playing=actions.playing();play.setImageResource(playing?R.drawable.ic_pause:R.drawable.ic_play);play.setContentDescription(getContext().getString(playing?R.string.pause_action:R.string.play_action));
  boolean cast=actions.castSession()!=null;fullscreen.setEnabled(!cast);fullscreen.setImageResource(actions.fullscreen()?R.drawable.ic_fullscreen_exit:R.drawable.ic_fullscreen);
  fullscreen.setContentDescription(getContext().getString(actions.fullscreen()?R.string.exit_fullscreen:R.string.fullscreen_action));
  try {
   CastSession session=actions.castSession();int level;
   if(session!=null) {volume.setMax(100);level=session.isMute()?0:(int)Math.round(session.getVolume()*100);volume.setEnabled(true);mute.setEnabled(true);}
   else {volume.setMax(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));level=audio.getStreamVolume(AudioManager.STREAM_MUSIC);volume.setEnabled(!audio.isVolumeFixed());mute.setEnabled(!audio.isVolumeFixed());}
   volume.setProgress(level);mute.setImageResource(level==0?R.drawable.ic_mute:R.drawable.ic_volume);mute.setContentDescription(getContext().getString(level==0?R.string.unmute_action:R.string.mute_action));
  } catch(IllegalStateException e) {actions.volumeFailed();}
 }
 public void setVolume(int value) {
  try {
   CastSession session=actions.castSession();
   if(session!=null) {session.setMute(false);session.setVolume(Math.max(0,Math.min(100,value))/100.0);}
   else {audio.setStreamVolume(AudioManager.STREAM_MUSIC,Math.max(0,Math.min(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),value)),0);if(value>0) restoreVolume=value;}
   refresh();
  } catch(java.io.IOException|IllegalStateException|SecurityException e) {actions.volumeFailed();}
 }
 public void toggleMute() {
  try {
   CastSession session=actions.castSession();
   if(session!=null) session.setMute(!session.isMute());
   else {int current=audio.getStreamVolume(AudioManager.STREAM_MUSIC);if(current>0) {restoreVolume=current;audio.setStreamVolume(AudioManager.STREAM_MUSIC,0,0);} else audio.setStreamVolume(AudioManager.STREAM_MUSIC,Math.min(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),Math.max(1,restoreVolume)),0);}
   refresh();
  } catch(java.io.IOException|IllegalStateException|SecurityException e) {actions.volumeFailed();}
 }
}
