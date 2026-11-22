package dk.stream.android;

import java.util.*;

/** Unified groups and lists; every row retains its owning source and credentials. */
public final class CombinedCatalog {
 public final Catalog catalog=new Catalog();
 private final List<Library.Source> sources=new ArrayList<>();private final List<Catalog> catalogs=new ArrayList<>();
 public CombinedCatalog(List<Library.Source> ordered,Map<String,Catalog> loaded){
  for(Library.Source source:ordered){Catalog data=loaded.get(source.id);if(data==null)continue;sources.add(source);catalogs.add(data);for(Playlist.Kind kind:Playlist.Kind.values())for(String group:data.groups(kind))catalog.addGroup(kind,group);for(Playlist.Channel channel:data.items)catalog.add(channel);}
 }
 public List<Library.Entry> filter(Playlist.Kind kind,String group,String query){List<Library.Entry> result=new ArrayList<>();for(int i=0;i<sources.size();i++)for(Playlist.Channel channel:catalogs.get(i).filter(kind,group,query))result.add(new Library.Entry(sources.get(i).id,channel));return result;}
 public static boolean sameConnection(Library.Source a,Library.Source b){return a!=null&&b!=null&&a.xtream==b.xtream&&a.server.equals(b.server)&&a.username.equals(b.username)&&a.password.equals(b.password);}
}
