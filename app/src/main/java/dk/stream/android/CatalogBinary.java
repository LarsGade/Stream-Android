package dk.stream.android;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Incremental catalog encoding: no intermediate JSON trees or whole-catalog byte arrays. */
final class CatalogBinary {
 static final int MAGIC=0x53434132;
 private static void text(DataOutputStream out,String value)throws IOException{byte[] bytes=value.getBytes(StandardCharsets.UTF_8);out.writeInt(bytes.length);out.write(bytes);}
 private static String text(DataInputStream in)throws IOException{int count=in.readInt();if(count<0||count>2*1024*1024)throw new IOException("Invalid catalog string");byte[] bytes=new byte[count];in.readFully(bytes);return new String(bytes,StandardCharsets.UTF_8);}
 private static int count(DataInputStream in)throws IOException{int value=in.readInt();if(value<0||value>1000000)throw new IOException("Invalid catalog count");return value;}
 static void write(OutputStream output,Library.Source source,Catalog catalog)throws IOException{
  DataOutputStream out=new DataOutputStream(output);out.writeInt(MAGIC);out.writeBoolean(source.xtream);text(out,source.server);text(out,source.username);text(out,source.password);
  for(Playlist.Kind kind:Playlist.Kind.values()){java.util.List<String> groups=catalog.groups(kind);out.writeInt(groups.size());for(String group:groups)text(out,group);}
  out.writeInt(catalog.items.size());for(Playlist.Channel item:catalog.items){text(out,item.name);text(out,item.url);text(out,item.group);out.writeInt(item.kind.ordinal());out.writeBoolean(item.seriesId!=null);if(item.seriesId!=null)text(out,item.seriesId);text(out,item.logoUrl);text(out,item.epgId);text(out,item.epgName);}out.flush();
 }
 static Catalog read(InputStream input,Library.Source source)throws IOException{
  DataInputStream in=new DataInputStream(input);if(in.readInt()!=MAGIC)throw new IOException("Invalid catalog version");boolean xtream=in.readBoolean();String server=text(in),username=text(in),password=text(in);if(xtream!=source.xtream||!server.equals(source.server)||!username.equals(source.username)||!password.equals(source.password))throw new IOException("Different source");
  Catalog catalog=new Catalog();for(Playlist.Kind kind:Playlist.Kind.values()){int count=count(in);for(int i=0;i<count;i++)catalog.addGroup(kind,text(in));}
  int count=count(in);for(int i=0;i<count;i++){String name=text(in),url=text(in),group=text(in);int kind=in.readInt();if(kind<0||kind>=Playlist.Kind.values().length)throw new IOException("Invalid kind");String series=in.readBoolean()?text(in):null;catalog.add(new Playlist.Channel(name,url,group,Playlist.Kind.values()[kind],series,text(in),text(in),text(in)));}if(in.read()!=-1)throw new IOException("Trailing catalog data");return catalog;
 }
 private CatalogBinary(){}
}
