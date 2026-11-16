package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class LocalFileArtworkTest {
 private LocalMedia file(String name){return new LocalMedia("content://local/file",name,"audio/mpeg",false);}
 @Test public void mapsProgrammeNamesCaseInsensitively(){assertTrue(LocalFileArtwork.asset(file("ANDERS OG ANDERS - Afsnit 01.mp3")).endsWith("anders.png"));assertTrue(LocalFileArtwork.asset(file("Bæltestedet - Afsnit 02.mp3")).endsWith("balte.png"));assertTrue(LocalFileArtwork.asset(file("r8dio - afsnit.mp3")).endsWith("roder.png"));assertTrue(LocalFileArtwork.asset(file("Den Korte Radioavis.mp3")).endsWith("korte.png"));}
 @Test public void leavesOtherFilesAndImagesAlone(){assertNull(LocalFileArtwork.asset(file("Anden podcast.mp3")));assertNull(LocalFileArtwork.asset(new LocalMedia("content://local/photo","Anders og Anders.jpg","image/jpeg",false)));assertNull(LocalFileArtwork.asset(new LocalMedia("content://local/folder","Bæltestedet","",true)));}
 @Test public void pcloudUsesTheSameFourLogos(){String[] names={"Anders og Anders - Afsnit 1.mp3","Bæltestedet - Afsnit 2.mp3","Den Korte Radioavis.mp3","r8dio - afsnit.mp3"};for(String name:names){LocalMedia cloud=new LocalMedia("content://dk.stream.android.pcloud/account?url=https%3A%2F%2Fexample.com%2Ffile",name,"audio/mpeg",false);assertEquals(LocalFileArtwork.asset(file(name)),LocalFileArtwork.asset(cloud));}}

}
