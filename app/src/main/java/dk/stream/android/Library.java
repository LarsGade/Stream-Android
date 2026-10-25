package dk.stream.android;

import java.util.*;
import org.json.*;

/** Ordered local sources and favorite snapshots. Serialize only into encrypted storage. */
public final class Library {
 public static final class Source {
  public final String id,name,server,username,password;
  public final boolean xtream;
  public Source(String id,String name,String server,String username,String password,boolean xtream) {
   this.id=id; this.name=name; this.server=server; this.username=username; this.password=password; this.xtream=xtream;
  }
  public static Source m3u(String name,String url) {
   String normalized=Playlist.httpUrl(url).toString();
   return new Source(UUID.randomUUID().toString(),label(name,normalized),normalized,"","",false);
  }
  public static Source xtream(String name,String server,String username,String password) {
   Playlist.xtreamUrl(server,username,password);
   String normalized=Playlist.httpUrl(server).toString();
   return new Source(UUID.randomUUID().toString(),label(name,normalized),normalized,username.trim(),password,true);
  }
  private static String label(String name,String url) { return name.trim().isEmpty()?Playlist.httpUrl(url).host():name.trim(); }
  @Override public String toString() { return name; }
 }
 public static final class Entry {
  public final String sourceId;
  public final Playlist.Channel channel;
  public Entry(String sourceId,Playlist.Channel channel) { this.sourceId=sourceId; this.channel=channel; }
  boolean same(Entry other) {
   return sourceId.equals(other.sourceId) && channel.kind==other.channel.kind
    && Objects.equals(channel.seriesId,other.channel.seriesId) && channel.url.equals(other.channel.url);
  }
 }
 public final List<Source> sources=new ArrayList<>();
 public final List<Entry> favorites=new ArrayList<>();
 public boolean favoritesOnStart;
 public String lastSourceId="";
 public Source findSource(String id) { for(Source source:sources) if(source.id.equals(id)) return source; return null; }
 public Source addSource(Source source) {
  for(Source old:sources) if(old.xtream==source.xtream && old.server.equals(source.server) && old.username.equals(source.username) && old.password.equals(source.password)) return old;
  sources.add(source); return source;
 }
 public void removeSource(String id) {
  for(int i=sources.size()-1;i>=0;i--) if(sources.get(i).id.equals(id)) sources.remove(i);
  for(int i=favorites.size()-1;i>=0;i--) if(favorites.get(i).sourceId.equals(id)) favorites.remove(i);
  if(lastSourceId.equals(id)) lastSourceId=sources.isEmpty()?"":sources.get(0).id;
 }
 public boolean isFavorite(Entry entry) { for(Entry item:favorites) if(item.same(entry)) return true; return false; }
 public void toggleFavorite(Entry entry) {
  for(int i=0;i<favorites.size();i++) if(favorites.get(i).same(entry)) {favorites.remove(i);return;}
  favorites.add(entry);
 }
 public List<Entry> favoriteItems(Playlist.Kind kind,String group,String query) {
  List<Entry> result=new ArrayList<>();String q=query.toLowerCase(Locale.ROOT);
  for(Entry entry:favorites) {
   Playlist.Channel item=entry.channel;
   if(item.kind==kind && (group==null || group.equals(item.group)) && (item.name+" "+item.group).toLowerCase(Locale.ROOT).contains(q)) result.add(entry);
  }
  return result;
 }
 public void refreshFavorites(String sourceId,List<Playlist.Channel> items) {
  // Keep favorites in the user's order while refreshing names, logos and links from this source.
  Map<String,Playlist.Channel> current=new HashMap<>();
  for(Playlist.Channel item:items) current.put(identity(item),item);
  for(int i=0;i<favorites.size();i++) {
   Entry old=favorites.get(i);
   if(old.sourceId.equals(sourceId)) {
    Playlist.Channel fresh=current.get(identity(old.channel));
    if(fresh!=null) favorites.set(i,new Entry(sourceId,fresh));
   }
  }
 }
 private static String identity(Playlist.Channel item) { return item.kind.name()+":"+(item.seriesId==null?item.url:item.seriesId); }
 public String toJson() throws JSONException {
  JSONObject root=new JSONObject(); root.put("version",1);root.put("favoritesOnStart",favoritesOnStart);root.put("lastSourceId",lastSourceId);
  JSONArray savedSources=new JSONArray();
  for(Source source:sources) {
   JSONObject value=new JSONObject();value.put("id",source.id);value.put("name",source.name);value.put("server",source.server);value.put("username",source.username);value.put("password",source.password);value.put("xtream",source.xtream);savedSources.put(value);
  }
  root.put("sources",savedSources);
  JSONArray savedFavorites=new JSONArray();
  for(Entry entry:favorites) {
   Playlist.Channel channel=entry.channel;JSONObject value=new JSONObject();value.put("sourceId",entry.sourceId);value.put("name",channel.name);value.put("url",channel.url);value.put("group",channel.group);value.put("kind",channel.kind.name());value.put("seriesId",channel.seriesId==null?JSONObject.NULL:channel.seriesId);value.put("logoUrl",channel.logoUrl);savedFavorites.put(value);
  }
  root.put("favorites",savedFavorites);return root.toString();
 }
 public static Library fromJson(String json) throws JSONException {
  Library library=new Library();JSONObject root=new JSONObject(json);
  if(root.optInt("version")!=1) throw new JSONException("Unsupported library format");
  library.favoritesOnStart=root.optBoolean("favoritesOnStart");library.lastSourceId=root.optString("lastSourceId","");
  JSONArray sources=root.getJSONArray("sources");
  for(int i=0;i<sources.length();i++) {
   JSONObject value=sources.getJSONObject(i);
   library.sources.add(new Source(value.getString("id"),value.getString("name"),value.getString("server"),value.optString("username",""),value.optString("password",""),value.getBoolean("xtream")));
  }
  JSONArray favorites=root.getJSONArray("favorites");
  for(int i=0;i<favorites.length();i++) {
   JSONObject value=favorites.getJSONObject(i);
   if(library.findSource(value.getString("sourceId"))==null) continue;
   Playlist.Kind kind;
   try { kind=Playlist.Kind.valueOf(value.getString("kind")); } catch(IllegalArgumentException e) { throw new JSONException("Unsupported content type"); }
   Playlist.Channel channel=new Playlist.Channel(value.getString("name"),value.getString("url"),value.optString("group",""),kind,value.isNull("seriesId")?null:value.getString("seriesId"),value.optString("logoUrl",""));
   library.favorites.add(new Entry(value.getString("sourceId"),channel));
  }
  return library;
 }
}
