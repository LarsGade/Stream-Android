package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class LocalFileArtworkTest {
 @Test public void programmeNamesNeverSelectManualArtwork(){for(String name:new String[]{"ANDERS OG ANDERS - Afsnit 01.mp3","Bæltestedet - Afsnit 02.mp3","baeltestedet.mp3","r8dio - afsnit.mp3","Den Korte Radioavis.mp3"}){assertNull(LocalFileArtwork.asset(new LocalMedia("content://local/file",name,"audio/mpeg",false)));assertNull(LocalFileArtwork.asset(new LocalMedia("content://dk.stream.android.pcloud/file",name,"audio/mpeg",false)));}}
 @Test public void imagesAndFoldersHaveNoProgrammeArtwork(){assertNull(LocalFileArtwork.asset(new LocalMedia("content://local/photo","Anders og Anders.jpg","image/jpeg",false)));assertNull(LocalFileArtwork.asset(new LocalMedia("content://local/folder","Bæltestedet","",true)));}
}
