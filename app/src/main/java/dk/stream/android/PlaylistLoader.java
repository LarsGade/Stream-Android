package dk.stream.android;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** Loads compressed playlists and follows HTTP/HTTPS redirects without logging credentials. */
public final class PlaylistLoader {
 public static final int MAX_BYTES=64*1024*1024;
 private final OkHttpClient client=new OkHttpClient.Builder()
  .connectTimeout(20,TimeUnit.SECONDS).readTimeout(45,TimeUnit.SECONDS)
  .callTimeout(90,TimeUnit.SECONDS).build();
 public static final class Failure extends IOException {
  public final int status;
  Failure(int status) { super("Playlist request failed"); this.status=status; }
 }
 public List<Playlist.Channel> load(String url) throws IOException {
  Request request=new Request.Builder().url(Playlist.httpUrl(url))
   .header("User-Agent","Stream-Android/1.1")
   .header("Accept","application/x-mpegURL, audio/x-mpegurl, text/plain, */*").build();
  try(Response response=client.newCall(request).execute()) {
   if(!response.isSuccessful()) throw new Failure(response.code());
   if(response.body()==null) throw new Failure(0);
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();
   try(InputStream in=response.body().byteStream()) {
    byte[] buffer=new byte[8192]; int count;
    while((count=in.read(buffer))!=-1) {
     if(bytes.size()+count>MAX_BYTES) throw new Failure(-1);
     bytes.write(buffer,0,count);
    }
   }
   byte[] data=bytes.toByteArray();
   Charset charset=response.body().contentType()==null?StandardCharsets.UTF_8:response.body().contentType().charset(StandardCharsets.UTF_8);
   if(data.length>=2 && ((data[0]==(byte)0xFF && data[1]==(byte)0xFE)||(data[0]==(byte)0xFE && data[1]==(byte)0xFF))) charset=StandardCharsets.UTF_16;
   String text=new String(data,charset);
   String clean=text.replace("\uFEFF", "").trim();
   if(clean.startsWith("<") || clean.startsWith("{") || clean.startsWith("[")) throw new Failure(0);
   List<Playlist.Channel> channels=Playlist.parse(text,response.request().url().toString());
   if(channels.isEmpty()) throw new Failure(0);
   return channels;
  }
 }
}
