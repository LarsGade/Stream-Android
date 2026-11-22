package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class RadioFavoriteNavigationTest {
 private Library library(){Library library=new Library();for(String id:new String[]{"one","two","three"})library.addRadio(new RadioStation(id,id,"https://example.com/"+id,""));return library;}
 @Test public void skipsBothDirectionsAndWraps(){Library library=library();assertEquals("two",RadioFavoriteNavigation.adjacent(library,"one",1).id);assertEquals("three",RadioFavoriteNavigation.adjacent(library,"one",-1).id);assertEquals("one",RadioFavoriteNavigation.adjacent(library,"three",1).id);}
 @Test public void unknownChannelStartsAtTheRelevantEnd(){Library library=library();assertEquals("one",RadioFavoriteNavigation.adjacent(library,"unknown",1).id);assertEquals("three",RadioFavoriteNavigation.adjacent(library,"unknown",-1).id);}
 @Test public void respectsFavoriteOrderAndCategory(){Library library=library();FavoriteLists.Item second=FavoriteLists.of(library.findRadio("two"));FavoriteLists.move(library,"Radio",second,-1);assertEquals("two",RadioFavoriteNavigation.adjacent(library,"unknown",1).id);FavoriteLists.assignHome(library,second,"Podcast");assertEquals("one",RadioFavoriteNavigation.adjacent(library,"unknown",1).id);}
 @Test public void emptyAndSingleStationAreSafe(){Library library=new Library();assertNull(RadioFavoriteNavigation.adjacent(library,"",1));library.addRadio(new RadioStation("one","One","https://example.com/radio",""));assertNull(RadioFavoriteNavigation.adjacent(library,"one",1));assertEquals("one",RadioFavoriteNavigation.adjacent(library,"",1).id);}
}
