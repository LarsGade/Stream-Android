package dk.stream.android;

import java.util.*;
public final class Catalog {
 public final List<Playlist.Channel> items=new ArrayList<>();
 private final Map<Playlist.Kind,LinkedHashSet<String>> groups=new EnumMap<>(Playlist.Kind.class);
 public Catalog() { for(var kind:Playlist.Kind.values()) groups.put(kind,new LinkedHashSet<>()); }
 public Catalog(List<Playlist.Channel> channels) { this(); for(var channel:channels) add(channel); }
 public void addGroup(Playlist.Kind kind,String group) { if(!group.isEmpty()) groups.get(kind).add(group); }
 public void add(Playlist.Channel channel) { items.add(channel); addGroup(channel.kind,channel.group); }
 public List<String> groups(Playlist.Kind kind) { return new ArrayList<>(groups.get(kind)); }
 public List<Playlist.Channel> filter(Playlist.Kind kind,String group,String query) {
  List<Playlist.Channel> result=new ArrayList<>(); String q=query.toLowerCase(Locale.ROOT);
  for(var item:items) if(item.kind==kind && (group==null || group.equals(item.group)) && (item.name+" "+item.group).toLowerCase(Locale.ROOT).contains(q)) result.add(item);
  return result;
 }
}
