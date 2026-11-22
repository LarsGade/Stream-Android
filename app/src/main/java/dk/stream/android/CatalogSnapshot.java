package dk.stream.android;
import org.json.*;

/** Lossless catalog snapshot, including empty provider groups. */
final class CatalogSnapshot {
 static String encode(Library.Source source,Catalog catalog)throws Exception{
  Library saved=new Library();saved.sources.add(source);for(Playlist.Channel channel:catalog.items)saved.favorites.add(new Library.Entry(source.id,channel));
  JSONObject root=new JSONObject(saved.toJson()),groups=new JSONObject();for(Playlist.Kind kind:Playlist.Kind.values())groups.put(kind.name(),new JSONArray(catalog.groups(kind)));root.put("catalogGroups",groups);return root.toString();
 }
 static Catalog decode(Library.Source source,String json)throws Exception{
  Library saved=Library.fromJson(json);if(saved.sources.size()!=1||!CombinedCatalog.sameConnection(source,saved.sources.get(0)))throw new IllegalArgumentException("Different source");
  Catalog result=new Catalog();for(Library.Entry entry:saved.favorites)result.add(entry.channel);JSONObject groups=new JSONObject(json).getJSONObject("catalogGroups");for(Playlist.Kind kind:Playlist.Kind.values()){JSONArray values=groups.getJSONArray(kind.name());for(int i=0;i<values.length();i++)result.addGroup(kind,values.getString(i));}return result;
 }
 private CatalogSnapshot(){}
}
