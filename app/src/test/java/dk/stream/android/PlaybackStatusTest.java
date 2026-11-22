package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaybackStatusTest {
 @Test public void untouchedMediaHasNoMarker(){assertEquals(0,PlaybackStatus.color(false,new PlaybackMark(0,100,false)));assertEquals(0,PlaybackStatus.color(false,new PlaybackMark(0,0,false)));}
 @Test public void partialMediaHasYellowMarker(){assertEquals(0xFFFFD600,PlaybackStatus.color(false,new PlaybackMark(1,100000,false)));}
 @Test public void completedMediaHasGreenMarker(){assertEquals(0xFF4CAF50,PlaybackStatus.color(false,new PlaybackMark(100,100,false)));assertEquals(0xFF4CAF50,PlaybackStatus.color(false,new PlaybackMark(90,100,true)));}
 @Test public void activePlaybackKeepsWhiteMarker(){assertEquals(0xFFFFFFFF,PlaybackStatus.color(true,new PlaybackMark(20,100,false)));assertEquals(0xFFFFFFFF,PlaybackStatus.color(true,new PlaybackMark(100,100,true)));}
 @Test public void musicSourceNeverGetsHistoryMarkers(){assertFalse(PlaybackStatus.historyAllowed("content://dk.stream.android.music/song/jiosaavn_song",true));}
 @Test public void localCloudAndOnDemandVideoKeepHistoryMarkers(){assertTrue(PlaybackStatus.historyAllowed("content://documents/audio.mp3",true));assertTrue(PlaybackStatus.historyAllowed("content://dk.stream.android.pcloud/account?url=audio.mp3",true));assertTrue(PlaybackStatus.historyAllowed("https://example.com/movie.mp4",true));assertFalse(PlaybackStatus.historyAllowed("https://example.com/live.m3u8",false));}
}
