package dk.stream.android;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class Playlist {
 public static final class Channel {
  public final String name, url, group;
  Channel(String name, String url, String group) { this.name=name; this.url=url; this.group=group; }
  @Override public String toString() { return name + (group.isEmpty() ? "" : "\n" + group); }
 }
 public static URI httpUri(String value) {
  URI uri=URI.create(value.trim());
  if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost()==null) throw new IllegalArgumentException("Brug et gyldigt HTTP- eller HTTPS-link");
  return uri;
 }
 public static List<Channel> parse(String text, String base) {
  List<Channel> result=new ArrayList<>(); String name=null, group="";
  URI source=httpUri(base);
  for(String raw:text.replace("\uFEFF", "").split("\\r?\\n")) {
   String line=raw.trim();
   if(line.startsWith("#EXTINF:")) {
    boolean quoted=false; int comma=-1;
    for(int i=0;i<line.length();i++) { if(line.charAt(i)=='"') quoted=!quoted; if(line.charAt(i)==',' && !quoted) { comma=i; break; } }
    name=comma<0?null:line.substring(comma+1).trim();
    var matcher=Pattern.compile("group-title=\"([^\"]*)\"").matcher(line);
    group=matcher.find()?matcher.group(1):"";
   } else if(!line.isEmpty() && !line.startsWith("#")) {
    try { String url=httpUri(source.resolve(line).toString()).toString(); result.add(new Channel(name==null || name.isEmpty()?"Kanal "+(result.size()+1):name,url,group)); } catch(IllegalArgumentException ignored) { }
    name=null; group="";
   }
  }
  return result;
 }
}
