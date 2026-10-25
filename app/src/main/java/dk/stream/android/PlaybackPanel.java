package dk.stream.android;

import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import androidx.media3.common.Player;
import androidx.media3.ui.PlayerView;
import com.google.android.gms.cast.framework.CastSession;

/** One video viewport and one tap-to-reveal overlay in both windowed and fullscreen modes. */
@android.annotation.SuppressLint("ViewConstructor")
@androidx.media3.common.util.UnstableApi
public final class PlaybackPanel extends FrameLayout {
 public interface Actions {
  void togglePlayback();void stopPlayback();void toggleFullscreen();void volumeFailed();
  boolean playing();boolean fullscreen();CastSession castSession();
 }
 public final PlayerView video;
 private final Actions actions;
 private final AudioManager audio;
 private final ImageButton play,mute,fullscreen,rewind,forward,settings;
 public final SeekBar volume;
 private final SeekBar timeline;
 private final TextView title,time;
 private int restoreVolume;
 private final LinearLayout controls;
 private boolean fullscreenMode,tracking;
 private final Runnable hideControls=()->hideControls();
 private final Runnable progressTick=new Runnable() {
  @Override public void run() {if(controls.getVisibility()==VISIBLE) {refreshProgress();postDelayed(this,500);}}
 };
 public PlaybackPanel(Context context,Actions actions) {
  super(context);this.actions=actions;audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);setBackgroundColor(Color.BLACK);
  video=new PlayerView(context);video.setUseController(false);video.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER);
  addView(video,new FrameLayout.LayoutParams(-1,-1));
  video.setOnClickListener(view->{if(controlsVisible()) hideControls();else showControls();});
  video.setOnTouchListener((view,event)->{if(event.getActionMasked()==MotionEvent.ACTION_UP) view.performClick();return true;});
  controls=new LinearLayout(context);controls.setOrientation(LinearLayout.VERTICAL);controls.setBackgroundColor(0xDD101010);
  title=new TextView(context);title.setTextColor(Color.WHITE);title.setTextSize(13);title.setSingleLine();title.setPadding(dp(12),dp(4),dp(12),0);controls.addView(title);
  LinearLayout buttons=new LinearLayout(context);buttons.setGravity(Gravity.CENTER_VERTICAL);controls.addView(buttons,new LinearLayout.LayoutParams(-1,dp(44)));
  rewind=button(buttons,R.drawable.ic_rewind,R.string.seek_back,()->seekBy(-10000));
  play=button(buttons,R.drawable.ic_play,R.string.play_action,actions::togglePlayback);
  forward=button(buttons,R.drawable.ic_forward,R.string.seek_forward,()->seekBy(10000));
  mute=button(buttons,R.drawable.ic_volume,R.string.mute_action,this::toggleMute);
  button(buttons,R.drawable.ic_stop,R.string.stop_action,actions::stopPlayback);
  fullscreen=button(buttons,R.drawable.ic_fullscreen,R.string.fullscreen_action,actions::toggleFullscreen);
  LinearLayout progress=new LinearLayout(context);progress.setGravity(Gravity.CENTER_VERTICAL);progress.setPadding(dp(8),0,dp(8),0);controls.addView(progress,new LinearLayout.LayoutParams(-1,dp(32)));
  time=new TextView(context);time.setTextColor(Color.LTGRAY);time.setTextSize(11);progress.addView(time);
  timeline=new SeekBar(context);timeline.setMax(1000);timeline.setContentDescription(context.getString(R.string.seek_position));progress.addView(timeline,new LinearLayout.LayoutParams(0,-1,1));
  timeline.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
   public void onProgressChanged(SeekBar bar,int value,boolean fromUser) {if(fromUser && duration()>0) seekTo(duration()*value/1000);}
   public void onStartTrackingTouch(SeekBar bar) {tracking=true;removeCallbacks(hideControls);}
   public void onStopTrackingTouch(SeekBar bar) {tracking=false;scheduleHide();}
  });
  LinearLayout sound=new LinearLayout(context);sound.setGravity(Gravity.CENTER_VERTICAL);sound.setPadding(dp(12),0,dp(12),0);controls.addView(sound,new LinearLayout.LayoutParams(-1,dp(40)));
  TextView label=new TextView(context);label.setText(R.string.volume);label.setTextSize(12);label.setTextColor(Color.LTGRAY);sound.addView(label);
  volume=new SeekBar(context);volume.setContentDescription(context.getString(R.string.volume));sound.addView(volume,new LinearLayout.LayoutParams(0,-1,1));
  settings=new ImageButton(context);settings.setImageResource(android.R.drawable.ic_menu_preferences);settings.setImageTintList(android.content.res.ColorStateList.valueOf(Color.LTGRAY));settings.setBackgroundResource(android.R.drawable.list_selector_background);settings.setContentDescription(context.getString(R.string.player_settings));sound.addView(settings,new LinearLayout.LayoutParams(dp(44),-1));settings.setOnClickListener(this::showSettings);
  volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
   public void onProgressChanged(SeekBar bar,int value,boolean fromUser) {if(fromUser) setVolume(value);}
   public void onStartTrackingTouch(SeekBar bar) {tracking=true;removeCallbacks(hideControls);}
   public void onStopTrackingTouch(SeekBar bar) {tracking=false;scheduleHide();}
  });
  addView(controls,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
  androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(this,(view,insets)->{
   androidx.core.graphics.Insets cutout=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.displayCutout());
   controls.setPadding(fullscreenMode?cutout.left:0,0,fullscreenMode?cutout.right:0,fullscreenMode?cutout.bottom:0);return insets;
  });
  restoreVolume=Math.max(1,audio.getStreamVolume(AudioManager.STREAM_MUSIC));hideControls();refresh();setVisibility(GONE);
 }
 @Override protected void onMeasure(int widthSpec,int heightSpec) {
  if(!fullscreenMode && MeasureSpec.getMode(heightSpec)!=MeasureSpec.EXACTLY) heightSpec=MeasureSpec.makeMeasureSpec(dp(210),MeasureSpec.EXACTLY);
  super.onMeasure(widthSpec,heightSpec);
 }
 private int dp(int value) {return Math.round(value*getResources().getDisplayMetrics().density);}
 private ImageButton button(LinearLayout row,int icon,int label,Runnable click) {
  ImageButton button=new ImageButton(getContext());button.setImageResource(icon);button.setContentDescription(getContext().getString(label));button.setPadding(dp(10),dp(10),dp(10),dp(10));button.setBackgroundResource(android.R.drawable.list_selector_background);
  row.addView(button,new LinearLayout.LayoutParams(0,-1,1));button.setOnClickListener(view->{click.run();if(controlsVisible()) scheduleHide();});return button;
 }
 public void setTitle(CharSequence name) {title.setText(name);}
 public boolean controlsVisible() {return controls.getVisibility()==VISIBLE;}
 public void showControls() {controls.setVisibility(VISIBLE);refresh();removeCallbacks(progressTick);post(progressTick);scheduleHide();}
 public void hideControls() {removeCallbacks(hideControls);removeCallbacks(progressTick);controls.setVisibility(GONE);video.hideController();}
 public void fullscreenLayout(boolean enabled) {
  fullscreenMode=enabled;controls.setPadding(0,0,0,0);
  video.setResizeMode(enabled?androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM:androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
  hideControls();requestLayout();refresh();
 }
 private void scheduleHide() {removeCallbacks(hideControls);if(!tracking && controlsVisible()) postDelayed(hideControls,2500);}
 @Override public boolean dispatchTouchEvent(MotionEvent event) {
  if(event.getActionMasked()==MotionEvent.ACTION_DOWN) removeCallbacks(hideControls);
  boolean handled=super.dispatchTouchEvent(event);
  if(event.getActionMasked()==MotionEvent.ACTION_UP || event.getActionMasked()==MotionEvent.ACTION_CANCEL) scheduleHide();return handled;
 }
 @Override protected void onDetachedFromWindow() {removeCallbacks(hideControls);removeCallbacks(progressTick);super.onDetachedFromWindow();}
 private void showSettings(View anchor) {
  Player player=video.getPlayer();if(player==null || actions.castSession()!=null) return;
  removeCallbacks(hideControls);
  PopupMenu menu=new PopupMenu(getContext(),anchor);
  menu.getMenu().add(0,1,0,R.string.playback_speed);menu.getMenu().add(0,2,1,R.string.subtitles);menu.getMenu().add(0,3,2,R.string.audio_track);
  menu.setOnMenuItemClickListener(item->{
   if(item.getItemId()==1) {
    float[] speeds={0.5f,0.75f,1f,1.25f,1.5f,2f};String[] labels={"0,5×","0,75×","1×","1,25×","1,5×","2×"};
    int selected=2;for(int i=0;i<speeds.length;i++) if(speeds[i]==player.getPlaybackParameters().speed) selected=i;
    new androidx.appcompat.app.AlertDialog.Builder(getContext()).setTitle(R.string.playback_speed).setSingleChoiceItems(labels,selected,(dialog,which)->{player.setPlaybackSpeed(speeds[which]);dialog.dismiss();scheduleHide();}).setNegativeButton(android.R.string.cancel,null).show();
   } else {
    int type=item.getItemId()==2?androidx.media3.common.C.TRACK_TYPE_TEXT:androidx.media3.common.C.TRACK_TYPE_AUDIO;
    new androidx.media3.ui.TrackSelectionDialogBuilder(getContext(),getContext().getString(type==androidx.media3.common.C.TRACK_TYPE_TEXT?R.string.subtitles:R.string.audio_track),player,type).setShowDisableOption(type==androidx.media3.common.C.TRACK_TYPE_TEXT).build().show();
   }
   return true;
  });menu.setOnDismissListener(ignored->scheduleHide());menu.show();
 }
 private long duration() {
  CastSession session=actions.castSession();
  if(session!=null && session.getRemoteMediaClient()!=null) return session.getRemoteMediaClient().getStreamDuration();
  Player player=video.getPlayer();return player==null?0:Math.max(0,player.getDuration());
 }
 private long position() {
  CastSession session=actions.castSession();if(session!=null && session.getRemoteMediaClient()!=null) return session.getRemoteMediaClient().getApproximateStreamPosition();
  Player player=video.getPlayer();return player==null?0:Math.max(0,player.getCurrentPosition());
 }
 private boolean seekable() {
  CastSession session=actions.castSession();
  if(session!=null && session.getRemoteMediaClient()!=null) return !session.getRemoteMediaClient().isLiveStream() && duration()>0;
  Player player=video.getPlayer();return player!=null && player.isCurrentMediaItemSeekable() && duration()>0;
 }
 private void seekTo(long value) {
  if(!seekable()) return;long target=Math.max(0,Math.min(duration(),value));CastSession session=actions.castSession();
  if(session!=null && session.getRemoteMediaClient()!=null) session.getRemoteMediaClient().seek(target);
  else if(video.getPlayer()!=null) video.getPlayer().seekTo(target);
 }
 private void seekBy(long delta) {seekTo(position()+delta);refreshProgress();}
 private String clock(long ms) {long seconds=Math.max(0,ms/1000);return String.format(java.util.Locale.ROOT,"%d:%02d",seconds/60,seconds%60);}
 private void refreshProgress() {
  boolean enabled=seekable();timeline.setEnabled(enabled);rewind.setEnabled(enabled);forward.setEnabled(enabled);
  if(!tracking) timeline.setProgress(enabled?(int)(position()*1000/Math.max(1,duration())):0);
  time.setText(enabled?getContext().getString(R.string.playback_time,clock(position()),clock(duration())):getContext().getString(R.string.live));
 }
 public void refresh() {
  boolean playing=actions.playing();play.setImageResource(playing?R.drawable.ic_pause:R.drawable.ic_play);play.setContentDescription(getContext().getString(playing?R.string.pause_action:R.string.play_action));
  fullscreen.setEnabled(actions.castSession()==null);fullscreen.setImageResource(actions.fullscreen()?R.drawable.ic_fullscreen_exit:R.drawable.ic_fullscreen);fullscreen.setContentDescription(getContext().getString(actions.fullscreen()?R.string.exit_fullscreen:R.string.fullscreen_action));
  try {
   CastSession session=actions.castSession();int level;
   if(session!=null) {volume.setMax(100);level=session.isMute()?0:(int)Math.round(session.getVolume()*100);volume.setEnabled(true);mute.setEnabled(true);}
   else {volume.setMax(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));level=audio.getStreamVolume(AudioManager.STREAM_MUSIC);volume.setEnabled(!audio.isVolumeFixed());mute.setEnabled(!audio.isVolumeFixed());}
   volume.setProgress(level);mute.setImageResource(level==0?R.drawable.ic_mute:R.drawable.ic_volume);mute.setContentDescription(getContext().getString(level==0?R.string.unmute_action:R.string.mute_action));
  } catch(IllegalStateException e) {actions.volumeFailed();}
  settings.setEnabled(video.getPlayer()!=null && actions.castSession()==null);refreshProgress();
 }
 public void setVolume(int value) {
  try {
   CastSession session=actions.castSession();if(session!=null) {session.setMute(false);session.setVolume(Math.max(0,Math.min(100,value))/100.0);}
   else {audio.setStreamVolume(AudioManager.STREAM_MUSIC,Math.max(0,Math.min(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),value)),0);if(value>0) restoreVolume=value;}refresh();
  } catch(java.io.IOException|IllegalStateException|SecurityException e) {actions.volumeFailed();}
 }
 public void toggleMute() {
  try {
   CastSession session=actions.castSession();if(session!=null) session.setMute(!session.isMute());
   else {int current=audio.getStreamVolume(AudioManager.STREAM_MUSIC);if(current>0) {restoreVolume=current;audio.setStreamVolume(AudioManager.STREAM_MUSIC,0,0);} else audio.setStreamVolume(AudioManager.STREAM_MUSIC,Math.min(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),Math.max(1,restoreVolume)),0);}refresh();
  } catch(java.io.IOException|IllegalStateException|SecurityException e) {actions.volumeFailed();}
 }
}
