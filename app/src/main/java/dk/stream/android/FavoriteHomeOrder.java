package dk.stream.android;
import java.util.*;
import org.json.*;
/** Home shortcuts migrate independently of saved favorite assignments. */
final class FavoriteHomeOrder {
 static final String[] NAMES={"TV","Film","Serier","Musik","Radio","Lokale filer","Cloud"};
 static List<String> read(JSONArray saved)throws JSONException{
  List<String> names=new ArrayList<>();Set<String> seen=new HashSet<>();
  List<String> allowed=saved.length()==4?Arrays.asList(FavoriteLists.NAMES):saved.length()==6?Arrays.asList("TV","Film","Serier","Radio","Podcast","Musik"):Arrays.asList(NAMES);
  if(saved.length()!=allowed.size())throw new JSONException("Invalid home order");
  for(int i=0;i<saved.length();i++){String name=saved.getString(i);if(!allowed.contains(name)||!seen.add(name))throw new JSONException("Invalid home order");if(name.equals("Streaming"))names.addAll(Arrays.asList("TV","Film","Serier"));else if(!name.equals("Podcast"))names.add(name);}
  if(names.equals(Arrays.asList("TV","Film","Serier","Radio","Lokale filer","Cloud","Musik")))return new ArrayList<>(Arrays.asList(NAMES));
  for(String name:NAMES)if(!names.contains(name))names.add(name);return names;
 }
 private FavoriteHomeOrder(){}
}
