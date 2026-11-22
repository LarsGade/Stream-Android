package dk.stream.android;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class PageShortcutTest {
 @Test public void selectionFollowsVisibleHomeOrderWithoutChangingTheLibrary(){Library library=new Library();library.moveFavoriteHome("Podcast",-1);library.hiddenHomeButtons.add("Film");List<String> before=new ArrayList<>(library.favoriteHomeOrder);assertEquals(Arrays.asList("TV","Serier","Radio","Podcast","Musik","Lokale filer","Cloud"),PageShortcut.choices(library));assertEquals(before,library.favoriteHomeOrder);}
 @Test public void alreadyConfiguredPageRemainsValidAfterHidingIt(){Library library=new Library();library.hiddenHomeButtons.add("Podcast");assertFalse(PageShortcut.choices(library).contains("Podcast"));assertTrue(PageShortcut.valid("Podcast"));}
 @Test public void invalidAndTranslatedKeysCannotLaunchAnArbitraryDestination(){assertFalse(PageShortcut.valid(null));assertFalse(PageShortcut.valid(""));assertFalse(PageShortcut.valid("Local files"));assertFalse(PageShortcut.valid("Indstillinger"));assertTrue(PageShortcut.valid("Lokale filer"));}
 @Test public void everyPageHasItsOwnHomeIcon(){Set<Integer> icons=new HashSet<>();for(String name:FavoriteHomeOrder.NAMES){assertTrue(PageShortcut.valid(name));assertTrue(icons.add(HomePageIcons.index(name)));}assertEquals(8,icons.size());}
}
