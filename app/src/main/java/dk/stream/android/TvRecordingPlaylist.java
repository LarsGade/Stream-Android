package dk.stream.android;

import java.io.IOException;
import java.net.URI;
import java.util.*;

/** A snapshot of a live HLS media playlist. Sequence numbers prevent duplicate segments. */
final class TvRecordingPlaylist {
 static final class Segment {
  final long sequence;final String url,key,iv,init;
  Segment(long sequence,String url,String key,String iv,String init){this.sequence=sequence;this.url=url;this.key=key;this.iv=iv;this.init=init;}
 }
 final List<Segment> segments=new ArrayList<>();String variant;boolean ended;long targetSeconds=4;
 static TvRecordingPlaylist parse(String base,String text)throws IOException {
  if(!text.trim().startsWith("#EXTM3U"))throw new IOException("Kilden er ikke en HLS-afspilningsliste.");
  TvRecordingPlaylist result=new TvRecordingPlaylist();long sequence=0;String key=null,iv=null,init=null;boolean variant=false;
  for(String value:text.split("\\r?\\n")){String line=value.trim();
   if(line.startsWith("#EXT-X-MEDIA-SEQUENCE:"))sequence=number(line.substring(22));
   else if(line.startsWith("#EXT-X-TARGETDURATION:"))result.targetSeconds=Math.max(1,number(line.substring(22)));
   else if(line.startsWith("#EXT-X-MEDIA:")&&"AUDIO".equals(attribute(line,"TYPE"))&&attribute(line,"URI")!=null)throw new IOException("Optagelse af TV med separate lydspor understøttes ikke.");
   else if(line.startsWith("#EXT-X-STREAM-INF:"))variant=true;
   else if(line.startsWith("#EXT-X-BYTERANGE:"))throw new IOException("Optagelse af denne HLS-type understøttes ikke.");
   else if(line.startsWith("#EXT-X-KEY:")){String method=attribute(line,"METHOD");if("NONE".equals(method)){key=null;iv=null;}else if("AES-128".equals(method)&&(attribute(line,"KEYFORMAT")==null||"identity".equals(attribute(line,"KEYFORMAT")))){key=resolve(base,attribute(line,"URI"));iv=attribute(line,"IV");}else throw new IOException("Kopibeskyttede TV-kilder kan ikke optages.");}
   else if(line.startsWith("#EXT-X-MAP:")){if(key!=null)throw new IOException("Krypterede MP4-TV-kilder kan ikke optages.");if(attribute(line,"BYTERANGE")!=null)throw new IOException("Optagelse af denne HLS-type understøttes ikke.");init=resolve(base,attribute(line,"URI"));}
   else if(line.equals("#EXT-X-ENDLIST"))result.ended=true;
   else if(!line.isEmpty()&&!line.startsWith("#")){String url=resolve(base,line);if(variant){if(result.variant==null)result.variant=url;variant=false;}else result.segments.add(new Segment(sequence++,url,key,iv,init));}
  }
  return result;
 }
 static String attribute(String line,String name){java.util.regex.Matcher match=java.util.regex.Pattern.compile("(?:[:,])\\s*"+name+"=(?:\"([^\"]*)\"|([^,]*))").matcher(line);return match.find()?(match.group(1)!=null?match.group(1):match.group(2).trim()):null;}
 static byte[] iv(String value,long sequence){byte[] result=new byte[16];if(value==null){for(int i=15;i>=8;i--){result[i]=(byte)sequence;sequence>>>=8;}}else{String hex=value.replaceFirst("^0[xX]","");if(hex.length()>32)throw new IllegalArgumentException("Ugyldig krypteringsnøgle.");while(hex.length()<32)hex="0"+hex;for(int i=0;i<16;i++)result[i]=(byte)Integer.parseInt(hex.substring(i*2,i*2+2),16);}return result;}
 private static long number(String value)throws IOException{try{return Long.parseLong(value.trim());}catch(NumberFormatException error){throw new IOException("Ugyldig TV-afspilningsliste.",error);}}
 private static String resolve(String base,String value)throws IOException{try{if(value==null)throw new IllegalArgumentException();URI uri=URI.create(base).resolve(value);if(!"http".equalsIgnoreCase(uri.getScheme())&&!"https".equalsIgnoreCase(uri.getScheme()))throw new IllegalArgumentException();return uri.toString();}catch(IllegalArgumentException error){throw new IOException("Ugyldig TV-adresse.",error);}}
}
