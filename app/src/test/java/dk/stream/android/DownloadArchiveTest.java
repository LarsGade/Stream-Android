package dk.stream.android;
import org.junit.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import static org.junit.Assert.*;
public class DownloadArchiveTest {
 @Test public void singleFileIsSavedWithoutZipWrapper()throws Exception{ByteArrayOutputStream output=new ByteArrayOutputStream();byte[] file={0,1,2,(byte)255};DownloadArchive.write(Arrays.asList("photo.png"),output,(index,destination)->destination.write(file));assertArrayEquals(file,output.toByteArray());}
 @Test public void multipleFilesProduceReadableZipWithFoldersAndSafeUniqueNames()throws Exception{ByteArrayOutputStream output=new ByteArrayOutputStream();List<String> names=Arrays.asList("Bæltestedet/episode.mp3","Bæltestedet/episode.mp3","../../Anders og Anders.mp3");DownloadArchive.write(names,output,(index,destination)->destination.write(("file-"+index).getBytes(StandardCharsets.UTF_8)));try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(output.toByteArray()))){String[] expected={"Bæltestedet/episode.mp3","Bæltestedet/episode (2).mp3","Anders og Anders.mp3"};for(int i=0;i<3;i++){ZipEntry entry=zip.getNextEntry();assertNotNull(entry);assertEquals(expected[i],entry.getName());assertEquals("file-"+i,new String(zip.readAllBytes(),StandardCharsets.UTF_8));}assertNull(zip.getNextEntry());}}
 @Test(expected=IOException.class)public void inputFailuresAreReportedInsteadOfCompletingBrokenArchive()throws Exception{DownloadArchive.write(Arrays.asList("one.mp3","two.mp3"),new ByteArrayOutputStream(),(index,destination)->{if(index==1)throw new IOException("network interrupted");destination.write(1);});}
 @Test(expected=IOException.class)public void emptyFolderDoesNotCreateFakeDownload()throws Exception{DownloadArchive.write(Collections.emptyList(),new ByteArrayOutputStream(),(index,destination)->fail());}
}
