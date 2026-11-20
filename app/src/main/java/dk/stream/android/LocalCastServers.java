package dk.stream.android;
import java.util.*;
/** Keep the old media endpoint alive until the replacement queue is accepted. */
final class LocalCastServers implements AutoCloseable {
 private final Set<LocalCastHttpServer> owned=new HashSet<>();private boolean closed;
 synchronized boolean add(LocalCastHttpServer server){if(closed){server.close();return false;}owned.add(server);return true;}
 synchronized boolean activate(LocalCastHttpServer server){if(closed||!owned.contains(server))return false;Iterator<LocalCastHttpServer> entries=owned.iterator();while(entries.hasNext()){LocalCastHttpServer old=entries.next();if(old!=server){old.close();entries.remove();}}return true;}
 @Override public synchronized void close(){closed=true;for(LocalCastHttpServer server:owned)server.close();owned.clear();}
}
