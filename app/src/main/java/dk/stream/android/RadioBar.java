package dk.stream.android;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;

/** Persistent transport below the page content; Stop retains the station for a fresh live play. */
@androidx.media3.common.util.UnstableApi
public final class RadioBar extends LinearLayout {
 private final TextView name;
 private final Button toggle;
 private final Runnable beforePlay;
 private final Runnable tick=new Runnable() {public void run() {refresh();postDelayed(this,500);}};
 public RadioBar(Context context,Runnable beforePlay) {
  super(context);this.beforePlay=beforePlay;setOrientation(HORIZONTAL);setGravity(Gravity.CENTER_VERTICAL);setBackgroundColor(0xFF1D1D1D);
  int padding=Math.round(8*getResources().getDisplayMetrics().density);setPadding(padding,0,padding,0);setTag("radio_bar");
  name=new TextView(context);name.setTextColor(Color.WHITE);name.setTextSize(16);name.setMaxLines(2);addView(name,new LayoutParams(0,-2,1));
  toggle=new Button(context);addView(toggle,new LayoutParams(-2,-2));toggle.setOnClickListener(v->transport());refresh();
 }
 void refresh() {
  RadioStation station=RadioPlaybackService.selectedStation();name.setText(station==null?"Radio":station.displayName());
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
