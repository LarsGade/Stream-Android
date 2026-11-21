package dk.stream.android;
import java.util.*;
final class AndroidAutoPages {
 static final String START="radio-start",ROOT="radio-root",FAVORITES="radio-favorites",PODCAST="car-podcast",MUSIC="car-music",LOCAL="car-local",CLOUD="car-cloud",COUNTRIES="radio-countries";
 static final String[] IDS={ROOT,FAVORITES,MUSIC,LOCAL,CLOUD};static final String[] LABELS={"Forside","Radio","Musik","Lokale filer","Cloud"};
 static String normalize(String id){if(PODCAST.equals(id))return LOCAL;return Arrays.asList(IDS).contains(id)?id:FAVORITES;}
 static int index(String id){return Arrays.asList(IDS).indexOf(normalize(id));}
 static String view(String page,boolean favorites){String id=normalize(page);return ROOT.equals(id)?ROOT:favorites?id:FAVORITES.equals(id)?COUNTRIES:id+"-all";}
 static String label(String id){if(START.equals(id))return "Stream";if(ROOT.equals(id))return "Alle sider";if(COUNTRIES.equals(id))return "Alle radiostationer";if("radio-danish".equals(id))return "Danmark";if("radio-england".equals(id))return "England";if("radio-usa".equals(id))return "USA";return LABELS[index(id!=null&&id.endsWith("-all")?id.substring(0,id.length()-4):id)];}
 static boolean folder(String id){return START.equals(id)||ROOT.equals(id)||PODCAST.equals(id)||Arrays.asList(IDS).contains(id)||Arrays.asList(MUSIC+"-all",LOCAL+"-all",CLOUD+"-all",COUNTRIES,"radio-danish","radio-england","radio-usa").contains(id)||id!=null&&id.startsWith("car-folder:");}
 static String effective(String id,boolean hasFavorites){return normalize(id);}
 static <T> List<T> startItems(String page,List<T> contents,T allPages){List<T> result=new ArrayList<>(contents);if(!ROOT.equals(page))result.add(allPages);return result;}
 private AndroidAutoPages(){}
}
