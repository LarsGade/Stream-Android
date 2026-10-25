package dk.stream.android;
import okhttp3.HttpUrl;
import java.net.URLDecoder;

public final class LogoUrls {
 private LogoUrls() { }
 public static String resolve(String raw,HttpUrl base) {
  if(raw==null) return "";
  String value=raw.trim().replace("\\/","/").replace("&amp;","&").replace("&#38;","&").replace("&#x26;","&");
  if(value.isEmpty() || value.equalsIgnoreCase("null")) return "";
  if(value.matches("(?i)^https?%3a.*")) {
   try {value=URLDecoder.decode(value,"UTF-8");} catch(Exception ignored) {return "";}
  }
  if(value.matches("(?i)^[a-z0-9.-]+\\.[a-z]{2,}(?::[0-9]+)?/.*")) value=base.scheme()+"://"+value;
  HttpUrl url=base.resolve(value);
  return url==null?"":url.toString();
 }
}
