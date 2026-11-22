package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.IOException;

public class TvRecordingPlaylistTest {
 @Test public void slidingLiveWindowKeepsAbsoluteSequenceNumbers()throws Exception{
  TvRecordingPlaylist first=TvRecordingPlaylist.parse("https://tv.example/live/index.m3u8","#EXTM3U\n#EXT-X-MEDIA-SEQUENCE:73\n#EXT-X-TARGETDURATION:6\n#EXTINF:6,\na.ts\n#EXTINF:6,\nb.ts");
  TvRecordingPlaylist next=TvRecordingPlaylist.parse("https://tv.example/live/index.m3u8","#EXTM3U\n#EXT-X-MEDIA-SEQUENCE:74\n#EXTINF:6,\nb.ts\n#EXTINF:6,\nc.ts");
  assertEquals(73,first.segments.get(0).sequence);assertEquals(74,next.segments.get(0).sequence);assertEquals(first.segments.get(1).url,next.segments.get(0).url);assertEquals(75,next.segments.get(1).sequence);assertEquals(6,first.targetSeconds);
 }
 @Test public void followsMasterAndRedirectRelativeUrls()throws Exception{
  TvRecordingPlaylist master=TvRecordingPlaylist.parse("https://tv.example/path/master.m3u8?token=secret","#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=1500000\n../hd/index.m3u8?token=x\n#EXT-X-STREAM-INF:BANDWIDTH=900000\nsd.m3u8");
  assertEquals("https://tv.example/hd/index.m3u8?token=x",master.variant);assertTrue(master.segments.isEmpty());
 }
 @Test public void encryptedSegmentsKeepKeyChangesAndExplicitIv()throws Exception{
  TvRecordingPlaylist list=TvRecordingPlaylist.parse("https://tv.example/live/list.m3u8","#EXTM3U\n#EXT-X-KEY:METHOD=AES-128,URI=\"keys/key,a\",IV=0x01\n001.ts\n#EXT-X-KEY:METHOD=NONE\n002.ts\n#EXT-X-ENDLIST");
  assertEquals("https://tv.example/live/keys/key,a",list.segments.get(0).key);assertEquals("0x01",list.segments.get(0).iv);assertNull(list.segments.get(1).key);assertTrue(list.ended);
 }
 @Test public void fragmentedMp4KeepsInitializationSegment()throws Exception{
  TvRecordingPlaylist list=TvRecordingPlaylist.parse("https://tv.example/list.m3u8","#EXTM3U\n#EXT-X-MAP:URI=\"init.mp4\"\npart.m4s");assertEquals("https://tv.example/init.mp4",list.segments.get(0).init);
 }
 @Test(expected=IOException.class) public void rejectsDrmRatherThanSavingUnplayableFile()throws Exception{TvRecordingPlaylist.parse("https://tv.example/list.m3u8","#EXTM3U\n#EXT-X-KEY:METHOD=SAMPLE-AES,URI=\"key\"\npart.ts");}
 @Test(expected=IOException.class) public void rejectsByteRangesInsteadOfDuplicatingWholeFiles()throws Exception{TvRecordingPlaylist.parse("https://tv.example/list.m3u8","#EXTM3U\n#EXT-X-BYTERANGE:500@20\nfile.ts");}
 @Test(expected=IOException.class) public void rejectsNonHttpSegmentAddresses()throws Exception{TvRecordingPlaylist.parse("https://tv.example/list.m3u8","#EXTM3U\nfile:///private/video.ts");}
 @Test public void aesIvUsesMediaSequenceWhenPlaylistDoesNotSupplyOne(){byte[] iv=TvRecordingPlaylist.iv(null,258);assertEquals(1,iv[14]);assertEquals(2,iv[15]);assertEquals(16,iv.length);}
 @Test public void explicitShortIvIsPaddedOnLeft(){byte[] iv=TvRecordingPlaylist.iv("0x0102",999);assertArrayEquals(TvRecordingPlaylist.iv(null,258),iv);}
 @Test(expected=IOException.class) public void refusesToSilentlyDropSeparateAudio()throws Exception{TvRecordingPlaylist.parse("https://tv.example/list.m3u8","#EXTM3U\n#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"audio\",URI=\"audio.m3u8\"\n#EXT-X-STREAM-INF:BANDWIDTH=1500000,AUDIO=\"audio\"\nvideo.m3u8");}
 @Test(expected=IOException.class) public void encryptedInitSegmentCannotBeSavedAsClearMp4()throws Exception{TvRecordingPlaylist.parse("https://tv.example/list.m3u8","#EXTM3U\n#EXT-X-KEY:METHOD=AES-128,URI=\"key\"\n#EXT-X-MAP:URI=\"init.mp4\"\npart.m4s");}
}
