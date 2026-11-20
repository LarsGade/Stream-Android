package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class CarAudioCatalogTest {
 private LocalMedia file(String name,String mime){return new LocalMedia("content://files/"+name.replace(" ","%20"),name,mime,false);}
 @Test public void acceptsAudioFilesAndAudiobooksRegardlessOfExtension(){for(String name:new String[]{"episode.mp3","book.m4b","track.flac","episode.opus","sound.wav","audio.aac","album.oga","surround.ac3"})assertTrue(name,CarAudioCatalog.accepts(file(name,"application/octet-stream")));assertTrue(CarAudioCatalog.accepts(file("opaque","audio/mp4")));}
 @Test public void excludesVideoImagesAndArchives(){assertFalse(CarAudioCatalog.accepts(file("movie.mp4","video/mp4")));assertFalse(CarAudioCatalog.accepts(file("photo.jpg","image/jpeg")));assertFalse(CarAudioCatalog.accepts(file("files.zip","application/zip")));}
 @Test public void foldersRemainBrowsable(){assertTrue(CarAudioCatalog.accepts(new LocalMedia("content://files/folder","Episodes","vnd.android.document/directory",true)));}
 @Test public void carIdsStayStableWhenFilesAreRenamed(){LocalMedia first=file("episode.mp3","audio/mpeg");assertEquals(CarAudioCatalog.id(first),CarAudioCatalog.id(new LocalMedia(first.uri,"New name","audio/mpeg",false)));assertNotEquals(CarAudioCatalog.id(first),CarAudioCatalog.id(file("other.mp3","audio/mpeg")));}
 @Test public void nestedEpisodesInheritProgrammeLogoAndKeepReadableLeafTitle(){LocalMedia folder=new LocalMedia("content://files/show","Anders og Anders","",true);LocalMedia child=CarAudioCatalog.child(folder,file("Episode 1.mp3","audio/mpeg"));assertEquals("Episode 1",child.displayFileName());assertTrue(LocalFileArtwork.asset(child).endsWith("anders.png"));}
 @Test public void radioStartWorksWithAudioFavoritesAsWellAsStations(){Library library=new Library();assertFalse(CarAudioCatalog.hasFavorites(library,"Radio"));FavoriteLists.assign(library,FavoriteLists.of(file("episode.mp3","audio/mpeg")),"Radio");assertTrue(CarAudioCatalog.hasFavorites(library,"Radio"));assertFalse(CarAudioCatalog.hasFavorites(library,"Podcast"));}
}
