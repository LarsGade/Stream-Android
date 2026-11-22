package dk.stream.android;

import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.ListView;

final class MediaListStyle {
 static int iconGap(View view){return Math.round(12*view.getResources().getDisplayMetrics().density);}
 static void apply(ListView list){list.setDivider(new ColorDrawable(0xFF303030));list.setDividerHeight(Math.round(6*list.getResources().getDisplayMetrics().density));}
 static void row(View view){view.setBackgroundColor(0xFF1D1D1D);}
 private MediaListStyle(){}
}
