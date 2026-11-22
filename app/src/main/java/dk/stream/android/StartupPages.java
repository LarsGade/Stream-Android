package dk.stream.android;
import java.util.*;
/** Startup destinations follow the visible home buttons, including their order. */
final class StartupPages {
 static final String HOME="Forside";
 private static final List<String> LEGACY_OPTIONS=Arrays.asList(HOME,"TV","Film","Serier","Radio","Lokale filer","Musik","Cloud");
 static List<String> choices(Library library){List<String> result=new ArrayList<>();result.add(HOME);for(String name:library.favoriteHomeOrder)if(!library.hiddenHomeButtons.contains(name))result.add(name);return result;}
 static String selected(Library library){String name=library.favoritesOnStart?(!library.startupDestination.isEmpty()?library.startupDestination:LEGACY_OPTIONS.get(library.favoritesStartOption())):HOME;return choices(library).contains(name)?name:HOME;}
 static void select(Library library,String name){if(!choices(library).contains(name))throw new IllegalArgumentException("Unknown startup page");library.startupDestination=name;if(LEGACY_OPTIONS.contains(name))library.setFavoritesStartOption(LEGACY_OPTIONS.indexOf(name));library.favoritesOnStart=true;}
 static boolean favorites(Library library){return selected(library).equals("Podcast")|| (library.startupFavorites!=null?library.startupFavorites:Boolean.TRUE.equals(library.pageFavorites.get(selected(library))));}
 private StartupPages(){}
}
