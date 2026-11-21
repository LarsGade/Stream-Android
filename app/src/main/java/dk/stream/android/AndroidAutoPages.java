package dk.stream.android;
import java.util.*;
final class AndroidAutoPages {
 static final String START="radio-start",ROOT="radio-root",FAVORITES="radio-favorites",PODCAST="car-podcast",MUSIC="car-music",COUNTRIES="radio-countries";
 static final String[] IDS={ROOT,FAVORITES,PODCAST,MUSIC};static final String[] LABELS={"Forside","Radio","Podcast","Musik"};
 static String normalize(String id){if(Arrays.asList(IDS).contains(id))return id;return FAVORITES;}
 static int index(String id){return Arrays.asList(IDS).indexOf(normalize(id));}
 static String label(String id){if(START.equals(id))return "Stream";if(MUSIC.equals(id))return "Musik";if(COUNTRIES.equals(id))return "Alle radiostationer";if("radio-danish".equals(id))return "Danmark";if("radio-england".equals(id))return "England";if("radio-usa".equals(id))return "USA";return LABELS[index(id)];}
 static boolean folder(String id){return START.equals(id)||Arrays.asList(IDS).contains(id)||Arrays.asList(MUSIC,COUNTRIES,"radio-danish","radio-england","radio-usa").contains(id)||id!=null&&id.startsWith("car-folder:");}
 static String effective(String id,boolean hasFavorites){String page=normalize(id);return FAVORITES.equals(page)&&!hasFavorites?ROOT:page;}
 static <T> List<T> startItems(String page,List<T> contents,T allPages){List<T> result=new ArrayList<>(contents);if(!ROOT.equals(page))result.add(allPages);return result;}
 private AndroidAutoPages(){}
}
