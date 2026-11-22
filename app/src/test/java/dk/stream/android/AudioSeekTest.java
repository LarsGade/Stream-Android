package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class AudioSeekTest {
 @Test public void skipsExactlyTenSecondsInBothDirections(){assertEquals(20000,AudioSeek.target(30000,60000,-1));assertEquals(40000,AudioSeek.target(30000,60000,1));}
 @Test public void stopsAtTrackBoundaries(){assertEquals(0,AudioSeek.target(1000,60000,-1));assertEquals(60000,AudioSeek.target(59000,60000,1));assertEquals(0,AudioSeek.target(0,0,1));}
 @Test public void clampsStalePositionsAndAvoidsOverflow(){assertEquals(60000,AudioSeek.target(70000,60000,1));assertEquals(0,AudioSeek.target(-1000,60000,-1));assertEquals(Long.MAX_VALUE,AudioSeek.target(Long.MAX_VALUE-5000,Long.MAX_VALUE,1));}
}
