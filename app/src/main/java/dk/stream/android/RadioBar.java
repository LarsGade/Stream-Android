package dk.stream.android;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import com.bumptech.glide.Glide;

/** Radio transport below the page content, hidden when playback is stopped. */
@androidx.media3.common.util.UnstableApi
public final class RadioBar extends LinearLayout {
 private final TextView name;
 private final ImageView logo;
 private final LocalChannelLogos logos;
 private String artworkKey="";
 private final Button toggle,live;
 private final Runnable beforePlay;
 private final Runnable tick=new Runnable() {public void run() {refresh();postDelayed(this,500);}};
 public RadioBar(Context context,Runnable beforePlay) {
  super(context);this.beforePlay=beforePlay;setOrientation(HORIZONTAL);setGravity(Gravity.CENTER_VERTICAL);setBackgroundColor(0xFF1D1D1D);
  int padding=Math.round(8*getResources().getDisplayMetrics().density);setPadding(padding,0,padding,0);setTag("radio_bar");
  logos=LocalChannelLogos.load(context);logo=new ImageView(context);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
  int size=Math.round(48*getResources().getDisplayMetrics().density);addView(logo,new LayoutParams(size,size));
  name=new TextView(context);name.setTextColor(Color.WHITE);name.setTextSize(16);name.setMaxLines(2);name.setPadding(padding,0,padding,0);addView(name,new LayoutParams(0,-2,1));
  toggle=new Button(context);addView(toggle,new LayoutParams(-2,-2));toggle.setOnClickListener(v->transport());
  live=new Button(context);live.setText(R.string.radio_live);addView(live,new LayoutParams(-2,-2));
  live.setOnClickListener(v->{beforePlay.run();getContext().startService(RadioPlaybackService.internalIntent(getContext(),RadioPlaybackService.LIVE));post(this::refresh);});refresh();
 }
 void refresh() {
  RadioStation station=RadioPlaybackService.selectedStation();setVisibility(station==null?GONE:VISIBLE);
  if(station!=null) {
   String local=logos.find(station.displayName()),source=local==null?station.logo:local;
   String key=station.id+"|"+source;
   if(!key.equals(artworkKey)) {artworkKey=key;Glide.with(getContext()).load(source).signature(com.bumptech.glide.signature.ApplicationVersionSignature.obtain(getContext())).placeholder(R.drawable.ic_channel).error(R.drawable.ic_channel).into(logo);}
   logo.setContentDescription(station.displayName());
  }
  name.setText(station==null?"Radio":station.displayName());
  live.setVisibility(RadioPlaybackService.needsLive()?VISIBLE:GONE);
  boolean running=station!=null && RadioPlaybackService.playing(station.id);
  toggle.setText(running?"Stop":"Afspil");toggle.setEnabled(station!=null);
  toggle.setContentDescription((running?"Stop ":"Afspil ")+(station==null?"radio":station.displayName()));
 }
 private void transport() {
  RadioStation station=RadioPlaybackService.selectedStation();if(station==null) return;
  if(RadioPlaybackService.playing(station.id)) getContext().startService(RadioPlaybackService.internalIntent(getContext(),RadioPlaybackService.STOP));
  else {beforePlay.run();androidx.core.content.ContextCompat.startForegroundService(getContext(),RadioPlaybackService.playIntent(getContext(),station));}
  post(this::refresh);
 }
 @Override protected void onAttachedToWindow() {super.onAttachedToWindow();removeCallbacks(tick);post(tick);}
 @Override protected void onDetachedFromWindow() {removeCallbacks(tick);super.onDetachedFromWindow();}
}
