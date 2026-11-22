package dk.stream.android;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.xmlpull.v1.*;

/** XMLTV times are converted to UTC instants; names are only used when unambiguous. */
public final class EpgData {
 static final int LIMIT=250000;
 public static final class Programme {
  public final long start,end;public final String title;
  Programme(long start,long end,String title) {this.start=start;this.end=end;this.title=title;}
 }
 private final Map<String,String> aliases=new HashMap<>();
 private final Map<String,List<Programme>> programmes=new HashMap<>();
 private void alias(String name,String id) {
  String key=LocalChannelLogos.key(name);if(key.isEmpty()) return;
  String old=aliases.get(key);aliases.put(key,old==null||old.equals(id)?id:"");
 }
 public Programme current(Playlist.Channel channel,long now) {
  String id=channel.epgId;
  if(!id.isEmpty() && programmes.containsKey(id)) return current(id,now);
  for(String name:new String[]{channel.epgId,channel.epgName,channel.name}) {
   String match=aliases.get(LocalChannelLogos.key(name));
   if(match!=null && !match.isEmpty()) {Programme programme=current(match,now);if(programme!=null) return programme;}
  }
  return null;
 }
 private Programme current(String id,long now) {
  List<Programme> list=programmes.get(id);if(list==null) return null;
  Programme found=null;
  for(int i=0;i<list.size();i++) {
   Programme programme=list.get(i);if(programme.start>now) break;
   long end=programme.end;
   if(end==0 && i+1<list.size()) end=list.get(i+1).start;
   if(programme.start<=now && end>now) found=programme;
  }
  return found;
 }
 public static long time(String value) {
  Matcher matcher=Pattern.compile("^(\\d{8}(?:\\d{2}){0,3})(?:\\s*(Z|UTC|[+-]\\d{2}:?\\d{2}))?$").matcher(value.trim());
  if(!matcher.matches()) return -1;
  String digits=matcher.group(1),zone=matcher.group(2);String pattern=digits.length()==14?"yyyyMMddHHmmss":digits.length()==12?"yyyyMMddHHmm":digits.length()==10?"yyyyMMddHH":"yyyyMMdd";
  TimeZone timezone=TimeZone.getTimeZone("UTC");
  if(zone!=null && !zone.equals("Z") && !zone.equals("UTC")) {
   String offset=zone.replace(":","");int hours=Integer.parseInt(offset.substring(1,3)),minutes=Integer.parseInt(offset.substring(3,5));
   if(hours>23||minutes>59) return -1;timezone=TimeZone.getTimeZone("GMT"+offset.substring(0,3)+":"+offset.substring(3));
  }
  SimpleDateFormat format=new SimpleDateFormat(pattern,Locale.ROOT);format.setLenient(false);format.setTimeZone(timezone);
  ParsePosition position=new ParsePosition(0);Date date=format.parse(digits,position);return date==null||position.getIndex()!=digits.length()?-1:date.getTime();
 }
 private static String text(XmlPullParser parser) throws IOException,XmlPullParserException {
  int depth=parser.getDepth();StringBuilder result=new StringBuilder();int event;
  while((event=parser.next())!=XmlPullParser.END_DOCUMENT) {
   if(event==XmlPullParser.END_TAG && parser.getDepth()==depth) break;
   if(event==XmlPullParser.TEXT && result.length()<4096) result.append(parser.getText(),0,Math.min(4096-result.length(),parser.getText().length()));
  }
  return result.toString().trim();
 }
 public static EpgData parse(InputStream input,long now) throws IOException {
  try {
   XmlPullParser parser=XmlPullParserFactory.newInstance().newPullParser();try {parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL,false);} catch(XmlPullParserException unsupported) {if(parser.getFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL)) throw unsupported;}
   if(parser.getFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL)) throw new IOException("DTD processing must be disabled");parser.setInput(input,null);
   EpgData data=new EpgData();String channelId="",programmeId="",title="";List<String> names=new ArrayList<>();long start=-1,end=-1;int count=0;boolean tv=false;
   int event;while((event=parser.next())!=XmlPullParser.END_DOCUMENT) {
    if(event==XmlPullParser.START_TAG) {
     String tag=parser.getName();
     if(parser.getDepth()==1) {if(!tag.equals("tv")) throw new IOException("Not XMLTV");tv=true;}
     if(tag.equals("channel") && parser.getDepth()==2) {channelId=parser.getAttributeValue(null,"id");names=new ArrayList<>();}
     else if(tag.equals("display-name") && channelId!=null && !channelId.isEmpty()) names.add(text(parser));
     else if(tag.equals("programme") && parser.getDepth()==2) {
      programmeId=parser.getAttributeValue(null,"channel");String from=parser.getAttributeValue(null,"start"),to=parser.getAttributeValue(null,"stop");
      start=from==null?-1:time(from);end=to==null?0:time(to);title="";
     } else if(tag.equals("title") && programmeId!=null && !programmeId.isEmpty()) {String value=text(parser);if(title.isEmpty()) title=value;}
    } else if(event==XmlPullParser.END_TAG) {
     if(parser.getName().equals("channel") && parser.getDepth()==2) {
      if(channelId!=null && !channelId.isEmpty()) {data.alias(channelId,channelId);for(String name:names) data.alias(name,channelId);}channelId="";
     } else if(parser.getName().equals("programme") && parser.getDepth()==2) {
      if(programmeId!=null && !programmeId.isEmpty() && start>=0 && start<=now+6*60*60*1000L && (end>now || (end==0 && start>=now-24*60*60*1000L)) && !title.isEmpty()) {
       if(++count>LIMIT) throw new IOException("EPG too large");
       List<Programme> list=data.programmes.get(programmeId);if(list==null) {list=new ArrayList<>();data.programmes.put(programmeId,list);}
       list.add(new Programme(start,end,title));data.alias(programmeId,programmeId);
      }
      programmeId="";
     }
    }
   }
   if(!tv) throw new IOException("Not XMLTV");
   for(List<Programme> list:data.programmes.values()) Collections.sort(list,(a,b)->Long.compare(a.start,b.start));
   return data;
  } catch(XmlPullParserException|IllegalArgumentException e) {throw new IOException("Invalid XMLTV",e);}
 }
 public void write(DataOutputStream output) throws IOException {
  output.writeInt(1);output.writeInt(aliases.size());for(Map.Entry<String,String> entry:aliases.entrySet()) {output.writeUTF(entry.getKey());output.writeUTF(entry.getValue());}
  output.writeInt(programmes.size());
  for(Map.Entry<String,List<Programme>> entry:programmes.entrySet()) {output.writeUTF(entry.getKey());output.writeInt(entry.getValue().size());for(Programme programme:entry.getValue()) {output.writeLong(programme.start);output.writeLong(programme.end);output.writeUTF(programme.title);}}
 }
 public static EpgData read(DataInputStream input) throws IOException {
  if(input.readInt()!=1) throw new IOException("Invalid cache");EpgData data=new EpgData();int names=input.readInt();if(names<0||names>LIMIT) throw new IOException("Invalid cache");
  for(int i=0;i<names;i++) data.aliases.put(input.readUTF(),input.readUTF());
  int channels=input.readInt(),total=0;if(channels<0||channels>LIMIT) throw new IOException("Invalid cache");
  for(int i=0;i<channels;i++) {String id=input.readUTF();int count=input.readInt();if(count<0||(total+=count)>LIMIT) throw new IOException("Invalid cache");List<Programme> list=new ArrayList<>();for(int j=0;j<count;j++) list.add(new Programme(input.readLong(),input.readLong(),input.readUTF()));data.programmes.put(id,list);}
  return data;
 }
}
