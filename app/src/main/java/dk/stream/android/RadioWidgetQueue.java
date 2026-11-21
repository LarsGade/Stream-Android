package dk.stream.android;
import java.util.*;
import org.json.*;
/** Each widget has its own ordered subset of radio favorites and a stable selected station ID. */
final class RadioWidgetQueue {
 static List<String> read(String json)throws JSONException{JSONArray values=new JSONArray(json);List<String> ids=new ArrayList<>();Set<String> seen=new HashSet<>();for(int i=0;i<values.length();i++){String id=values.getString(i);if(id.trim().isEmpty()||!seen.add(id))throw new JSONException("Invalid widget station selection");ids.add(id);}if(ids.isEmpty())throw new JSONException("Empty widget station selection");return ids;}
 static String json(List<String> ids)throws JSONException{String json=new JSONArray(ids).toString();read(json);return json;}
 static List<RadioStation> available(List<RadioStation> favorites,List<String> ids){Map<String,RadioStation> known=new HashMap<>();for(RadioStation station:favorites)known.put(station.id,station);List<RadioStation> result=new ArrayList<>();for(String id:ids){RadioStation station=known.get(id);if(station!=null)result.add(station);}return result;}
 static RadioStation current(List<RadioStation> stations,String id){for(RadioStation station:stations)if(station.id.equals(id))return station;return stations.isEmpty()?null:stations.get(0);}
 static RadioStation adjacent(List<RadioStation> stations,String id,int direction){if(direction!=1&&direction!=-1)throw new IllegalArgumentException("Invalid direction");RadioStation current=current(stations,id);if(current==null)return null;int index=stations.indexOf(current);return stations.get((index+direction+stations.size())%stations.size());}
 private RadioWidgetQueue(){}
}
