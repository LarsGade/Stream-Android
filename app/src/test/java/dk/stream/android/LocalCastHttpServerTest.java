package dk.stream.android;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.junit.*;
import static org.junit.Assert.*;

public class LocalCastHttpServerTest {
 private LocalCastHttpServer server;private URL url;private int opens;
 @Before public void setup() throws Exception {server=new LocalCastHttpServer(InetAddress.getLoopbackAddress());url=new URL(server.add(()->{opens++;return new ByteArrayInputStream("0123456789".getBytes(StandardCharsets.US_ASCII));},10,"audio/mpeg"));}
 @After public void close(){server.close();}
 private String request(String method,String path,String extra) throws Exception {try(Socket socket=new Socket(url.getHost(),url.getPort())){socket.setSoTimeout(3000);socket.getOutputStream().write((method+" "+path+" HTTP/1.1\r\nHost: localhost\r\n"+extra+"\r\n").getBytes(StandardCharsets.US_ASCII));ByteArrayOutputStream result=new ByteArrayOutputStream();byte[] buffer=new byte[1024];int count;while((count=socket.getInputStream().read(buffer))!=-1)result.write(buffer,0,count);return result.toString("US-ASCII");}}
 @Test public void servesOnlyExplicitlyRegisteredUrls() throws Exception {String result=request("GET",url.getPath(),"");assertTrue(result.startsWith("HTTP/1.1 200"));assertTrue(result.endsWith("0123456789"));assertTrue(result.contains("Content-Length: 10"));assertTrue(result.contains("Access-Control-Allow-Origin: *"));}
 @Test public void unknownTokenDoesNotOpenAnyFile() throws Exception {assertTrue(request("GET","/wrong/media","").startsWith("HTTP/1.1 404"));assertEquals(0,opens);}
 @Test public void byteRangeSupportsReceiverSeeking() throws Exception {String result=request("GET",url.getPath(),"Range: bytes=3-6\r\n");assertTrue(result.startsWith("HTTP/1.1 206"));assertTrue(result.contains("Content-Range: bytes 3-6/10"));assertTrue(result.endsWith("3456"));}
 @Test public void suffixAndOpenEndedRangesWork() throws Exception {assertTrue(request("GET",url.getPath(),"Range: bytes=-3\r\n").endsWith("789"));assertTrue(request("GET",url.getPath(),"Range: bytes=8-\r\n").endsWith("89"));}
 @Test public void rejectsInvalidAndMultipleRanges() throws Exception {for(String range:new String[]{"bytes=10-","bytes=5-2","bytes=0-1,4-5","bytes=-0","garbage"})assertTrue(request("GET",url.getPath(),"Range: "+range+"\r\n").startsWith("HTTP/1.1 416"));assertEquals(0,opens);}
 @Test public void headAndCorsPreflightDoNotSendFileContents() throws Exception {String head=request("HEAD",url.getPath(),"");assertTrue(head.contains("Content-Length: 10"));assertTrue(head.endsWith("\r\n\r\n"));assertTrue(request("OPTIONS",url.getPath(),"").startsWith("HTTP/1.1 204"));}
 @Test public void stopsServingWhenClosed() throws Exception {server.close();try{request("GET",url.getPath(),"");fail();}catch(IOException expected){}}
}
