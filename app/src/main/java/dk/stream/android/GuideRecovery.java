package dk.stream.android;
import java.util.*;
/** Missing programme rows share one background retry per guide, with a bounded retry interval. */
final class GuideRecovery {
 static final long RETRY_MS=5*60*1000L;
 private final Map<String,Long> attempts=new HashMap<>();
 boolean request(String id,long now,boolean pending){if(pending)return false;Long last=attempts.get(id);if(last!=null&&now>=last&&now-last<RETRY_MS)return false;attempts.put(id,now);return true;}
}
