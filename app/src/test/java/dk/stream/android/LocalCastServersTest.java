package dk.stream.android;
import java.io.*;
import java.net.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class LocalCastServersTest {
 private LocalCastHttpServer server()throws Exception{return new LocalCastHttpServer(InetAddress.getLoopbackAddress());}
 private String url(LocalCastHttpServer server){return server.add(()->new ByteArrayInputStream(new byte[]{42}),1,"audio/mpeg");}
 private boolean reachable(String address){try{HttpURLConnection connection=(HttpURLConnection)new URL(address).openConnection();connection.setConnectTimeout(1000);connection.setReadTimeout(1000);try(InputStream input=connection.getInputStream()){return input.read()==42;}finally{connection.disconnect();}}catch(IOException closed){return false;}}
 @Test public void fileSwitchKeepsOldEndpointUntilNewQueueIsAccepted()throws Exception{try(LocalCastServers servers=new LocalCastServers()){LocalCastHttpServer old=server(),next=server();servers.add(old);String previous=url(old);servers.add(next);String current=url(next);assertTrue(reachable(previous));assertTrue(reachable(current));assertTrue(servers.activate(next));assertFalse(reachable(previous));assertTrue(reachable(current));}}
 @Test public void oldLoadCallbackCannotCloseNewPlayback()throws Exception{try(LocalCastServers servers=new LocalCastServers()){LocalCastHttpServer old=server(),next=server();servers.add(old);servers.add(next);String current=url(next);servers.activate(next);assertFalse(servers.activate(old));assertTrue(reachable(current));}}
 @Test public void shutdownClosesActiveAndRejectsLatePreparation()throws Exception{LocalCastServers servers=new LocalCastServers();LocalCastHttpServer old=server();servers.add(old);String previous=url(old);servers.close();assertFalse(reachable(previous));LocalCastHttpServer late=server();String lateUrl=url(late);assertFalse(servers.add(late));assertFalse(reachable(lateUrl));}
}
