package dk.stream.android;
import android.content.*;
/** The chooser confirms an actual app selection, not merely an open chooser. */
public final class ExternalPlayerChosenReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context,Intent intent){if(intent.getStringExtra("session")!=null&&intent.getStringExtra("session").equals(context.getSharedPreferences("external_player",0).getString("session",null))&&intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT)!=null)context.getSharedPreferences("external_player",0).edit().putBoolean("selected",true).apply();}
}
