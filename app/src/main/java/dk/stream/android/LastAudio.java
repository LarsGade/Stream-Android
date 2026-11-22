package dk.stream.android;
import android.content.Context;
final class LastAudio {
 static final String STORE="last_audio",KEY="selection";
 static LastAudioSelection read(Context context){try{String json=context.getSharedPreferences(STORE,0).getString(KEY,null);return json==null?null:LastAudioSelection.read(json);}catch(Exception invalid){return null;}}
 static void radio(Context context,RadioStation station){try{save(context,LastAudioSelection.radio(station));}catch(Exception ignored){}}
 static void file(Context context,LocalMedia file){try{save(context,LastAudioSelection.file(file));}catch(Exception ignored){}}
 private static void save(Context context,LastAudioSelection selection)throws Exception{android.content.SharedPreferences prefs=context.getSharedPreferences(STORE,0);String data=selection.json();if(!data.equals(prefs.getString(KEY,null)))prefs.edit().putString(KEY,data).apply();}
 private LastAudio(){}
}
