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
 private ImageView artwork;private Object artworkModel;
 private SeekBar progress;
 private ImageButton toggle,previous,next;
 private Library navigationLibrary=new Library();
 private ImageButton live;
 private androidx.mediarouter.app.MediaRouteButton castButton;
 private boolean local,tracking;
 private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
 private final Runnable tick=new Runnable(){public void run(){refresh();handler.postDelayed(this,500);}};
 private Player player(){return local?LocalAudioService.player():RadioPlaybackService.audioPlayer();}
 private RemoteMediaClient remote(){return CastPlayback.hasMedia(this)?CastPlayback.remote(this):null;}
 @Override public void onCreate(Bundle state){
  super.onCreate(state);try{navigationLibrary=new LibraryStore(this).load();}catch(Exception ignored){}CastPlayback.init(this);local=getIntent().getBooleanExtra("local",false);setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(24,24,24,24);root.setBackgroundColor(0xFF101010);setContentView(root);
  androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{androidx.core.graphics.Insets bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()|androidx.core.view.WindowInsetsCompat.Type.displayCutout());view.setPadding(bars.left+24,bars.top+24,bars.right+24,bars.bottom+24);return insets;});
  LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);root.addView(top,new LinearLayout.LayoutParams(-1,-2));ImageButton back=new ImageButton(this);back.setImageResource(R.drawable.ic_back);back.setContentDescription("Tilbage");back.setBackgroundColor(Color.TRANSPARENT);int backSize=Math.round(48*getResources().getDisplayMetrics().density);top.addView(back,new LinearLayout.LayoutParams(backSize,backSize));top.addView(new Space(this),new LinearLayout.LayoutParams(0,backSize,1));back.setOnClickListener(view->finish());
  LinearLayout outputs=new LinearLayout(this);outputs.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);outputs.addView(new BluetoothOutputButton(this),new LinearLayout.LayoutParams(Math.round(40*getResources().getDisplayMetrics().density),-1));
  androidx.mediarouter.app.MediaRouteButton cast=new CastOutputButton(this){@Override public boolean performClick(){if(local&&android.os.Build.VERSION.SDK_INT>=37&&checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK")!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{"android.permission.ACCESS_LOCAL_NETWORK"},7301);return true;}return super.performClick();}};castButton=cast;LinearLayout.LayoutParams castParams=new LinearLayout.LayoutParams(Math.round(40*getResources().getDisplayMetrics().density),-1);outputs.addView(cast,castParams);try{com.google.android.gms.cast.framework.CastButtonFactory.setUpMediaRouteButton(this,cast);}catch(RuntimeException ignored){}
  artwork=new ImageView(this);artwork.setScaleType(ImageView.ScaleType.FIT_CENTER);artwork.setAdjustViewBounds(true);artwork.setPadding(16,16,16,16);artwork.setVisibility(android.view.View.GONE);root.addView(artwork,new LinearLayout.LayoutParams(-1,0,2));
  title=new TextView(this);title.setTextColor(Color.WHITE);title.setTextSize(24);title.setGravity(Gravity.CENTER);root.addView(title,new LinearLayout.LayoutParams(-1,0,1));
  time=new TextView(this);time.setTextColor(Color.LTGRAY);root.addView(time);
  progress=new SeekBar(this);progress.setMax(1000);progress.setContentDescription(getString(R.string.seek_position));root.addView(progress,new LinearLayout.LayoutParams(-1,-2));
  progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
   public void onProgressChanged(SeekBar bar,int value,boolean user){if(!user)return;RemoteMediaClient remote=remote();if(remote!=null){long duration=remote.getStreamDuration();if(duration>0)remote.seek(new com.google.android.gms.cast.MediaSeekOptions.Builder().setPosition(duration*value/1000).build());}else {Player player=player();if(player!=null&&player.getDuration()>0&&player.isCurrentMediaItemSeekable())player.seekTo(player.getDuration()*value/1000);}}
   public void onStartTrackingTouch(SeekBar bar){tracking=true;}public void onStopTrackingTouch(SeekBar bar){tracking=false;}
  });
  FrameLayout controlBar=new FrameLayout(this);int controlHeight=Math.round(64*getResources().getDisplayMetrics().density);root.addView(controlBar,new LinearLayout.LayoutParams(-1,controlHeight));
  LinearLayout liveControls=new LinearLayout(this);controlBar.addView(liveControls,new FrameLayout.LayoutParams(-2,-1,Gravity.RIGHT|Gravity.CENTER_VERTICAL));
  LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);controlBar.addView(controls,new FrameLayout.LayoutParams(-2,-1,Gravity.CENTER));
  live=icon(liveControls,R.drawable.ic_live,"Gå til live");live.setVisibility(android.view.View.GONE);live.setOnClickListener(view->{RemoteMediaClient remote=remote();if(remote!=null)LivePlayback.goLive(remote);else if(!local)startService(RadioPlaybackService.internalIntent(this,RadioPlaybackService.LIVE));refresh();});
  previous=icon(controls,R.drawable.ic_previous,"Forrige");previous.setOnClickListener(view->move(-1));
  toggle=icon(controls,R.drawable.ic_play,"Afspil");toggle.setOnClickListener(view->{RemoteMediaClient remote=remote();if(remote!=null)CastPlayback.toggle(this);else {Player player=player();if(player!=null){if(player.getPlaybackState()==Player.STATE_ENDED){player.seekTo(0);player.play();}else if(player.getPlayWhenReady())player.pause();else {if(player.getPlaybackState()==Player.STATE_IDLE)player.prepare();player.play();}}}refresh();});
  ImageButton stop=icon(controls,R.drawable.ic_stop,"Stop");stop.setOnClickListener(view->{if(remote()!=null)CastPlayback.stop(this);else if(local)startService(LocalAudioService.command(this,LocalAudioService.STOP));else startService(RadioPlaybackService.internalIntent(this,RadioPlaybackService.STOP));finish();});next=icon(controls,R.drawable.ic_next,"Næste");next.setOnClickListener(view->move(1));
  controlBar.removeAllViews();LinearLayout transport=new LinearLayout(this);transport.setGravity(Gravity.CENTER_VERTICAL);while(outputs.getChildCount()>0){android.view.View child=outputs.getChildAt(0);outputs.removeViewAt(0);transport.addView(child,new LinearLayout.LayoutParams(0,-1,1));}while(controls.getChildCount()>0){android.view.View child=controls.getChildAt(0);controls.removeViewAt(0);transport.addView(child,new LinearLayout.LayoutParams(0,-1,1));}liveControls.removeView(live);transport.addView(live,new LinearLayout.LayoutParams(0,-1,1));controlBar.addView(transport,new FrameLayout.LayoutParams(-1,-1));refresh();
 }
 private ImageButton icon(LinearLayout parent,int resource,String description){ImageButton button=new ImageButton(this);button.setImageResource(resource);button.setContentDescription(description);button.setBackgroundColor(Color.TRANSPARENT);float density=getResources().getDisplayMetrics().density;int size=Math.round(64*density);int padding=Math.round((64-(resource==R.drawable.ic_live?28.8f:48))/2f*density);button.setPadding(padding,padding,padding,padding);button.setScaleType(ImageView.ScaleType.FIT_CENTER);parent.addView(button,new LinearLayout.LayoutParams(size,size));return button;}
 private RadioStation radioTarget(int direction){RemoteMediaClient remote=remote();RadioStation current=RadioPlaybackService.selectedStation();String id=current==null?"":current.id;if(remote!=null){id="";String name=CastPlayback.title(this);for(RadioStation station:RadioFavoriteNavigation.stations(navigationLibrary))if(station.displayName().equals(name)){id=station.id;break;}}return RadioFavoriteNavigation.adjacent(navigationLibrary,id,direction);}
 private boolean castRadio(RemoteMediaClient remote){return remote!=null&&remote.isLiveStream()&&remote.getMediaInfo()!=null&&remote.getMediaInfo().getMetadata()!=null&&remote.getMediaInfo().getMetadata().getMediaType()==com.google.android.gms.cast.MediaMetadata.MEDIA_TYPE_MUSIC_TRACK;}
 private boolean canMove(int direction){RemoteMediaClient remote=remote();if(remote!=null)return castRadio(remote)?radioTarget(direction)!=null:PlaybackNavigation.canMove(remote,direction);return local?PlaybackNavigation.canMove(player(),direction):radioTarget(direction)!=null;}
 private void move(int direction){if(!canMove(direction))return;RemoteMediaClient remote=remote();if(remote!=null){if(castRadio(remote))CastPlayback.playRadio(this,radioTarget(direction));else PlaybackNavigation.move(remote,direction);}else if(local)PlaybackNavigation.move(player(),direction);else startService(RadioPlaybackService.internalIntent(this,direction<0?RadioPlaybackService.PREVIOUS:RadioPlaybackService.NEXT));refresh();}
 private String format(long ms){long seconds=Math.max(0,ms/1000);return String.format(java.util.Locale.ROOT,"%d:%02d",seconds/60,seconds%60);}
 private void refresh(){
  RemoteMediaClient remote=remote();Player player=player();long duration,position;boolean seekable,running;
  if(remote!=null){title.setText(CastPlayback.title(this));duration=remote.getStreamDuration();position=remote.getApproximateStreamPosition();seekable=duration>0&&remote.getMediaInfo().getStreamType()!=com.google.android.gms.cast.MediaInfo.STREAM_TYPE_LIVE;running=remote.isPlaying()||remote.isBuffering();}
  else {if(player==null||player.getPlaybackState()==Player.STATE_IDLE&&(local||!RadioPlaybackService.recovering())){finish();return;}LocalMedia file=local?LocalAudioService.current():null;RadioStation station=local?null:RadioPlaybackService.selectedStation();CharSequence name=file!=null?file.displayName():station!=null?station.displayName():"";title.setText(name);duration=player.getDuration();position=player.getCurrentPosition();seekable=duration>0&&duration!=C.TIME_UNSET&&player.isCurrentMediaItemSeekable();running=player.getPlayWhenReady()&&player.getPlaybackState()!=Player.STATE_ENDED;}
  boolean liveChannel=remote!=null?remote.isLiveStream():!local&&RadioPlaybackService.selectedStation()!=null;
  boolean liveNow=liveChannel&&(remote!=null?remote.isPlaying()&&!LivePlayback.behind(remote):player!=null&&player.isPlaying()&&!RadioPlaybackService.needsLive());
  live.setVisibility(liveChannel?android.view.View.VISIBLE:android.view.View.GONE);live.setImageTintList(android.content.res.ColorStateList.valueOf(liveNow?0xFFFF3B30:Color.WHITE));live.setContentDescription(liveNow?"Live":"Gå til live");live.setSelected(liveNow);
  previous.setEnabled(canMove(-1));previous.setAlpha(previous.isEnabled()?1f:0.35f);next.setEnabled(canMove(1));next.setAlpha(next.isEnabled()?1f:0.35f);
  updateArtwork(remote,player);
  progress.setEnabled(seekable);if(!tracking)progress.setProgress(seekable?(int)Math.min(1000,position*1000/duration):0);time.setText(format(position)+(duration>0?" / "+format(duration):""));toggle.setImageResource(running?R.drawable.ic_pause:R.drawable.ic_play);toggle.setContentDescription(running?"Pause":"Afspil");
 }
 private void updateArtwork(RemoteMediaClient remote,Player player){
  Object model=null;
  if(remote!=null){
   com.google.android.gms.cast.MediaInfo info=remote.getMediaInfo();com.google.android.gms.cast.MediaMetadata metadata=info==null?null:info.getMetadata();
   model=CastPlayback.phoneArtwork(this);
   if(model==null&&metadata!=null&&!metadata.getImages().isEmpty())model=metadata.getImages().get(0).getUrl();
  }else if(local&&player!=null){MediaMetadata metadata=player.getMediaMetadata();if(metadata.artworkData!=null&&metadata.artworkData.length>0)model=metadata.artworkData;else if(metadata.artworkUri!=null)model=metadata.artworkUri;}
  else {RadioStation station=RadioPlaybackService.selectedStation();if(station!=null)model=PlaybackArtwork.radioModel(this,station);}
  String localCover=local?LocalFileArtwork.asset(LocalAudioService.current()):null;if(remote==null&&localCover!=null)model=localCover;
  artwork.setContentDescription("Billede til "+title.getText());
  if(java.util.Objects.equals(model,artworkModel))return;
  artworkModel=model;com.bumptech.glide.Glide.with(this).clear(artwork);artwork.setImageDrawable(null);artwork.setVisibility(model==null?android.view.View.GONE:android.view.View.VISIBLE);if(model==null)return;
  com.bumptech.glide.Glide.with(this).load(model).override(512,512).fitCenter().diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE).skipMemoryCache(true).listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>(){
   public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException failure,Object requested,com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,boolean first){artwork.setVisibility(android.view.View.GONE);return false;}
   public boolean onResourceReady(android.graphics.drawable.Drawable image,Object requested,com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,com.bumptech.glide.load.DataSource source,boolean first){artwork.setVisibility(android.view.View.VISIBLE);return false;}
  }).into(artwork);
 }
 @Override protected void onNewIntent(android.content.Intent intent){super.onNewIntent(intent);setIntent(intent);local=intent.getBooleanExtra("local",false);if(artwork!=null){com.bumptech.glide.Glide.with(this).clear(artwork);artwork.setImageDrawable(null);artwork.setVisibility(android.view.View.GONE);}artworkModel=null;refresh();}
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==7301&&results.length>0&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)castButton.performClick();}
 @Override protected void onStart(){super.onStart();artworkModel=null;handler.post(tick);}
 @Override protected void onStop(){handler.removeCallbacks(tick);if(artwork!=null){com.bumptech.glide.Glide.with(this).clear(artwork);artwork.setImageDrawable(null);artwork.setVisibility(android.view.View.GONE);}artworkModel=null;super.onStop();}
}
