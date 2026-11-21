package dk.stream.android;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Serves only explicitly queued documents under unguessable, per-session URLs. */
public final class LocalCastHttpServer implements AutoCloseable {
 public interface Opener {InputStream open() throws IOException;}
 public interface RangeOpener {InputStream open(long position) throws IOException;}
 public interface Resolver {Media resolve() throws IOException;}
 public static final class Media {final RangeOpener opener;final long size;final String mime;public Media(RangeOpener opener,long size,String mime){this.opener=opener;this.size=size;this.mime=mime;}}

 private static final class Resource {final RangeOpener opener;final boolean seekable;final long size;final String mime;Resource(RangeOpener opener,long size,String mime,boolean seekable){this.opener=opener;this.size=size;this.mime=mime;this.seekable=seekable;}}
 private final Map<String,Resource> resources=new ConcurrentHashMap<>();
 private final Map<String,Resolver> deferred=new ConcurrentHashMap<>();
 private final Set<Socket> clients=Collections.newSetFromMap(new ConcurrentHashMap<Socket,Boolean>());
 private final ServerSocket server;
 private final String origin;
 private volatile boolean closed;
 private final ExecutorService worker=new ThreadPoolExecutor(2,4,30,TimeUnit.SECONDS,new SynchronousQueue<>(),task->{Thread thread=new Thread(task,"local-cast-file");thread.setDaemon(true);return thread;});
 public LocalCastHttpServer(InetAddress address) throws IOException {
  if(address.isAnyLocalAddress()) throw new IOException("Specific network address required");
  server=new ServerSocket(0,16,address);String host=address.getHostAddress();origin="http://"+(host.contains(":")?"["+host+"]":host)+":"+server.getLocalPort();
  Thread accept=new Thread(()->{while(!closed) try {Socket socket=server.accept();socket.setSoTimeout(15000);clients.add(socket);try {worker.execute(()->serve(socket));}catch(RejectedExecutionException full){clients.remove(socket);socket.close();}}catch(IOException error){if(!closed) close();}},"local-cast-listener");accept.setDaemon(true);accept.start();
 }
 public String add(Opener opener,long size,String mime) {String path="/"+UUID.randomUUID()+"/media";resources.put(path,new Resource(position->opener.open(),size,mime.replaceAll("[\\r\\n]",""),false));return origin+path;}
 public String addRange(RangeOpener opener,long size,String mime) {String path="/"+UUID.randomUUID()+"/media";resources.put(path,new Resource(opener,size,mime.replaceAll("[\\r\\n]",""),true));return origin+path;}
 public String addDeferred(Resolver resolver){String path="/"+UUID.randomUUID()+"/media";deferred.put(path,resolver);return origin+path;}
 private static String line(InputStream input) throws IOException {ByteArrayOutputStream bytes=new ByteArrayOutputStream();for(int count=0;count<8192;count++){int value=input.read();if(value<0) return bytes.size()==0?null:bytes.toString("US-ASCII");if(value=='\n') return bytes.toString("US-ASCII").replace("\r","");bytes.write(value);}throw new IOException("Header too long");}
 private static void headers(OutputStream output,int status,String extra) throws IOException {
  String reason=status==200?"OK":status==206?"Partial Content":status==204?"No Content":status==416?"Range Not Satisfiable":status==405?"Method Not Allowed":status==502?"Bad Gateway":"Not Found";
  output.write(("HTTP/1.1 "+status+" "+reason+"\r\nConnection: close\r\nAccess-Control-Allow-Origin: *\r\nAccess-Control-Allow-Headers: Range\r\nAccess-Control-Expose-Headers: Content-Length, Content-Range, Accept-Ranges\r\nAccess-Control-Allow-Methods: GET, HEAD, OPTIONS\r\n"+extra+"\r\n").getBytes(StandardCharsets.US_ASCII));
 }
 private void serve(Socket socket) {
  try(Socket connection=socket) {
   InputStream request=connection.getInputStream();OutputStream output=connection.getOutputStream();String first=line(request);if(first==null)return;
   String[] parts=first.split(" ");if(parts.length!=3)return;Resource file=resources.get(parts[1]);
   Resolver resolver=deferred.get(parts[1]);if(file==null&&resolver==null){headers(output,404,"Content-Length: 0\r\n");return;}
   String method=parts[0],range=null;int count=0;String header;while((header=line(request))!=null&&!header.isEmpty()){if(++count>64)throw new IOException("Too many headers");int colon=header.indexOf(':');if(colon>0&&header.substring(0,colon).equalsIgnoreCase("Range"))range=header.substring(colon+1).trim();}
   if(method.equals("OPTIONS")){headers(output,204,"Content-Length: 0\r\n");return;}
   if(!method.equals("GET")&&!method.equals("HEAD")){headers(output,405,"Content-Length: 0\r\n");return;}
   if(resolver!=null)try{Media media=resolver.resolve();file=new Resource(media.opener,media.size,media.mime.replaceAll("[\\r\\n]",""),true);}catch(IOException|RuntimeException unavailable){headers(output,502,"Content-Length: 0\r\n");return;}
   long start=0,end=file.size-1;boolean partial=range!=null&&file.size>=0;
   if(partial)try {
    if(!range.startsWith("bytes=")||range.contains(","))throw new IllegalArgumentException();String[] bounds=range.substring(6).split("-",-1);if(bounds.length!=2)throw new IllegalArgumentException();
    if(bounds[0].isEmpty()){long suffix=Long.parseLong(bounds[1]);if(suffix<=0)throw new IllegalArgumentException();start=Math.max(0,file.size-suffix);}else {start=Long.parseLong(bounds[0]);if(!bounds[1].isEmpty())end=Math.min(end,Long.parseLong(bounds[1]));}
    if(start<0||start>=file.size||end<start)throw new IllegalArgumentException();
   }catch(IllegalArgumentException invalid){headers(output,416,"Content-Range: bytes */"+file.size+"\r\nContent-Length: 0\r\n");return;}
   if(method.equals("HEAD")){long length=file.size>=0?end-start+1:-1;headers(output,partial?206:200,"Content-Type: "+file.mime+"\r\n"+(file.size>=0?"Accept-Ranges: bytes\r\nContent-Length: "+length+"\r\n":"")+(partial?"Content-Range: bytes "+start+"-"+end+"/"+file.size+"\r\n":""));return;}
   try(InputStream media=file.opener.open(file.seekable?start:0)) {
    long skip=file.seekable?0:start;while(skip>0){long moved=media.skip(skip);if(moved>0)skip-=moved;else if(media.read()<0)throw new EOFException();else skip--;}
    long length=file.size>=0?end-start+1:-1;
    String fields="Content-Type: "+file.mime+"\r\nCache-Control: no-store\r\n"+(file.size>=0?"Accept-Ranges: bytes\r\nContent-Length: "+length+"\r\n":"")+(partial?"Content-Range: bytes "+start+"-"+end+"/"+file.size+"\r\n":"");
    headers(output,partial?206:200,fields);if(method.equals("HEAD"))return;
    byte[] buffer=new byte[65536];long remaining=length;while(!closed&&(remaining>0||remaining==-1)){int read=media.read(buffer,0,remaining==-1?buffer.length:(int)Math.min(buffer.length,remaining));if(read<0)break;output.write(buffer,0,read);if(remaining>=0)remaining-=read;}
   }
  }catch(IOException|RuntimeException error){/* Receiver disconnects and revoked document grants terminate this request. */}finally{clients.remove(socket);}
 }
 @Override public void close() {closed=true;try{server.close();}catch(IOException ignored){}for(Socket socket:clients)try{socket.close();}catch(IOException ignored){}clients.clear();resources.clear();deferred.clear();worker.shutdownNow();}
}
