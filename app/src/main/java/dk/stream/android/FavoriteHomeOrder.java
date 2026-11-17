package dk.stream.android;
import java.util.*;
import org.json.*;
/** Home shortcuts are separate from the four favorite assignment categories. */
final class FavoriteHomeOrder {
 static final String[] NAMES={"TV","Film","Serier","Radio","Podcast","Musik"};
 static List<String> read(JSONArray saved)throws JSONException{
  List<String> names=new ArrayList<>();Set<String> seen=new HashSet<>();boolean legacy=saved.length()==FavoriteLists.NAMES.length;List<String> allowed=Arrays.asList(legacy?FavoriteLists.NAMES:NAMES);
  if(saved.length()!=allowed.size())throw new JSONException("Invalid favorite home order");
  for(int i=0;i<saved.length();i++){String name=saved.getString(i);if(!allowed.contains(name)||!seen.add(name))throw new JSONException("Invalid favorite home order");if(name.equals("Streaming"))names.addAll(Arrays.asList("TV","Film","Serier"));else names.add(name);}return names;
 }
 private FavoriteHomeOrder(){}
}
