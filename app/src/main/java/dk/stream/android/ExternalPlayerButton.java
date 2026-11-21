package dk.stream.android;

import android.content.Context;
import android.graphics.Color;
import android.os.*;
import android.view.View;
import android.widget.ImageButton;

/** A shared output action, placed immediately before the Bluetooth button. */
@androidx.media3.common.util.UnstableApi
final class ExternalPlayerButton extends androidx.appcompat.widget.AppCompatImageButton {
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final Runnable tick=new Runnable(){public void run(){boolean available=getContext() instanceof MainActivity?((MainActivity)getContext()).externalPlaybackAvailable():ExternalPlaybackActivity.available(getContext());int visible=available?View.VISIBLE:View.GONE;boolean changed=getVisibility()!=visible;setVisibility(visible);setImageTintList(android.content.res.ColorStateList.valueOf(ExternalOutputState.blue(BluetoothOutputButton.streamPlaying())&&!(CastPlayback.hasMedia(getContext())&&CastPlayback.remote(getContext())!=null&&(CastPlayback.remote(getContext()).isPlaying()||CastPlayback.remote(getContext()).isBuffering()))?0xFF3686FF:Color.LTGRAY));if(changed&&getContext() instanceof MainActivity)((MainActivity)getContext()).refreshExternalOutputSpacing();handler.postDelayed(this,500);}};
 ExternalPlayerButton(Context context){super(context);setImageResource(R.drawable.ic_open_external);setImageTintList(android.content.res.ColorStateList.valueOf(Color.LTGRAY));setBackgroundColor(Color.TRANSPARENT);setContentDescription("Åbn i anden app");int padding=Math.round(12*getResources().getDisplayMetrics().density);setPadding(padding,padding,padding,padding);setOnClickListener(view->{if(context instanceof MainActivity)((MainActivity)context).openExternalPlayback();else ExternalPlaybackActivity.open(context);});setVisibility(GONE);}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();handler.post(tick);}
 @Override protected void onDetachedFromWindow(){handler.removeCallbacks(tick);super.onDetachedFromWindow();}
}
