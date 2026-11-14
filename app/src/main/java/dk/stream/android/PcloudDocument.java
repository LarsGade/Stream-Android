package dk.stream.android;

import java.net.*;
import java.nio.charset.StandardCharsets;
import okhttp3.HttpUrl;

/** Opaque playback references contain a path and account identity, never credentials. */
public final class PcloudDocument {
 public static final String AUTHORITY="dk.stream.android.pcloud";
 public static boolean is(String uri){try{return "content".equals(URI.create(uri).getScheme())&&AUTHORITY.equals(URI.create(uri).getAuthority());}catch(Exception ignored){return false;}}
 public static String uri(PcloudAccount account,HttpUrl url,long size){
  checkOrigin(HttpUrl.get(account.server),url);
  try{return "content://"+AUTHORITY+"/"+account.id+"?url="+URLEncoder.encode(url.toString(),"UTF-8")+"&size="+Math.max(-1,size);}catch(java.io.UnsupportedEncodingException impossible){throw new AssertionError(impossible);}
 }
 public static HttpUrl url(PcloudAccount account,String uri){
  if(account==null||!is(uri)||!("/"+account.id).equals(URI.create(uri).getPath())||URI.create(uri).getRawQuery()==null)throw new IllegalArgumentException("Log på den tilknyttede Pcloud-konto igen.");
  try{for(String part:URI.create(uri).getRawQuery().split("&"))if(part.startsWith("url=")){HttpUrl url=HttpUrl.get(URLDecoder.decode(part.substring(4),"UTF-8"));checkOrigin(HttpUrl.get(account.server),url);return url;}}catch(java.io.UnsupportedEncodingException impossible){throw new AssertionError(impossible);}
  throw new IllegalArgumentException("Ugyldigt Pcloud-link.");
 }
 public static long size(String uri){try{for(String part:URI.create(uri).getRawQuery().split("&"))if(part.startsWith("size="))return Math.max(-1,Long.parseLong(part.substring(5)));}catch(Exception ignored){}return -1;}
 static boolean sameOrigin(HttpUrl base,HttpUrl url){return base.scheme().equals(url.scheme())&&base.host().equals(url.host())&&base.port()==url.port();}
 static void checkOrigin(HttpUrl base,HttpUrl url){if(!sameOrigin(base,url)||!url.username().isEmpty()||!url.password().isEmpty()||url.fragment()!=null)throw new IllegalArgumentException("Pcloud-linket peger uden for kontoen.");}
 private PcloudDocument(){}
}
