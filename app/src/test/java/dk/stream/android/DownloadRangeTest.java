package dk.stream.android;
import org.junit.Test;
import java.io.*;
import static org.junit.Assert.*;
public class DownloadRangeTest {
 @Test public void validatesResumedRangeAndTotal()throws Exception{DownloadRange.validate("bytes 123-999/1000",123);assertEquals(1000,DownloadRange.total("bytes 123-999/1000",-1));}
 @Test(expected=IOException.class)public void rejectsUnexpectedOffset()throws Exception{DownloadRange.validate("bytes 0-999/1000",123);}
 @Test public void unsupportedRangeCanSkipPrefixWithoutLosingOrRepeatingBytes()throws Exception{InputStream source=new ByteArrayInputStream(new byte[]{0,1,2,3,4,5}){@Override public synchronized long skip(long count){return 0;}};DownloadRange.skip(source,3);assertArrayEquals(new byte[]{3,4,5},source.readAllBytes());}
 @Test(expected=EOFException.class)public void detectsTruncatedSourceDuringResume()throws Exception{DownloadRange.skip(new ByteArrayInputStream(new byte[]{1}),2);}
}
