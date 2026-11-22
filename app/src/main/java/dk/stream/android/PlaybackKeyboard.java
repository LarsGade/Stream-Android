package dk.stream.android;
import android.app.Activity;
import android.content.*;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
/** Close search input when a user starts playback, including cast playback. */
final class PlaybackKeyboard {
 static void hide(Context context){while(context instanceof ContextWrapper&&!(context instanceof Activity))context=((ContextWrapper)context).getBaseContext();if(!(context instanceof Activity))return;Activity activity=(Activity)context;View focus=activity.getCurrentFocus();View target=focus!=null?focus:activity.getWindow().getDecorView();androidx.core.view.WindowInsetsControllerCompat controller=androidx.core.view.ViewCompat.getWindowInsetsController(target);if(controller!=null)controller.hide(androidx.core.view.WindowInsetsCompat.Type.ime());InputMethodManager input=(InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);if(input!=null)input.hideSoftInputFromWindow(target.getWindowToken(),0);if(focus!=null)focus.clearFocus();}
 private PlaybackKeyboard(){}
}
