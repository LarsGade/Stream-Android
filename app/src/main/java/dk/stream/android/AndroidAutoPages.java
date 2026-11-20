package dk.stream.android;

import java.util.*;

/** Stable browse IDs are stored independently of the phone's start tab. */
final class AndroidAutoPages {
 static final String START="radio-start",ROOT="radio-root",FAVORITES="radio-favorites";
 static final String[] IDS={ROOT,FAVORITES,"radio-danish","radio-england","radio-usa"};
 static final String[] LABELS={"Forside","Radiofavoritter","Danmark","England","USA"};
 static String normalize(String id){return Arrays.asList(IDS).contains(id)?id:FAVORITES;}
 static int index(String id){return Arrays.asList(IDS).indexOf(normalize(id));}
 static String label(String id){return START.equals(id)?"Stream":LABELS[index(id)];}
 static boolean folder(String id){return START.equals(id)||Arrays.asList(IDS).contains(id);}
 static String effective(String id,boolean hasFavorites){String page=normalize(id);return FAVORITES.equals(page)&&!hasFavorites?ROOT:page;}
 /** The all-pages link is part of pagination and appears once, after the selected page. */
 static <T> List<T> startItems(String page,List<T> contents,T allPages){List<T> result=new ArrayList<>(contents);if(!ROOT.equals(page))result.add(allPages);return result;}
 private AndroidAutoPages(){}
}
