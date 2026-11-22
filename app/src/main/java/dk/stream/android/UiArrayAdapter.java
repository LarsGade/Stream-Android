package dk.stream.android;
import android.content.Context;
import android.view.*;
import android.widget.*;
import java.util.*;
/** Translates option labels without altering the backing values used by callbacks. */
class UiArrayAdapter<T> extends ArrayAdapter<T> {
 UiArrayAdapter(Context context,int layout,T[] values){super(context,layout,values);}
 UiArrayAdapter(Context context,int layout,List<T> values){super(context,layout,values);}
 private View translated(View view,int position){if(view instanceof TextView&&getItem(position) instanceof String)((TextView)view).setText(UiText.text((String)getItem(position)));return view;}
 @Override public View getView(int position,View recycled,ViewGroup parent){return translated(super.getView(position,recycled,parent),position);}
 @Override public View getDropDownView(int position,View recycled,ViewGroup parent){return translated(super.getDropDownView(position,recycled,parent),position);}
}
