package dk.stream.android;

import java.io.*;
import java.net.URI;
import java.util.*;

/** A user-selected video handed to an installed player, without changing Auto's host policy. */
public final class CarVideo {
 public final String title,uri,mime;
 public CarVideo(String title,String uri,String mime) {
  URI parsed=URI.create(uri);String scheme=parsed.getScheme();
  if(scheme==null||!Arrays.asList("http","https","content").contains(scheme.toLowerCase(Locale.ROOT))||parsed.getRawAuthority()==null||uri.indexOf('\r')>=0||uri.indexOf('\n')>=0)throw new IllegalArgumentException("Video URI required");
  this.title=title==null?"Video":title.replaceAll("[\\r\\n\\x00]"," ");this.uri=uri;this.mime=mime;
 }
 public static CarVideo channel(Playlist.Channel channel) {
  if(channel.seriesId!=null)throw new IllegalArgumentException("Choose an episode first");
  String path=Objects.toString(URI.create(channel.url).getPath(),"").toLowerCase(Locale.ROOT);
  String mime=path.endsWith(".m3u8")||path.endsWith(".m3u")?"application/x-mpegurl":path.endsWith(".mpd")?"application/dash+xml":path.endsWith(".mkv")?"video/x-matroska":path.endsWith(".webm")?"video/webm":path.endsWith(".ts")?"video/mp2t":path.endsWith(".avi")?"video/x-msvideo":channel.kind==Playlist.Kind.TV&&!path.endsWith(".mp4")?"video/mp2t":"video/mp4";
  return new CarVideo(channel.name,channel.url,mime);
 }
 public static CarVideo local(LocalMedia file) {
  if(file.folder||file.audio()||!file.supported())throw new IllegalArgumentException("Video file required");
  return new CarVideo(file.displayName(),file.uri,file.mime.startsWith("video/")?file.mime:"video/*");
 }
 public static List<CarVideo> favorites(Library library) {
  List<CarVideo> result=new ArrayList<>();Set<String> seen=new HashSet<>();
  for(Library.Entry entry:library.favorites)if(entry.channel.seriesId==null)try {CarVideo video=channel(entry.channel);if(video.uri.startsWith("http")&&seen.add(video.uri))result.add(video);}catch(IllegalArgumentException ignored){}
  return result;
 }
 public static void writePlaylist(Writer output,List<CarVideo> videos) throws IOException {
  // Content URIs need an individual Android read grant and must never be embedded in an export.
  for(CarVideo video:videos)if(!video.uri.startsWith("http://")&&!video.uri.startsWith("https://"))throw new IllegalArgumentException("Network videos required");
  output.write("#EXTM3U\n");for(CarVideo video:videos){output.write("#EXTINF:-1,");output.write(video.title);output.write('\n');output.write(video.uri);output.write('\n');}
 }
 @Override public String toString(){return title;}
}
