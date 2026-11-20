package dk.stream.android;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

/** Resolve the receiver's media type from the response, including extensionless IPTV URLs. */
final class CastStream {
 final String url,type;
 CastStream(String url,String type){this.url=url;this.type=type;}
 static final OkHttpClient HTTP=new OkHttpClient.Builder().connectTimeout(5,TimeUnit.SECONDS).readTimeout(5,TimeUnit.SECONDS).callTimeout(8,TimeUnit.SECONDS).build();
 static CastStream resolve(String input,boolean live){return resolve(HTTP,input,live);}
 static CastStream resolve(OkHttpClient client,String input,boolean live){
  HttpUrl url=Playlist.httpUrl(input);String path=url.encodedPath().toLowerCase(Locale.ROOT);
  // Xtream MPEG-TS live links can often also be requested as HLS, which Cast supports.
  if(live&&path.contains("/live/")&&path.endsWith(".ts")){
   String last=url.pathSegments().get(url.pathSize()-1);String hls=url.newBuilder().setPathSegment(url.pathSize()-1,last.substring(0,last.length()-3)+".m3u8").build().toString();
   CastStream candidate=probe(client,hls);if(candidate!=null&&candidate.type.equals("application/x-mpegURL"))return candidate;
  }
  CastStream detected=probe(client,url.toString());return detected!=null?detected:new CastStream(url.toString(),type(path,"",""));
 }
 private static CastStream probe(OkHttpClient client,String url){
  try(Response response=client.newCall(new Request.Builder().url(url).get().build()).execute()){
   if(!response.isSuccessful()||response.body()==null)return null;
   okio.BufferedSource source=response.body().source();source.request(512);String prefix=source.buffer().clone().readString(StandardCharsets.UTF_8).trim();
   String mime=type(Playlist.httpUrl(url).encodedPath(),response.header("Content-Type",""),prefix);
   if(prefix.toLowerCase(Locale.ROOT).startsWith("<html")||prefix.toLowerCase(Locale.ROOT).startsWith("<!doctype html")||prefix.startsWith("{")||prefix.startsWith("["))return null;
   return new CastStream(url,mime);
  }catch(IOException|IllegalArgumentException error){return null;}
 }
 static String type(String path,String header,String prefix){
  String mime=header.split(";",2)[0].trim().toLowerCase(Locale.ROOT);path=path.toLowerCase(Locale.ROOT);prefix=prefix.replace("\uFEFF","").trim();
  if(prefix.startsWith("#EXTM3U")||mime.contains("mpegurl")||path.endsWith(".m3u8"))return "application/x-mpegURL";
  if(prefix.contains("<MPD")||mime.equals("application/dash+xml")||path.endsWith(".mpd"))return "application/dash+xml";
  if(mime.startsWith("video/")||mime.startsWith("audio/"))return mime;
  if(path.endsWith(".mp4")||path.endsWith(".m4v"))return "video/mp4";
  if(path.endsWith(".mkv"))return "video/x-matroska";if(path.endsWith(".avi"))return "video/x-msvideo";
  if(path.endsWith(".mp3"))return "audio/mpeg";if(path.endsWith(".aac"))return "audio/aac";if(path.endsWith(".m4a"))return "audio/mp4";if(path.endsWith(".flac"))return "audio/flac";if(path.endsWith(".wav"))return "audio/wav";if(path.endsWith(".ogg"))return "audio/ogg";
  return "video/mp2t";
 }
}
