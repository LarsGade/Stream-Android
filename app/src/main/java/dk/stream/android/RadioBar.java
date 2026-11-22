package dk.stream.android;

import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

/** One transport icon; the rest of the bar opens the full player for every source. */
@androidx.media3.common.util.UnstableApi
public final class RadioBar extends LinearLayout {
 private long progressPosition,progressDuration;
 public interface VideoActions {default FavoriteLists.Item downloadItem(){return null;}default long position(){return 0;}default long duration(){return 0;}default Object artwork(){return null;}String title();boolean playing();void toggle();void open();default boolean needsLive(){return false;}default void goLive(){}default boolean liveChannel(){return false;}default boolean liveNow(){return false;}}
 private final TextView name;
 private final ImageView artwork;private Object artworkModel;private int artworkFallback;
 private final ImageButton toggle;
 private final Runnable beforePlay;
 private final VideoActions video;
 private final Runnable tick=new Runnable(){public void run(){refresh();postDelayed(this,500);}};
 public RadioBar(Context context,Runnable beforePlay){this(context,beforePlay,null);}
 public RadioBar(Context context,Runnable beforePlay,VideoActions video){
  super(context);this.beforePlay=beforePlay;this.video=video;setOrientation(HORIZONTAL);setGravity(Gravity.CENTER_VERTICAL);setBackgroundColor(Color.BLACK);setWillNotDraw(false);int border=Math.max(1,Math.round(getResources().getDisplayMetrics().density));setPadding(0,border,0,0);setTag("radio_bar");
  int size=Math.round(48*getResources().getDisplayMetrics().density);
  toggle=new ImageButton(context);toggle.setBackgroundColor(Color.TRANSPARENT);sizeButtonIcon(toggle,43.2f);toggle.setOnClickListener(view->transport());
  artwork=new ImageView(context);artwork.setScaleType(ImageView.ScaleType.FIT_CENTER);int artSize=Math.round(size*1.25f);artwork.setPadding(artSize/12,artSize/12,artSize/12,artSize/12);artwork.setVisibility(GONE);LayoutParams artParams=new LayoutParams(artSize,artSize);artParams.setMarginEnd(MediaListStyle.iconGap(artwork));addView(artwork,artParams);
  name=new TextView(context);name.setTextColor(Color.WHITE);name.setTextSize(16);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);name.setGravity(Gravity.CENTER_VERTICAL);addView(name,new LayoutParams(0,-2,1));
  addView(toggle,new LayoutParams(size,size));
  setOnClickListener(view->open());setOnLongClickListener(view->{FavoriteLists.Item item=MediaDownloads.current(getContext());if(item==null&&video!=null)item=video.downloadItem();if(!MediaDownloads.eligible(item))return false;MediaDownloads.show(this,getContext(),item);return true;});refresh();
 }
 private void sizeButtonIcon(ImageButton button,float iconDp){
  int padding=Math.round((48-iconDp)/2*getResources().getDisplayMetrics().density);
  button.setPadding(padding,padding,padding,padding);button.setScaleType(ImageView.ScaleType.FIT_CENTER);
 }
 void refresh(){
  progressPosition=0;progressDuration=0;boolean running=false;String title="";boolean isLive=false,liveChannel=false,liveNow=false;Object image=null;
  if(CastPlayback.hasMedia(getContext())){title=CastPlayback.title(getContext());isLive=LivePlayback.behind(CastPlayback.remote(getContext()));liveChannel=CastPlayback.remote(getContext()).isLiveStream();liveNow=liveChannel&&CastPlayback.remote(getContext()).isPlaying()&&!isLive;image=CastPlayback.phoneArtwork(getContext());if(image==null&&video!=null&&title.equals(video.title()))image=video.artwork();if(image==null){com.google.android.gms.cast.MediaInfo info=CastPlayback.remote(getContext()).getMediaInfo();com.google.android.gms.cast.MediaMetadata metadata=info==null?null:info.getMetadata();if(metadata!=null&&!metadata.getImages().isEmpty())image=metadata.getImages().get(0).getUrl();}running=CastPlayback.remote(getContext()).isPlaying()||CastPlayback.remote(getContext()).isBuffering();if(!liveChannel&&!CastPlayback.image(getContext())){progressPosition=CastPlayback.remote(getContext()).getApproximateStreamPosition();progressDuration=CastPlayback.remote(getContext()).getStreamDuration();}}
  else if(LocalAudioService.current()!=null){LocalMedia file=LocalAudioService.current();title=file.displayName();running=LocalAudioService.playing(file.uri);androidx.media3.common.Player player=LocalAudioService.player();if(player!=null){androidx.media3.common.MediaMetadata metadata=player.getMediaMetadata();image=metadata.artworkData!=null&&metadata.artworkData.length>0?metadata.artworkData:metadata.artworkUri;if(!player.isCurrentMediaItemLive()){progressPosition=player.getCurrentPosition();progressDuration=player.getDuration();}}}
  else if(RadioPlaybackService.selectedFile()!=null){LocalMedia file=RadioPlaybackService.selectedFile();title=file.displayName();androidx.media3.common.Player player=RadioPlaybackService.audioPlayer();running=player!=null&&player.getPlayWhenReady()&&player.getPlaybackState()!=androidx.media3.common.Player.STATE_ENDED;if(player!=null){progressPosition=player.getCurrentPosition();progressDuration=player.getDuration();androidx.media3.common.MediaMetadata metadata=player.getMediaMetadata();image=metadata.artworkData!=null?metadata.artworkData:metadata.artworkUri;}String cover=LocalFileArtwork.asset(file);if(cover!=null)image=cover;}
  else if(video!=null&&!video.title().isEmpty()){title=video.title();running=video.playing();isLive=video.needsLive();liveChannel=video.liveChannel();liveNow=video.liveNow();image=video.artwork();if(!liveChannel){progressPosition=video.position();progressDuration=video.duration();}}
  else {RadioStation station=RadioPlaybackService.selectedStation();if(station!=null){title=station.displayName();running=RadioPlaybackService.playing(station.id);isLive=RadioPlaybackService.needsLive();liveChannel=true;liveNow=RadioPlaybackService.audioPlayer()!=null&&RadioPlaybackService.audioPlayer().isPlaying()&&!isLive;image=PlaybackArtwork.radioModel(getContext(),station);}}
  String localCover=LocalFileArtwork.asset(LocalAudioService.current());if(localCover!=null&&!CastPlayback.hasMedia(getContext()))image=localCover;
  int fallback=R.drawable.ic_music_note;if(CastPlayback.hasMedia(getContext()))fallback=CastPlayback.image(getContext())?R.drawable.ic_image:CastPlayback.audio(getContext())?R.drawable.ic_music_note:R.drawable.ic_video;else if(LocalAudioService.current()!=null)fallback=MediaFallback.icon(LocalAudioService.current());else if(RadioPlaybackService.selectedFile()!=null)fallback=MediaFallback.icon(RadioPlaybackService.selectedFile());else if(video!=null&&!video.title().isEmpty()&&RadioPlaybackService.selectedStation()==null)fallback=R.drawable.ic_video;
  updateArtwork(image,title,fallback);
  setVisibility(title.isEmpty()?GONE:VISIBLE);name.setText(title);toggle.setImageResource(CastPlayback.image(getContext())?R.drawable.ic_image:running?(liveChannel?R.drawable.ic_stop:R.drawable.ic_pause):R.drawable.ic_play);toggle.setContentDescription(UiText.text(CastPlayback.image(getContext())?"Vis billede":running?(liveChannel?"Stop":"Pause"):"Afspil"));toggle.setEnabled(!title.isEmpty());invalidate();
 }
 @Override protected void onMeasure(int widthSpec,int heightSpec){super.onMeasure(widthSpec,View.MeasureSpec.makeMeasureSpec(MediaRowHeight.pixels(name),View.MeasureSpec.EXACTLY));}
 @Override protected void onDraw(android.graphics.Canvas canvas){super.onDraw(canvas);android.graphics.Paint paint=new android.graphics.Paint();paint.setColor(Color.WHITE);canvas.drawRect(0,0,getWidth(),getPaddingTop(),paint);if(progressDuration>0){int height=Math.max(1,Math.round(2*getResources().getDisplayMetrics().density));paint.setColor(0xFF343434);canvas.drawRect(0,getHeight()-height,getWidth(),getHeight(),paint);paint.setColor(0xFF3686FF);canvas.drawRect(0,getHeight()-height,(float)(getWidth()*Math.max(0,Math.min(1,progressPosition/(double)progressDuration))),getHeight(),paint);}}
 private void updateArtwork(Object model,String title,int fallback){
  artwork.setContentDescription(UiText.text("Billede til "+title));boolean same=java.util.Objects.equals(model,artworkModel)&&artworkFallback==fallback;artworkModel=model;artworkFallback=fallback;
  if(title.isEmpty()){com.bumptech.glide.Glide.with(getContext().getApplicationContext()).clear(artwork);artwork.setVisibility(GONE);artworkModel=null;artworkFallback=0;return;}
  artwork.setVisibility(VISIBLE);
  if(same&&artwork.getDrawable()!=null)return;
  com.bumptech.glide.Glide.with(getContext().getApplicationContext()).clear(artwork);artwork.setImageResource(fallback);
  if(model==null||model instanceof Integer||model instanceof String&&((String)model).isEmpty())return;
  if(fallback==R.drawable.ic_image){PictureThumbnails.load(artwork,model.toString(),160);return;}
  com.bumptech.glide.Glide.with(getContext().getApplicationContext()).load(model).placeholder(fallback).error(fallback).override(160,160).fitCenter().diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE).skipMemoryCache(true).into(artwork);
 }
 private void transport(){
  if(CastPlayback.image(getContext())){open();return;}
  if(CastPlayback.hasMedia(getContext())&&video!=null&&!video.title().isEmpty()&&video.liveChannel()){video.toggle();return;}
  if(CastPlayback.hasMedia(getContext())){com.google.android.gms.cast.framework.media.RemoteMediaClient remote=CastPlayback.remote(getContext());if(remote.isLiveStream()&&remote.getMediaInfo()!=null&&remote.getMediaInfo().getMetadata()!=null&&remote.getMediaInfo().getMetadata().getMediaType()==com.google.android.gms.cast.MediaMetadata.MEDIA_TYPE_MUSIC_TRACK&&(remote.isPlaying()||remote.isBuffering()))CastPlayback.stop(getContext());else CastPlayback.toggle(getContext());return;}
  if(LocalAudioService.current()!=null){getContext().startService(LocalAudioService.command(getContext(),LocalAudioService.TOGGLE));return;}
  if(RadioPlaybackService.selectedFile()!=null){getContext().startService(RadioPlaybackService.internalIntent(getContext(),RadioPlaybackService.TRANSPORT_TOGGLE));return;}
  if(video!=null&&!video.title().isEmpty()){video.toggle();return;}
  RadioStation station=RadioPlaybackService.selectedStation();if(station==null)return;
  getContext().startService(RadioPlaybackService.internalIntent(getContext(),RadioPlaybackService.playing(station.id)?RadioPlaybackService.STOP:RadioPlaybackService.LIVE));post(this::refresh);
 }
 private void open(){
  if(CastPlayback.image(getContext())){getContext().startActivity(ImageViewerActivity.reopenIntent(getContext()));return;}
  if(CastPlayback.hasMedia(getContext())){getContext().startActivity(new Intent(getContext(),AudioPlayerActivity.class).putExtra("cast",true));return;}
  if(RadioPlaybackService.selectedFile()!=null){getContext().startActivity(new Intent(getContext(),AudioPlayerActivity.class));return;}
  LocalMedia file=LocalAudioService.current();
  if(file!=null&&!file.audio()||file==null&&video!=null&&!video.title().isEmpty()){if(video!=null)video.open();return;}
  getContext().startActivity(new Intent(getContext(),AudioPlayerActivity.class).putExtra("local",file!=null));
 }
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();removeCallbacks(tick);post(tick);}
 @Override protected void onDetachedFromWindow(){removeCallbacks(tick);com.bumptech.glide.Glide.with(getContext().getApplicationContext()).clear(artwork);artwork.setImageDrawable(null);artwork.setVisibility(GONE);artworkModel=null;super.onDetachedFromWindow();}
}
