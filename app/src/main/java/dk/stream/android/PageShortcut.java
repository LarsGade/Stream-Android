package dk.stream.android;
import java.util.*;
/** Stable destination keys are independent of translations and later home visibility changes. */
final class PageShortcut {
 static final String EXTRA="openHomePage";
 static boolean valid(String page){return Arrays.asList(FavoriteHomeOrder.NAMES).contains(page);}
 static List<String> choices(Library library){List<String> result=new ArrayList<>();for(String page:library.favoriteHomeOrder)if(!library.hiddenHomeButtons.contains(page))result.add(page);return result;}
 private PageShortcut(){}
}
