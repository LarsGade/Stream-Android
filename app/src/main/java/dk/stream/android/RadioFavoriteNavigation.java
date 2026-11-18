package dk.stream.android;
import java.util.*;
final class RadioFavoriteNavigation {
 static List<RadioStation> stations(Library library){List<RadioStation> stations=new ArrayList<>();for(FavoriteLists.Item item:FavoriteLists.items(library,"Radio"))if(item.radio!=null)stations.add(item.radio);return stations;}
 static RadioStation adjacent(Library library,String currentId,int direction){if(direction!=-1&&direction!=1)throw new IllegalArgumentException("Invalid direction");List<RadioStation> stations=stations(library);if(stations.isEmpty())return null;int index=-1;for(int i=0;i<stations.size();i++)if(stations.get(i).id.equals(currentId))index=i;if(index<0)return stations.get(direction>0?0:stations.size()-1);if(stations.size()==1)return null;return stations.get((index+direction+stations.size())%stations.size());}
 private RadioFavoriteNavigation(){}
}
