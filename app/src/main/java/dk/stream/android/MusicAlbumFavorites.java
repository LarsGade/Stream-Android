package dk.stream.android;
import java.util.*;
/** Add a whole album once, keeping its track order and the order of other favorites. */
final class MusicAlbumFavorites {
 static void add(Library library,List<LocalMedia> tracks){List<String> order=new ArrayList<>();for(FavoriteLists.Item item:FavoriteLists.items(library,"Musik"))order.add(item.key());LinkedHashMap<String,LocalMedia> unique=new LinkedHashMap<>();for(LocalMedia track:tracks)if(!track.folder&&track.audio())unique.put(track.uri,track);List<String> album=new ArrayList<>();for(LocalMedia track:unique.values()){FavoriteLists.Item item=FavoriteLists.of(track);FavoriteLists.assign(library,item,"Musik");album.add(item.key());}order.removeAll(album);order.addAll(album);library.favoriteOrder.removeAll(order);library.favoriteOrder.addAll(order);}
 private MusicAlbumFavorites(){}
}
