package dk.stream.android;

import android.graphics.Paint;
import android.widget.TextView;

/** Shared height for two-line file rows and the bottom player. */
final class MediaRowHeight {
 static int pixels(TextView text) {
  float density=text.getResources().getDisplayMetrics().density;
  Paint.FontMetricsInt metrics=text.getPaint().getFontMetricsInt();
  int lines=text.getLineHeight()*2+metrics.bottom-metrics.descent+metrics.ascent-metrics.top;
  return Math.max(Math.round(64*density),lines+Math.round(8*density)*2);
 }
 private MediaRowHeight(){}
}
