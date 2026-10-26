package dk.stream.android;

import java.util.*;
import org.json.*;

/** Ordered local sources and favorite snapshots. Serialize only into encrypted storage. */
public final class Library {
 public static final class Source {
  public final String id,name,server,username,password,epgId;
  public final boolean xtream;
  public Source(String id,String name,String server,String username,String password,boolean xtream) {this(id,name,server,username,password,xtream,"");}
  public Source(String id,String name,String server,String username,String password,boolean xtream,String epgId) {
   this.id=id; this.name=name; this.server=server; this.username=username; this.password=password; this.xtream=xtream;this.epgId=epgId;
  }
  public Source withEpg(String epgId) {return new Source(id,name,server,username,password,xtream,epgId);}
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
 public static final class Guide {
  public final String id,name,url;
  public Guide(String id,String name,String url) {this.id=id;this.name=name;this.url=url;}
  public static Guide create(String name,String url) {
   if(name.trim().isEmpty()) throw new IllegalArgumentException("Guide name required");
   return new Guide(UUID.randomUUID().toString(),name.trim(),Playlist.httpUrl(url).toString());
  }
  @Override public String toString() {return name;}
 }
 public final List<RadioStation> radioFavorites=new ArrayList<>();
 public RadioStation findRadio(String id) {for(RadioStation station:radioFavorites) if(station.id.equals(id)) return station;return null;}
 public void addRadio(RadioStation station) {if(findRadio(station.id)==null) radioFavorites.add(station);}
 public void removeRadio(String id) {for(int i=radioFavorites.size()-1;i>=0;i--) if(radioFavorites.get(i).id.equals(id)) radioFavorites.remove(i);}
 public void renameRadio(String id,String name) {for(int i=0;i<radioFavorites.size();i++) if(radioFavorites.get(i).id.equals(id)) {radioFavorites.set(i,radioFavorites.get(i).renamed(name));return;}}
 public void reorderRadio(List<RadioStation> ordered) {
  Set<String> expected=new HashSet<>();for(RadioStation station:radioFavorites) expected.add(station.id);
  if(ordered.size()!=radioFavorites.size()) throw new IllegalArgumentException("Favorite set changed");
  for(RadioStation station:ordered) if(!expected.remove(station.id)) throw new IllegalArgumentException("Favorite set changed");
  List<RadioStation> fresh=new ArrayList<>();for(RadioStation station:ordered) fresh.add(findRadio(station.id));radioFavorites.clear();radioFavorites.addAll(fresh);
 }
 public final List<Guide> guides=new ArrayList<>();
 public Guide findGuide(String id) {for(Guide guide:guides) if(guide.id.equals(id)) return guide;return null;}
 public void putGuide(Guide guide) {for(int i=0;i<guides.size();i++) if(guides.get(i).id.equals(guide.id)) {guides.set(i,guide);return;}guides.add(guide);}
 public void assignEpg(String sourceId,String epgId) {for(int i=0;i<sources.size();i++) if(sources.get(i).id.equals(sourceId)) sources.set(i,sources.get(i).withEpg(epgId));}
 public void removeGuide(String id) {
  for(int i=guides.size()-1;i>=0;i--) if(guides.get(i).id.equals(id)) guides.remove(i);
  for(int i=0;i<sources.size();i++) if(sources.get(i).epgId.equals(id)) sources.set(i,sources.get(i).withEpg(""));
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
 public boolean favoritesOnStart,autoPlayLast,miniPlayer;
 public Playlist.Channel lastPlayed;
 public long lastPlayedPosition;
 public String lastSourceId="";
 public Source findSource(String id) { for(Source source:sources) if(source.id.equals(id)) return source; return null; }
 public Source addSource(Source source) {
  for(int i=0;i<sources.size();i++) if(sources.get(i).id.equals(source.id)) {sources.set(i,source);return source;}
  for(int i=0;i<sources.size();i++) {
   Source old=sources.get(i);
   if(old.xtream==source.xtream && old.server.equals(source.server) && old.username.equals(source.username) && old.password.equals(source.password)) {
    Source updated=old.withEpg(source.epgId);sources.set(i,updated);return updated;
   }
  }
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
 public void removeFavorite(Entry entry) {for(int i=favorites.size()-1;i>=0;i--) if(favorites.get(i).same(entry)) favorites.remove(i);}
 public void reorderFavorites(List<Entry> ordered) {
  if(ordered.size()!=favorites.size()) throw new IllegalArgumentException("Favorite set changed");
  List<Entry> remaining=new ArrayList<>(favorites);
  for(Entry entry:ordered) {int found=-1;for(int i=0;i<remaining.size();i++) if(remaining.get(i).same(entry)) {found=i;break;}if(found<0) throw new IllegalArgumentException("Favorite set changed");remaining.remove(found);}
  favorites.clear();favorites.addAll(ordered);
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
  // Avoid building indexes for large catalogs when this source has no favorites.
  boolean needed=false;for(Entry entry:favorites) if(entry.sourceId.equals(sourceId)) {needed=true;break;}
  if(!needed) return;
  // Keep favorites in the user's order while refreshing names, logos and links from this source.
  Set<String> wantedIds=new HashSet<>(),wantedNames=new HashSet<>(),wantedEpg=new HashSet<>();
  for(Entry entry:favorites) if(entry.sourceId.equals(sourceId)) {
   wantedIds.add(identity(entry.channel));wantedNames.add(entry.channel.kind+":"+LocalChannelLogos.key(entry.channel.name));
   if(!entry.channel.epgId.isEmpty()) wantedEpg.add(entry.channel.kind+":"+entry.channel.epgId);
  }
  Map<String,Playlist.Channel> current=new HashMap<>();
  Map<String,Playlist.Channel> byEpg=new HashMap<>(),byName=new HashMap<>();Set<String> duplicateEpg=new HashSet<>(),duplicateName=new HashSet<>();
  for(Playlist.Channel item:items) {
   String identity=identity(item);if(wantedIds.contains(identity)) current.put(identity,item);
   String name=item.kind+":"+LocalChannelLogos.key(item.name);
   if(wantedNames.contains(name) && byName.put(name,item)!=null) duplicateName.add(name);
   if(!item.epgId.isEmpty()) {String id=item.kind+":"+item.epgId;if(wantedEpg.contains(id) && byEpg.put(id,item)!=null) duplicateEpg.add(id);}
  }
  for(int i=0;i<favorites.size();i++) {
   Entry old=favorites.get(i);
   if(old.sourceId.equals(sourceId)) {
    Playlist.Channel fresh=current.get(identity(old.channel));
    String epg=old.channel.kind+":"+old.channel.epgId,name=old.channel.kind+":"+LocalChannelLogos.key(old.channel.name);
    if(fresh==null && !old.channel.epgId.isEmpty() && !duplicateEpg.contains(epg)) fresh=byEpg.get(epg);
    if(fresh==null && !duplicateName.contains(name)) fresh=byName.get(name);
    if(fresh!=null) favorites.set(i,new Entry(sourceId,fresh));
   }
  }
 }
 private static String identity(Playlist.Channel item) { return item.kind.name()+":"+(item.seriesId==null?item.url:item.seriesId); }
 public String toJson() throws JSONException {
  JSONObject root=new JSONObject(); root.put("version",1);root.put("favoritesOnStart",favoritesOnStart);root.put("lastSourceId",lastSourceId);
  root.put("autoPlayLast",autoPlayLast);root.put("miniPlayer",miniPlayer);root.put("lastPlayedPosition",lastPlayedPosition);
  if(lastPlayed!=null) {
   JSONObject value=new JSONObject();value.put("name",lastPlayed.name);value.put("url",lastPlayed.url);value.put("group",lastPlayed.group);value.put("kind",lastPlayed.kind.name());value.put("logoUrl",lastPlayed.logoUrl);value.put("epgId",lastPlayed.epgId);value.put("epgName",lastPlayed.epgName);root.put("lastPlayed",value);
  }
  JSONArray savedSources=new JSONArray();
  for(Source source:sources) {
   JSONObject value=new JSONObject();value.put("id",source.id);value.put("name",source.name);value.put("server",source.server);value.put("username",source.username);value.put("password",source.password);value.put("xtream",source.xtream);value.put("epgId",source.epgId);savedSources.put(value);
  }
  root.put("sources",savedSources);
  JSONArray savedGuides=new JSONArray();
  for(Guide guide:guides) {JSONObject value=new JSONObject();value.put("id",guide.id);value.put("name",guide.name);value.put("url",guide.url);savedGuides.put(value);}
  root.put("guides",savedGuides);
  JSONArray savedFavorites=new JSONArray();
  for(Entry entry:favorites) {
   Playlist.Channel channel=entry.channel;JSONObject value=new JSONObject();value.put("sourceId",entry.sourceId);value.put("name",channel.name);value.put("url",channel.url);value.put("group",channel.group);value.put("kind",channel.kind.name());value.put("seriesId",channel.seriesId==null?JSONObject.NULL:channel.seriesId);value.put("logoUrl",channel.logoUrl);value.put("epgId",channel.epgId);value.put("epgName",channel.epgName);savedFavorites.put(value);
  }
  root.put("favorites",savedFavorites);JSONArray radios=new JSONArray();for(RadioStation station:radioFavorites) radios.put(station.toJson());root.put("radioFavorites",radios);return root.toString();
 }
 public static Library fromJson(String json) throws JSONException {
  Library library=new Library();JSONObject root=new JSONObject(json);
  if(root.optInt("version")!=1) throw new JSONException("Unsupported library format");
  library.favoritesOnStart=root.optBoolean("favoritesOnStart");library.lastSourceId=root.optString("lastSourceId","");
  library.autoPlayLast=root.optBoolean("autoPlayLast");library.miniPlayer=root.optBoolean("miniPlayer");library.lastPlayedPosition=Math.max(0,root.optLong("lastPlayedPosition"));
  JSONObject last=root.optJSONObject("lastPlayed");
  if(last!=null) {
   try {library.lastPlayed=new Playlist.Channel(last.getString("name"),last.getString("url"),last.optString("group",""),Playlist.Kind.valueOf(last.getString("kind")),null,last.optString("logoUrl",""),last.optString("epgId",""),last.optString("epgName",""));}
   catch(IllegalArgumentException e) {throw new JSONException("Unsupported playback type");}
  }
  JSONArray sources=root.getJSONArray("sources");
  for(int i=0;i<sources.length();i++) {
   JSONObject value=sources.getJSONObject(i);
   library.sources.add(new Source(value.getString("id"),value.getString("name"),value.getString("server"),value.optString("username",""),value.optString("password",""),value.getBoolean("xtream"),value.optString("epgId","")));
  }
  JSONArray guides=root.optJSONArray("guides");
  if(guides!=null) for(int i=0;i<guides.length();i++) {JSONObject value=guides.getJSONObject(i);library.guides.add(new Guide(value.getString("id"),value.getString("name"),value.getString("url")));}
  JSONArray favorites=root.getJSONArray("favorites");
  for(int i=0;i<favorites.length();i++) {
   JSONObject value=favorites.getJSONObject(i);
   if(library.findSource(value.getString("sourceId"))==null) continue;
   Playlist.Kind kind;
   try { kind=Playlist.Kind.valueOf(value.getString("kind")); } catch(IllegalArgumentException e) { throw new JSONException("Unsupported content type"); }
   Playlist.Channel channel=new Playlist.Channel(value.getString("name"),value.getString("url"),value.optString("group",""),kind,value.isNull("seriesId")?null:value.getString("seriesId"),value.optString("logoUrl",""),value.optString("epgId",""),value.optString("epgName",""));
   library.favorites.add(new Entry(value.getString("sourceId"),channel));
  }
  JSONArray radios=root.optJSONArray("radioFavorites");if(radios!=null) for(int i=0;i<radios.length();i++) library.addRadio(RadioStation.fromJson(radios.getJSONObject(i)));
  return library;
 }
}
