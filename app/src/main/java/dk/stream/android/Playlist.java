package dk.stream.android;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class Playlist {
 public enum Kind { TV, FILM, SERIES }
 public static final class Channel {
  public final String name, url, group, seriesId, logoUrl;
  public final Kind kind;
  Channel(String name,String url,String group) { this(name,url,group,Kind.TV,null); }
  Channel(String name,String url,String group,Kind kind,String seriesId) { this(name,url,group,kind,seriesId,""); }
  Channel(String name,String url,String group,Kind kind,String seriesId,String logoUrl) {
   this.name=name; this.url=url; this.group=group; this.kind=kind; this.seriesId=seriesId; this.logoUrl=logoUrl;
  }
  @Override public String toString() { return name + (group.isEmpty() ? "" : "\n" + group); }
 }
 public static Kind inferKind(String metadata,String group,String url) {
  String hint=(metadata+" "+group).toLowerCase(java.util.Locale.ROOT);
  String path=httpUrl(url).encodedPath().toLowerCase(java.util.Locale.ROOT);
  var declared=Pattern.compile("(?:tvg-type|media-type|stream-type|type)\\s*=\\s*[\"']?([a-zA-Z-]+)",Pattern.CASE_INSENSITIVE).matcher(metadata);
  if(declared.find()) {
   String type=declared.group(1).toLowerCase(java.util.Locale.ROOT);
   if(type.equals("live") || type.equals("tv")) return Kind.TV;
   if(type.equals("series") || type.equals("serie")) return Kind.SERIES;
   if(type.equals("movie") || type.equals("vod") || type.equals("film")) return Kind.FILM;
  }
  if(path.contains("/live/")) return Kind.TV;
  if(path.contains("/series/") || hint.matches(".*\\b(series|serier|serie|tvshows|tv-shows)\\b.*")) return Kind.SERIES;
  if(path.contains("/movie/") || hint.matches(".*\\b(movie|movies|film|films|vod)\\b.*")) return Kind.FILM;
  if(path.endsWith(".mp4") || path.endsWith(".mkv") || path.endsWith(".avi")) return Kind.FILM;
  return Kind.TV;
 }
 public static URI httpUri(String value) {
  return httpUrl(value).uri();
 }
 public static okhttp3.HttpUrl httpUrl(String value) {
  String input=value.trim().replace("&amp;", "&");
  if(input.length()>=2 && input.startsWith("\"") && input.endsWith("\"")) input=input.substring(1,input.length()-1).trim();
  if(!input.contains("://") && (!input.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*") || input.matches("^[^/?#:]+:[0-9]+.*")) ) input="https://"+input;
  okhttp3.HttpUrl url=okhttp3.HttpUrl.parse(input);
  if(url==null) throw new IllegalArgumentException("Invalid HTTP URL");
  return url;
 }
 public static String xtreamUrl(String server,String username,String password) {
  if(username.trim().isEmpty() || password.isEmpty()) throw new IllegalArgumentException("Missing credentials");
  okhttp3.HttpUrl base=httpUrl(server);
  if(!base.username().isEmpty() || !base.password().isEmpty()) throw new IllegalArgumentException("Use separate credentials");
  String path=base.encodedPath();
  if(path.endsWith("/get.php") || path.endsWith("/player_api.php")) path=path.substring(0,path.lastIndexOf('/')+1);
  if(!path.endsWith("/")) path+="/";
  return base.newBuilder().encodedPath(path+"get.php").query(null).fragment(null)
   .addQueryParameter("username",username.trim()).addQueryParameter("password",password)
   .addQueryParameter("type","m3u_plus").addQueryParameter("output","m3u8").build().toString();
 }
 public static String logo(String metadata,okhttp3.HttpUrl base) {
  var matcher=Pattern.compile("tvg-logo\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')",Pattern.CASE_INSENSITIVE).matcher(metadata);
  if(!matcher.find()) return "";
  String value=matcher.group(1)!=null?matcher.group(1):matcher.group(2);
  if(value.isEmpty()) return "";
  okhttp3.HttpUrl url=base.resolve(value.replace("&amp;","&")); return url==null?"":url.toString();
 }
 public static List<Channel> parse(String text, String base) {
  List<Channel> result=new ArrayList<>(); String name=null, group="", metadata="";
  okhttp3.HttpUrl source=httpUrl(base);
  for(String raw:text.replace("\uFEFF", "").split("\\r\\n|\\n|\\r")) {
   String line=raw.trim();
   if(line.startsWith("#EXTINF:")) {
    boolean quoted=false; int comma=-1;
    for(int i=0;i<line.length();i++) { if(line.charAt(i)=='"') quoted=!quoted; if(line.charAt(i)==',' && !quoted) { comma=i; break; } }
    metadata=comma<0?line:line.substring(0,comma);
    name=comma<0?null:line.substring(comma+1).trim();
    var matcher=Pattern.compile("group-title=\"([^\"]*)\"").matcher(line);
    group=matcher.find()?matcher.group(1):"";
   } else if(line.startsWith("#EXTGRP:")) {
    group=line.substring(8).trim();
   } else if(!line.isEmpty() && !line.startsWith("#")) {
    if(name!=null || line.matches("(?i)^https?://.*")) {
     okhttp3.HttpUrl resolved=source.resolve(line.replace("&amp;","&"));
     if(resolved!=null) result.add(new Channel(name==null || name.isEmpty()?"Kanal "+(result.size()+1):name,resolved.toString(),group,inferKind(metadata,group,resolved.toString()),null,logo(metadata,source)));
    }
    name=null; group=""; metadata="";
   }
  }
  return result;
 }
}
