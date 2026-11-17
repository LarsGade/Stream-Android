package dk.stream.android;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

final class OutputDeviceLabel {
 static CharSequence active(String name){SpannableString label=new SpannableString(name);label.setSpan(new ForegroundColorSpan(0xFF3686FF),0,label.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);return label;}
 private OutputDeviceLabel(){}
}
