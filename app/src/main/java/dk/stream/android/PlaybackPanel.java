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
  boolean playing();boolean fullscreen();CastSession castSession();default FavoriteLists.Item downloadItem(){return null;}default boolean recordingAvailable(){return false;}default boolean recording(){return false;}default void toggleRecording(){}default boolean backupAvailable(){return false;}default void switchBackup(){}default boolean liveChannel(){return false;}default void goLive(){}default void chooseCast(){}default boolean pictureInPicture(){return false;}
 }
 public final PlayerView video;
 private final Actions actions;
 private final AudioManager audio;
 private final ImageButton play,stop,mute,fullscreen,settings,back;
 private final SeekBar timeline;
 private final TextView title,time;
 private boolean liveWasPaused;private long liveBaseline=androidx.media3.common.C.TIME_UNSET;
 private int restoreVolume;
 private final LinearLayout controls,outputs,buttons,progress;
 private int compactControlsWidth;
 private final CastOutputButton castOutput;
 private boolean fullscreenMode,tracking;private int outputCutoutTop,outputCutoutRight;
 private final Runnable hideControls=()->hideControls();
 private final Runnable progressTick=new Runnable() {
  @Override public void run() {if(controls.getVisibility()==VISIBLE) {refreshProgress();postDelayed(this,500);}}
 };
 private ImageButton backup,record;
 public PlaybackPanel(Context context,Actions actions) {
  super(context);this.actions=actions;audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);setBackgroundColor(Color.BLACK);
  video=new PlayerView(context);video.setUseController(false);video.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER);
  addView(video,new FrameLayout.LayoutParams(-1,-1));
  video.setOnClickListener(view->{if(controlsVisible()) hideControls();else showControls();});
  video.setOnTouchListener((view,event)->{if(event.getActionMasked()==MotionEvent.ACTION_UP) view.performClick();return true;});
  controls=new LinearLayout(context);controls.setOrientation(LinearLayout.VERTICAL);controls.setBackgroundColor(0xDD101010);
  title=new TextView(context);title.setTextColor(Color.WHITE);title.setTextSize(13);title.setSingleLine();title.setOnLongClickListener(view->{FavoriteLists.Item item=actions.downloadItem();if(!MediaDownloads.eligible(item))return false;MediaDownloads.show(title,getContext(),item);return true;});title.setPadding(dp(12),dp(4),dp(12),0);controls.addView(title);
  buttons=new LinearLayout(context);buttons.setGravity(Gravity.CENTER_VERTICAL);controls.addView(buttons,new LinearLayout.LayoutParams(-1,dp(44)));
  play=button(buttons,R.drawable.ic_play,R.string.play_action,actions::togglePlayback);
  stop=button(buttons,R.drawable.ic_stop,R.string.stop_action,actions::stopPlayback);
  mute=button(buttons,R.drawable.ic_volume,R.string.mute_action,this::toggleMute);
  fullscreen=button(buttons,R.drawable.ic_fullscreen,R.string.fullscreen_action,actions::toggleFullscreen);
  backup=button(buttons,R.drawable.ic_tv_backup,R.string.tv_backup_next,actions::switchBackup);backup.setVisibility(GONE);
  record=button(buttons,R.drawable.ic_record,R.string.record_tv,actions::toggleRecording);record.setVisibility(GONE);
  progress=new LinearLayout(context);progress.setGravity(Gravity.CENTER_VERTICAL);progress.setPadding(dp(8),0,dp(8),0);controls.addView(progress,new LinearLayout.LayoutParams(-1,dp(44)));
  time=new TextView(context);time.setTextColor(Color.LTGRAY);time.setTextSize(11);progress.addView(time);
  timeline=new SeekBar(context);timeline.setMax(1000);timeline.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF3686FF));timeline.setThumbTintList(android.content.res.ColorStateList.valueOf(0xFF3686FF));timeline.setContentDescription(context.getString(R.string.seek_position));progress.addView(timeline,new LinearLayout.LayoutParams(0,-1,1));
  timeline.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
   public void onProgressChanged(SeekBar bar,int value,boolean fromUser) {if(fromUser && duration()>0) seekTo(duration()*value/1000);}
   public void onStartTrackingTouch(SeekBar bar) {tracking=true;removeCallbacks(hideControls);}
   public void onStopTrackingTouch(SeekBar bar) {tracking=false;scheduleHide();}
  });
  settings=new ImageButton(context);settings.setImageResource(R.drawable.ic_settings);settings.setImageTintList(android.content.res.ColorStateList.valueOf(Color.LTGRAY));settings.setBackground(controlBackground());settings.setContentDescription(context.getString(R.string.player_settings));settings.setPadding(dp(8),dp(8),dp(8),dp(8));buttons.addView(settings,new LinearLayout.LayoutParams(0,-1,1));settings.setOnClickListener(this::showSettings);
  addView(controls,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
  back=new ImageButton(context);back.setImageResource(R.drawable.ic_back);back.setContentDescription("Tilbage");back.setImageTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));back.setBackgroundColor(Color.TRANSPARENT);back.setPadding(dp(12),dp(12),dp(12),dp(12));FrameLayout.LayoutParams backPosition=new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.TOP|Gravity.START);backPosition.setMargins(dp(8),dp(8),dp(8),dp(8));addView(back,backPosition);back.setVisibility(GONE);back.setOnClickListener(view->{if(actions.fullscreen())actions.toggleFullscreen();});
  outputs=new LinearLayout(context);outputs.setGravity(Gravity.CENTER_VERTICAL);outputs.setBackgroundColor(Color.TRANSPARENT);outputs.addView(new BluetoothOutputButton(context),new LinearLayout.LayoutParams(dp(48),dp(48)));
  castOutput=new CastOutputButton(context){@Override public boolean performClick(){if(android.os.Build.VERSION.SDK_INT>=37&&context instanceof android.app.Activity&&context.checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK")!=android.content.pm.PackageManager.PERMISSION_GRANTED){((android.app.Activity)context).requestPermissions(new String[]{"android.permission.ACCESS_LOCAL_NETWORK"},7301);return true;}return super.performClick();}};castOutput.setBackgroundColor(Color.TRANSPARENT);outputs.addView(castOutput,new LinearLayout.LayoutParams(dp(48),dp(48)));FrameLayout.LayoutParams outputPosition=new FrameLayout.LayoutParams(-2,dp(48),Gravity.TOP|Gravity.END);outputPosition.setMargins(0,0,0,0);addView(outputs,outputPosition);
  androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(this,(view,insets)->{
   androidx.core.graphics.Insets cutout=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.displayCutout());
   controls.setPadding(fullscreenMode?cutout.left:0,0,fullscreenMode?cutout.right:0,fullscreenMode?cutout.bottom:0);FrameLayout.LayoutParams position=(FrameLayout.LayoutParams)back.getLayoutParams();position.setMarginStart(dp(8)+cutout.left);position.topMargin=dp(8)+cutout.top;back.setLayoutParams(position);outputCutoutTop=cutout.top;outputCutoutRight=cutout.right;positionOutputs();return insets;
  });
  restoreVolume=Math.max(1,audio.getStreamVolume(AudioManager.STREAM_MUSIC));hideControls();refresh();setVisibility(GONE);
 }
 @Override protected void onMeasure(int widthSpec,int heightSpec) {
  if(!fullscreenMode && MeasureSpec.getMode(heightSpec)!=MeasureSpec.EXACTLY) heightSpec=MeasureSpec.makeMeasureSpec(dp(210),MeasureSpec.EXACTLY);
  int width=MeasureSpec.getSize(widthSpec);if(!fullscreenMode&&width>0)compactControlsWidth=width;
  int reference=compactControlsWidth>0?compactControlsWidth:Math.min(getResources().getDisplayMetrics().widthPixels,getResources().getDisplayMetrics().heightPixels);int rowWidth=fullscreenMode?Math.min(Math.max(0,width-controls.getPaddingLeft()-controls.getPaddingRight()),reference):-1;LinearLayout.LayoutParams buttonPosition=(LinearLayout.LayoutParams)buttons.getLayoutParams();if(buttonPosition.width!=rowWidth){buttonPosition.width=rowWidth;buttonPosition.gravity=Gravity.CENTER_HORIZONTAL;buttons.setLayoutParams(buttonPosition);}
  super.onMeasure(widthSpec,heightSpec);
 }
 private android.graphics.drawable.Drawable controlBackground() {return new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x663686FF),null,new android.graphics.drawable.ColorDrawable(Color.WHITE));}
 private int dp(int value) {return Math.round(value*getResources().getDisplayMetrics().density);}
 private ImageButton button(LinearLayout row,int icon,int label,Runnable click) {
  ImageButton button=new ImageButton(getContext());button.setImageResource(icon);button.setContentDescription(getContext().getString(label));button.setPadding(dp(8),dp(8),dp(8),dp(8));button.setBackground(controlBackground());
  row.addView(button,new LinearLayout.LayoutParams(0,-1,1));button.setOnClickListener(view->{click.run();if(controlsVisible()) scheduleHide();});return button;
 }
 public void setTitle(CharSequence name) {if(!android.text.TextUtils.equals(title.getText(),name)){liveWasPaused=false;liveBaseline=androidx.media3.common.C.TIME_UNSET;}title.setText(name);}
 public androidx.mediarouter.app.MediaRouteButton castButton(){return castOutput;}
 public boolean controlsVisible() {return controls.getVisibility()==VISIBLE;}
 public void showControls() {controls.setVisibility(VISIBLE);outputs.setVisibility(VISIBLE);refresh();removeCallbacks(progressTick);post(progressTick);scheduleHide();}
 public void hideControls() {removeCallbacks(hideControls);removeCallbacks(progressTick);controls.setVisibility(GONE);outputs.setVisibility(GONE);back.setVisibility(GONE);video.hideController();}
 public void fullscreenLayout(boolean enabled) {
  fullscreenMode=enabled;outputs.setBackgroundColor(Color.TRANSPARENT);castOutput.setBackgroundColor(Color.TRANSPARENT);controls.setPadding(0,0,0,0);positionOutputs();
  video.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
  hideControls();requestLayout();refresh();
 }
 private void positionOutputs(){FrameLayout.LayoutParams position=(FrameLayout.LayoutParams)outputs.getLayoutParams();position.topMargin=fullscreenMode?dp(8)+outputCutoutTop:0;position.setMarginEnd(fullscreenMode?dp(8)+outputCutoutRight:0);outputs.setLayoutParams(position);}
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
  PopupMenu menu=new StablePopupMenu(getContext(),anchor);
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
 private String clock(long ms) {long seconds=Math.max(0,ms/1000);return String.format(java.util.Locale.ROOT,"%d:%02d",seconds/60,seconds%60);}
 public boolean liveChannel(){CastSession session=actions.castSession();if(session!=null&&session.getRemoteMediaClient()!=null)return session.getRemoteMediaClient().getMediaInfo()==null?actions.liveChannel():session.getRemoteMediaClient().isLiveStream();Player player=video.getPlayer();return actions.liveChannel()||player!=null&&player.isCurrentMediaItemLive();}
 public boolean liveNow(){CastSession session=actions.castSession();if(session!=null&&session.getRemoteMediaClient()!=null)return session.getRemoteMediaClient().isLiveStream()&&session.getRemoteMediaClient().isPlaying()&&!needsLive();Player player=video.getPlayer();return liveChannel()&&player!=null&&player.isPlaying()&&!needsLive();}
 public void resetLive(){liveWasPaused=false;liveBaseline=androidx.media3.common.C.TIME_UNSET;}
 public boolean needsLive(){
  CastSession session=actions.castSession();if(session!=null)return LivePlayback.behind(session.getRemoteMediaClient());
  Player player=video.getPlayer();boolean channel=actions.liveChannel()||player!=null&&player.isCurrentMediaItemLive();if(!channel){liveWasPaused=false;return false;}
  if(player!=null&&!player.getPlayWhenReady()&&player.getPlaybackState()==Player.STATE_READY)liveWasPaused=true;
  long offset=player==null?androidx.media3.common.C.TIME_UNSET:player.getCurrentLiveOffset();long target=androidx.media3.common.C.TIME_UNSET;
  if(player!=null&&!player.getCurrentTimeline().isEmpty()){androidx.media3.common.Timeline.Window window=player.getCurrentTimeline().getWindow(player.getCurrentMediaItemIndex(),new androidx.media3.common.Timeline.Window());if(window.liveConfiguration!=null)target=window.liveConfiguration.targetOffsetMs;}
  if(target!=androidx.media3.common.C.TIME_UNSET)liveBaseline=target;else if(liveBaseline==androidx.media3.common.C.TIME_UNSET&&player!=null&&player.isPlaying()&&offset!=androidx.media3.common.C.TIME_UNSET)liveBaseline=offset;
  return liveWasPaused||offset!=androidx.media3.common.C.TIME_UNSET&&liveBaseline!=androidx.media3.common.C.TIME_UNSET&&offset>liveBaseline+5000;
 }
 private void refreshRecording(){boolean active=actions.recording();record.setVisibility(actions.recordingAvailable()?VISIBLE:GONE);record.setImageResource(active?R.drawable.ic_record_stop:R.drawable.ic_record);record.setContentDescription(getContext().getString(active?R.string.stop_recording:R.string.record_tv));play.setEnabled(!active);stop.setEnabled(!active);backup.setEnabled(!active);for(int index=0;index<outputs.getChildCount();index++)outputs.getChildAt(index).setEnabled(!active);}
 private void refreshProgress() {
  refreshRecording();
  boolean isLive=liveChannel();progress.setVisibility(isLive?GONE:VISIBLE);if(isLive){time.setText("");return;}
  boolean enabled=seekable();timeline.setEnabled(enabled);
  if(!tracking) timeline.setProgress(enabled?(int)(position()*1000/Math.max(1,duration())):0);
  time.setText(enabled?getContext().getString(R.string.playback_time,clock(position()),clock(duration())):getContext().getString(R.string.live));
 }
 public void refresh() {
  backup.setVisibility(actions.backupAvailable()?VISIBLE:GONE);
  record.setVisibility(actions.recordingAvailable()?VISIBLE:GONE);record.setImageResource(actions.recording()?R.drawable.ic_record_stop:R.drawable.ic_record);record.setContentDescription(getContext().getString(actions.recording()?R.string.stop_recording:R.string.record_tv));
  back.setVisibility(controlsVisible()&&fullscreenMode&&actions.fullscreen()&&!actions.pictureInPicture()?VISIBLE:GONE);
  boolean playing=actions.playing(),live=liveChannel();stop.setVisibility(live?GONE:VISIBLE);play.setImageResource(playing?(live?R.drawable.ic_stop:R.drawable.ic_pause):R.drawable.ic_play);play.setContentDescription(getContext().getString(playing?(live?R.string.stop_action:R.string.pause_action):R.string.play_action));
  fullscreen.setEnabled(actions.castSession()==null);fullscreen.setImageResource(actions.fullscreen()?R.drawable.ic_fullscreen_exit:R.drawable.ic_fullscreen);fullscreen.setContentDescription(getContext().getString(actions.fullscreen()?R.string.exit_fullscreen:R.string.fullscreen_action));
  try {
   CastSession session=actions.castSession();int level;
   if(session!=null) {level=session.isMute()?0:(int)Math.round(session.getVolume()*100);mute.setEnabled(true);}
   else {level=audio.getStreamVolume(AudioManager.STREAM_MUSIC);mute.setEnabled(!audio.isVolumeFixed());}
   mute.setImageResource(level==0?R.drawable.ic_mute:R.drawable.ic_volume);mute.setContentDescription(getContext().getString(level==0?R.string.unmute_action:R.string.mute_action));
  } catch(IllegalStateException e) {actions.volumeFailed();}
  settings.setEnabled(video.getPlayer()!=null && actions.castSession()==null);refreshProgress();
 }
 public void toggleMute() {
  try {
   CastSession session=actions.castSession();if(session!=null) session.setMute(!session.isMute());
   else {int current=audio.getStreamVolume(AudioManager.STREAM_MUSIC);if(current>0) {restoreVolume=current;audio.setStreamVolume(AudioManager.STREAM_MUSIC,0,0);} else audio.setStreamVolume(AudioManager.STREAM_MUSIC,Math.min(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),Math.max(1,restoreVolume)),0);}refresh();
  } catch(java.io.IOException|IllegalStateException|SecurityException e) {actions.volumeFailed();}
 }
}
