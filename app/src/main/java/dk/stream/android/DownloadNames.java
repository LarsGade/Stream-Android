package dk.stream.android;
import java.util.*;
final class DownloadNames {
 static String clean(String name){String value=name==null?"":name.replaceAll("[\\x00-\\x1F\\x7F/\\\\:*?\"<>|]","_").trim();while(value.startsWith("."))value=value.substring(1);return value.isEmpty()?"Download":value;}
 static String path(String name){StringBuilder result=new StringBuilder();for(String part:name.replace('\\','/').split("/")){if(part.isEmpty()||part.equals(".")||part.equals(".."))continue;if(result.length()>0)result.append('/');result.append(clean(part));}return result.length()==0?"Download":result.toString();}
 static String unique(String name,Set<String> used){String safe=path(name),value=safe;int suffix=2;int slash=safe.lastIndexOf('/'),dot=safe.lastIndexOf('.');while(!used.add(value.toLowerCase(Locale.ROOT))){value=dot>slash?safe.substring(0,dot)+" ("+suffix+++")"+safe.substring(dot):safe+" ("+suffix+++")";}return value;}
 static String file(String title,String url){String name=clean(title);String path=Playlist.httpUrl(url).encodedPath();String leaf=path.substring(path.lastIndexOf('/')+1);int dot=leaf.lastIndexOf('.');if(dot>=0){String extension=leaf.substring(dot).toLowerCase(Locale.ROOT);if(extension.matches("\\.[a-z0-9]{1,6}")&&!name.toLowerCase(Locale.ROOT).endsWith(extension))name+=extension;}return name;}
 private DownloadNames(){}
}
