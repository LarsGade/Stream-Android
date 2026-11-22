package dk.stream.android;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Testable journal shared by file, streaming and Cast playback. */
final class PlaybackJournal {
 interface Store {String get(String key);void put(String key,String value);void clear();}
 private final Store store;private final Map<String,PlaybackMark> memory=new HashMap<>();private final Map<String,Long> written=new HashMap<>();
 PlaybackJournal(Store store){this.store=store;}
 private static String hash(String key){try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));StringBuilder result=new StringBuilder();for(byte b:bytes)result.append(String.format(Locale.ROOT,"%02x",b&255));return result.toString();}catch(Exception failure){throw new IllegalStateException(failure);}}
 PlaybackMark get(String key){if(key==null||key.isEmpty())return new PlaybackMark(0,0,false);PlaybackMark mark=memory.get(key);if(mark!=null)return mark;try{String[] fields=store.get(hash(key)).split(":");mark=new PlaybackMark(Long.parseLong(fields[0]),Long.parseLong(fields[1]),Boolean.parseBoolean(fields[2]));}catch(Exception missing){mark=new PlaybackMark(0,0,false);}memory.put(key,mark);return mark;}
 void record(String key,long position,long duration,boolean complete,boolean force,long now){if(key==null||key.isEmpty()||duration<=0||position<0)return;PlaybackMark mark=new PlaybackMark(position,duration,complete);memory.put(key,mark);Long last=written.get(key);if(force||complete||last==null||now-last>=5000){store.put(hash(key),mark.position+":"+mark.duration+":"+mark.complete);written.put(key,now);}}
 void flush(){for(Map.Entry<String,PlaybackMark> entry:memory.entrySet()){PlaybackMark mark=entry.getValue();if(mark.duration>0)store.put(hash(entry.getKey()),mark.position+":"+mark.duration+":"+mark.complete);}}
 void clear(){memory.clear();written.clear();store.clear();}
}
