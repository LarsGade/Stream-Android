package dk.stream.android;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;

/** Consistent Cast connection state and an explicit disconnect action. */
@androidx.media3.common.util.UnstableApi
class CastOutputButton extends androidx.mediarouter.app.MediaRouteButton {
 private final Runnable update=new Runnable(){public void run(){refreshState();postDelayed(this,500);}};
 CastOutputButton(Context context){super(context);setAlwaysVisible(true);try{com.google.android.gms.cast.framework.CastButtonFactory.setUpMediaRouteButton(context,this);}catch(RuntimeException ignored){}refreshState();}
 private void refreshState(){boolean active=CastPlayback.connected(getContext());Drawable icon=androidx.appcompat.content.res.AppCompatResources.getDrawable(getContext(),R.drawable.ic_cast_output);if(icon!=null){icon=icon.mutate();androidx.core.graphics.drawable.DrawableCompat.setTintList(icon,ColorStateList.valueOf(active?0xFF3686FF:0xFFFFFFFF));setRemoteIndicatorDrawable(icon);}setContentDescription(UiText.text(active?"Cast, tilsluttet. Stop cast":"Vælg Cast-enhed"));}
 @Override public boolean performClick(){if(CastPlayback.connected(getContext())){new androidx.appcompat.app.AlertDialog.Builder(getContext()).setTitle("Cast").setItems(UiText.labels(new CharSequence[]{OutputDeviceLabel.active(CastPlayback.session(getContext()).getCastDevice()!=null?CastPlayback.session(getContext()).getCastDevice().getFriendlyName():"Cast-enhed"),"Stop cast"}),(dialog,which)->{if(which!=1)return;CastPlayback.stopCasting(getContext());refreshState();}).setNegativeButton(UiText.text("Annuller"),null).show();return true;}return super.performClick();}
 @Override public void onAttachedToWindow(){super.onAttachedToWindow();removeCallbacks(update);post(update);}
 @Override public void onDetachedFromWindow(){removeCallbacks(update);super.onDetachedFromWindow();}
}
