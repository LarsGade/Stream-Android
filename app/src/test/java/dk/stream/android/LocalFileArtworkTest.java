package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class LocalFileArtworkTest {
 private LocalMedia file(String name){return new LocalMedia("content://local/file",name,"audio/mpeg",false);}
 @Test public void mapsProgrammeNamesCaseInsensitively(){assertTrue(LocalFileArtwork.asset(file("ANDERS OG ANDERS - Afsnit 01.mp3")).endsWith("anders.jpg"));assertTrue(LocalFileArtwork.asset(file("Bæltestedet - Afsnit 02.mp3")).endsWith("balte.jpg"));assertTrue(LocalFileArtwork.asset(file("r8dio - afsnit.mp3")).endsWith("roder.jpg"));assertTrue(LocalFileArtwork.asset(file("Den Korte Radioavis.mp3")).endsWith("korte.png"));}
 @Test public void leavesOtherFilesAndImagesAlone(){assertNull(LocalFileArtwork.asset(file("Anden podcast.mp3")));assertNull(LocalFileArtwork.asset(new LocalMedia("content://local/photo","Anders og Anders.jpg","image/jpeg",false)));assertNull(LocalFileArtwork.asset(new LocalMedia("content://local/folder","Bæltestedet","",true)));}
}
