package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaybackStatusTest {
 @Test public void untouchedMediaHasNoMarker(){assertEquals(0,PlaybackStatus.color(false,new PlaybackMark(0,100,false)));assertEquals(0,PlaybackStatus.color(false,new PlaybackMark(0,0,false)));}
 @Test public void partialMediaIsYellowEvenBelowOnePercent(){assertEquals(0xFFFFD600,PlaybackStatus.color(false,new PlaybackMark(1,100000,false)));}
 @Test public void completedMediaIsGreen(){assertEquals(0xFF4CAF50,PlaybackStatus.color(false,new PlaybackMark(100,100,false)));assertEquals(0xFF4CAF50,PlaybackStatus.color(false,new PlaybackMark(90,100,true)));}
 @Test public void activePlaybackKeepsWhiteMarker(){assertEquals(0xFFFFFFFF,PlaybackStatus.color(true,new PlaybackMark(20,100,false)));assertEquals(0xFFFFFFFF,PlaybackStatus.color(true,new PlaybackMark(100,100,true)));}
}
