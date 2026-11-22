package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class DownloadProgressTest {
 @Test public void reportsSingleFilePercentage(){assertEquals(0,DownloadProgress.percent(0,1,0,1000));assertEquals(37,DownloadProgress.percent(0,1,375,1000));assertEquals(100,DownloadProgress.percent(1,1,0,-1));}
 @Test public void includesCurrentFileWithinArchiveProgress(){assertEquals(25,DownloadProgress.percent(1,4,0,100));assertEquals(37,DownloadProgress.percent(1,4,50,100));assertEquals(100,DownloadProgress.percent(4,4,0,-1));}
 @Test public void unknownLengthStillReportsCompletedFiles(){assertEquals(0,DownloadProgress.percent(0,1,500,-1));assertEquals(50,DownloadProgress.percent(1,2,500,-1));}
 @Test public void progressCannotOverflowPercentageRange(){assertEquals(100,DownloadProgress.percent(0,1,1000,10));assertEquals(0,DownloadProgress.percent(0,1,-1,10));assertEquals(100,DownloadProgress.percent(5,2,0,0));}
}
