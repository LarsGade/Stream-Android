package dk.stream.android;
import java.util.*;
import java.text.Normalizer;
final class TvFavorites {
 static int mask(Library library,Library.Entry entry){FavoriteLists.Item item=FavoriteLists.of(entry);Integer saved=library.tvFavoriteLists.get(item.key());if(saved!=null)return saved;return library.isFavorite(entry)&&"Streaming".equals((library.favoriteAssignments.containsKey(item.key())?library.favoriteAssignments.get(item.key()):item.defaultList()))&&item.kind(library)==Playlist.Kind.TV?1:0;}
 static List<Library.Entry> list(Library library,int index){List<Library.Entry> result=new ArrayList<>();for(Library.Entry entry:library.favorites)if(entry.channel.kind==Playlist.Kind.TV&&(mask(library,entry)&(1<<index))!=0)result.add(entry);if(!library.favoriteOrder.isEmpty())Collections.sort(result,(a,b)->Integer.compare(order(library,a),order(library,b)));return result;}
 private static int order(Library library,Library.Entry entry){int index=library.favoriteOrder.indexOf(FavoriteLists.of(entry).key());return index<0?Integer.MAX_VALUE:index;}
 static void set(Library library,Library.Entry entry,int mask){if(mask<0||mask>15||entry.channel.kind!=Playlist.Kind.TV)throw new IllegalArgumentException();FavoriteLists.Item item=FavoriteLists.of(entry);if(mask==0){FavoriteLists.remove(library,item);return;}FavoriteLists.assignStreaming(library,item,0);library.tvFavoriteLists.put(item.key(),mask);}
 static void add(Library library,Library.Entry entry,int index){set(library,entry,mask(library,entry)|(1<<index));}
 static void remove(Library library,Library.Entry entry,int index){set(library,entry,mask(library,entry)&~(1<<index));}
 static String name(String name){return Normalizer.normalize(name,Normalizer.Form.NFKC).trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);}
 static final class Match {final Library.Entry entry;final int index;Match(Library.Entry entry,int index){this.entry=entry;this.index=index;}}
 static Match next(Library library,Library.Entry anchor,int current,String activeUrl){for(int step=1;step<=4;step++){int index=(current+step)%4;for(Library.Entry entry:list(library,index))if(name(entry.channel.name).equals(name(anchor.channel.name))&&!entry.channel.url.equals(activeUrl))return new Match(entry,index);}return null;}
 static Library.Entry primary(Library library,String url){for(Library.Entry entry:list(library,0))if(entry.channel.url.equals(url))return entry;return null;}
 static Library.Entry primary(Library library,Playlist.Channel channel){Library.Entry exact=primary(library,channel.url);if(exact!=null)return exact;if(channel.kind==Playlist.Kind.TV)for(Library.Entry entry:list(library,0))if(name(entry.channel.name).equals(name(channel.name)))return entry;return null;}
 static void addGroup(Library library,List<Library.Entry> group,int index){List<String> order=new ArrayList<>();Set<String> keys=new LinkedHashSet<>();for(Library.Entry entry:group)keys.add(FavoriteLists.of(entry).key());for(Library.Entry entry:list(library,index)){String key=FavoriteLists.of(entry).key();if(!keys.contains(key))order.add(key);}for(Library.Entry entry:group){add(library,entry,index);order.add(FavoriteLists.of(entry).key());}library.favoriteOrder.removeAll(order);library.favoriteOrder.addAll(new LinkedHashSet<>(order));}
 private TvFavorites(){}
}
