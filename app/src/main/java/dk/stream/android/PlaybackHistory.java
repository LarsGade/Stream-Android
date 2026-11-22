package dk.stream.android;
import android.content.*;

final class PlaybackHistory {
 private static PlaybackJournal journal;private static boolean suspended;
 private static PlaybackJournal journal(Context context){if(journal==null){SharedPreferences prefs=context.getApplicationContext().getSharedPreferences("playback_history",0);journal=new PlaybackJournal(new PlaybackJournal.Store(){public String get(String key){return prefs.getString(key,"");}public void put(String key,String value){prefs.edit().putString(key,value).apply();}public void clear(){prefs.edit().clear().apply();}});}return journal;}
 static synchronized PlaybackMark get(Context context,String key){return journal(context).get(key);}
 static synchronized void record(Context context,String key,long position,long duration,boolean complete,boolean force){if(!suspended)journal(context).record(key,position,duration,complete,force,android.os.SystemClock.elapsedRealtime());}
 static synchronized void clear(Context context){journal(context).clear();}
 static synchronized void flush(Context context){journal(context).flush();}
 static synchronized void suspend(){suspended=true;}
 static synchronized void reload(){journal=null;suspended=false;}
 private PlaybackHistory(){}
}
