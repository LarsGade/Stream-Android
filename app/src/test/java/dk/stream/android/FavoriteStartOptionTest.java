package dk.stream.android;
import java.io.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class FavoriteStartOptionTest {
 @Test public void allEightOptionsSurviveBackup()throws Exception{for(int option=0;option<8;option++){Library library=new Library();library.setFavoritesStartOption(option);Library restored=SettingsBackup.decode(new ByteArrayInputStream(SettingsBackup.encode(library)));assertEquals(option,restored.favoritesStartOption());if(option>=1&&option<=3){assertEquals(0,restored.favoritesStartTab);assertEquals(option-1,restored.favoritesStartKind);}}}
 @Test public void existingStreamingChoiceKeepsItsKind(){Library library=new Library();library.setFavoritesStartTab(0);library.favoritesStartKind=2;assertEquals(3,library.favoritesStartOption());library.favoritesStartKind=1;assertEquals(2,library.favoritesStartOption());}
 @Test public void radioLocalFilesAndMusicMapToExistingTabs(){Library library=new Library();for(int tab=1;tab<=3;tab++){library.setFavoritesStartOption(tab+3);assertEquals(tab,library.favoritesStartTab);}}
}
