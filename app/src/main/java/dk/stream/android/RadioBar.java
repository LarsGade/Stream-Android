package dk.stream.android;

import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

/** One transport icon; the rest of the bar opens the full player for every source. */
@androidx.media3.common.util.UnstableApi
public final class RadioBar extends LinearLayout {
 public interface VideoActions {String title();boolean playing();void toggle();void open();default boolean needsLive(){return false;}default void goLive(){}default boolean liveChannel(){return false;}default boolean liveNow(){return false;}}
 private final TextView name;
 private final ImageView artwork;private Object artworkModel;
 private final ImageButton toggle,live;
 private final Runnable beforePlay;
 private final VideoActions video;
 private final Runnable tick=new Runnable(){public void run(){refresh();postDelayed(this,500);}};
 public RadioBar(Context context,Runnable beforePlay){this(context,beforePlay,null);}
 public RadioBar(Context context,Runnable beforePlay,VideoActions video){
  super(context);this.beforePlay=beforePlay;this.video=video;setOrientation(HORIZONTAL);setGravity(Gravity.CENTER_VERTICAL);setBackgroundColor(Color.BLACK);setWillNotDraw(false);int border=Math.max(1,Math.round(getResources().getDisplayMetrics().density));setPadding(0,border,0,0);setTag("radio_bar");
  int size=Math.round(48*getResources().getDisplayMetrics().density);
  toggle=new ImageButton(context);toggle.setBackgroundColor(Color.TRANSPARENT);sizeButtonIcon(toggle,43.2f);toggle.setOnClickListener(view->transport());
  artwork=new ImageView(context);artwork.setScaleType(ImageView.ScaleType.FIT_CENTER);int artSize=Math.round(size*1.25f);artwork.setPadding(artSize/12,artSize/12,artSize/12,artSize/12);artwork.setVisibility(GONE);LayoutParams artParams=new LayoutParams(artSize,artSize);artParams.setMarginEnd(size/6);addView(artwork,artParams);
  name=new TextView(context);name.setTextColor(Color.WHITE);name.setTextSize(16);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);name.setGravity(Gravity.CENTER_VERTICAL);addView(name,new LayoutParams(0,-2,1));
  live=new ImageButton(context);live.setImageResource(R.drawable.ic_live);live.setBackgroundColor(Color.TRANSPARENT);sizeButtonIcon(live,28.8f);live.setContentDescription("Gå til live");addView(live,new LayoutParams(size,size));live.setOnClickListener(view->{beforePlay.run();if(CastPlayback.hasMedia(getContext()))LivePlayback.goLive(CastPlayback.remote(getContext()));else if(video!=null&&!video.title().isEmpty()&&LocalAudioService.current()==null)video.goLive();else getContext().startService(RadioPlaybackService.internalIntent(getContext(),RadioPlaybackService.LIVE));refresh();});
  addView(toggle,new LayoutParams(size,size));
  setOnClickListener(view->open());refresh();
 }
 private void sizeButtonIcon(ImageButton button,float iconDp){
  int padding=Math.round((48-iconDp)/2*getResources().getDisplayMetrics().density);
  button.setPadding(padding,padding,padding,padding);button.setScaleType(ImageView.ScaleType.FIT_CENTER);
 }
 void refresh(){
  boolean running=false;String title="";boolean isLive=false,liveChannel=false,liveNow=false;Object image=null;
  if(CastPlayback.hasMedia(getContext())){title=CastPlayback.title(getContext());isLive=LivePlayback.behind(CastPlayback.remote(getContext()));liveChannel=CastPlayback.remote(getContext()).isLiveStream();liveNow=liveChannel&&CastPlayback.remote(getContext()).isPlaying()&&!isLive;image=CastPlayback.phoneArtwork(getContext());if(image==null){com.google.android.gms.cast.MediaInfo info=CastPlayback.remote(getContext()).getMediaInfo();com.google.android.gms.cast.MediaMetadata metadata=info==null?null:info.getMetadata();if(metadata!=null&&!metadata.getImages().isEmpty())image=metadata.getImages().get(0).getUrl();}running=CastPlayback.remote(getContext()).isPlaying()||CastPlayback.remote(getContext()).isBuffering();}
  else if(LocalAudioService.current()!=null){LocalMedia file=LocalAudioService.current();title=file.displayName();running=LocalAudioService.playing(file.uri);androidx.media3.common.Player player=LocalAudioService.player();if(player!=null){androidx.media3.common.MediaMetadata metadata=player.getMediaMetadata();image=metadata.artworkData!=null&&metadata.artworkData.length>0?metadata.artworkData:metadata.artworkUri;}}
  else if(video!=null&&!video.title().isEmpty()){title=video.title();running=video.playing();isLive=video.needsLive();liveChannel=video.liveChannel();liveNow=video.liveNow();}
  else {RadioStation station=RadioPlaybackService.selectedStation();if(station!=null){title=station.displayName();running=RadioPlaybackService.playing(station.id);isLive=RadioPlaybackService.needsLive();liveChannel=true;liveNow=RadioPlaybackService.audioPlayer()!=null&&RadioPlaybackService.audioPlayer().isPlaying()&&!isLive;image=PlaybackArtwork.radioModel(getContext(),station);}}
  String localCover=LocalFileArtwork.asset(LocalAudioService.current());if(localCover!=null&&!CastPlayback.hasMedia(getContext()))image=localCover;
  updateArtwork(title.isEmpty()?null:image,title);
  setVisibility(title.isEmpty()?GONE:VISIBLE);name.setText(title);toggle.setImageResource(CastPlayback.image(getContext())?R.drawable.ic_image:running?R.drawable.ic_pause:R.drawable.ic_play);toggle.setContentDescription(CastPlayback.image(getContext())?"Vis billede":running?"Pause":"Afspil");toggle.setEnabled(!title.isEmpty());live.setVisibility(liveChannel?VISIBLE:GONE);live.setImageTintList(android.content.res.ColorStateList.valueOf(liveNow?0xFFFF3B30:Color.WHITE));live.setContentDescription(liveNow?"Live":"Gå til live");live.setSelected(liveNow);
 }
 @Override protected void onMeasure(int widthSpec,int heightSpec){super.onMeasure(widthSpec,View.MeasureSpec.makeMeasureSpec(MediaRowHeight.pixels(name),View.MeasureSpec.EXACTLY));}
 @Override protected void onDraw(android.graphics.Canvas canvas){super.onDraw(canvas);android.graphics.Paint paint=new android.graphics.Paint();paint.setColor(Color.WHITE);canvas.drawRect(0,0,getWidth(),getPaddingTop(),paint);}
 private void updateArtwork(Object model,String title){
  artwork.setContentDescription("Billede til "+title);if(java.util.Objects.equals(model,artworkModel))return;artworkModel=model;
  com.bumptech.glide.Glide.with(getContext().getApplicationContext()).clear(artwork);artwork.setImageDrawable(null);artwork.setVisibility(model==null?GONE:VISIBLE);if(model==null)return;
  com.bumptech.glide.Glide.with(getContext().getApplicationContext()).load(model).override(160,160).centerCrop().diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE).skipMemoryCache(true).listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>(){
   public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException failure,Object requested,com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,boolean first){artwork.setVisibility(GONE);return false;}
   public boolean onResourceReady(android.graphics.drawable.Drawable image,Object requested,com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,com.bumptech.glide.load.DataSource source,boolean first){artwork.setVisibility(VISIBLE);return false;}
  }).into(artwork);
 }
 private void transport(){
  if(CastPlayback.image(getContext())){open();return;}
  if(CastPlayback.hasMedia(getContext())){CastPlayback.toggle(getContext());return;}
  if(LocalAudioService.current()!=null){getContext().startService(LocalAudioService.command(getContext(),LocalAudioService.TOGGLE));return;}
  if(video!=null&&!video.title().isEmpty()){video.toggle();return;}
  RadioStation station=RadioPlaybackService.selectedStation();if(station==null)return;
  getContext().startService(RadioPlaybackService.internalIntent(getContext(),RadioPlaybackService.TRANSPORT_TOGGLE));post(this::refresh);
 }
 private void open(){
  if(CastPlayback.image(getContext())){getContext().startActivity(ImageViewerActivity.reopenIntent(getContext()));return;}
  if(CastPlayback.hasMedia(getContext())){getContext().startActivity(new Intent(getContext(),AudioPlayerActivity.class).putExtra("cast",true));return;}
  LocalMedia file=LocalAudioService.current();
  if(file!=null&&!file.audio()||file==null&&video!=null&&!video.title().isEmpty()){if(video!=null)video.open();return;}
  getContext().startActivity(new Intent(getContext(),AudioPlayerActivity.class).putExtra("local",file!=null));
 }
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();removeCallbacks(tick);post(tick);}
 @Override protected void onDetachedFromWindow(){removeCallbacks(tick);com.bumptech.glide.Glide.with(getContext().getApplicationContext()).clear(artwork);artwork.setImageDrawable(null);artwork.setVisibility(GONE);artworkModel=null;super.onDetachedFromWindow();}
}
