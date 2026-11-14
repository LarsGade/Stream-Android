package dk.stream.android;

import java.io.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CarVideoTest {
 private Playlist.Channel channel(String name,String uri,Playlist.Kind kind,String series){return new Playlist.Channel(name,uri,"",kind,series);}
 @Test public void streamMimeIsDetectedWithoutChangingSignedLinks(){String link="https://example.com/live/channel.m3u8?token=a%2Bb&user=abc";CarVideo video=CarVideo.channel(channel("TV",link,Playlist.Kind.TV,null));assertEquals(link,video.uri);assertEquals("application/x-mpegurl",video.mime);}
 @Test public void favoritesKeepOrderAndSkipSeriesContainersAndDuplicates(){Library library=new Library();library.favorites.add(new Library.Entry("a",channel("TV","https://example.com/1.ts",Playlist.Kind.TV,null)));library.favorites.add(new Library.Entry("a",channel("Series","https://example.com/series",Playlist.Kind.SERIES,"1")));library.favorites.add(new Library.Entry("b",channel("Again","https://example.com/1.ts",Playlist.Kind.TV,null)));library.favorites.add(new Library.Entry("b",channel("Film","https://example.com/2.mp4",Playlist.Kind.FILM,null)));List<CarVideo> videos=CarVideo.favorites(library);assertEquals(2,videos.size());assertEquals("TV",videos.get(0).title);assertEquals("Film",videos.get(1).title);}
 @Test public void playlistRetainsDanishNamesAndDoesNotAllowTitleLineInjection() throws Exception {StringWriter writer=new StringWriter();CarVideo.writePlaylist(writer,Arrays.asList(new CarVideo("DR København\n#EXTM3U","https://example.com/1.ts","video/mp2t")));assertEquals("#EXTM3U\n#EXTINF:-1,DR København #EXTM3U\nhttps://example.com/1.ts\n",writer.toString());}
 @Test public void rawPathsAndNonMediaSchemesAreRejected(){for(String uri:Arrays.asList("file:///sdcard/movie.mp4","/sdcard/movie.mp4","javascript:alert(1)","https://example.com/a\n#EXTINF:1"))try{new CarVideo("Movie",uri,"video/mp4");fail();}catch(IllegalArgumentException expected){}}
 @Test public void localVideoUsesOriginalDocumentUriAndAReadableTitle(){LocalMedia local=new LocalMedia("content://docs/tree/root/document/root%2FMovie.mp4","Movie.mp4","video/mp4",false);CarVideo video=CarVideo.local(local);assertEquals(local.uri,video.uri);assertEquals("Movie",video.title);assertEquals("video/mp4",video.mime);}
 @Test public void foldersAndAudioCannotBeHandedOffAsVideos(){for(LocalMedia file:Arrays.asList(new LocalMedia("content://docs/folder","Folder","",true),new LocalMedia("content://docs/song","Song.mp3","audio/mpeg",false)))try{CarVideo.local(file);fail();}catch(IllegalArgumentException expected){}}
 @Test public void ungrantedDocumentLinksCannotLeakIntoNetworkPlaylist() throws Exception {StringWriter writer=new StringWriter();try{CarVideo.writePlaylist(writer,Collections.singletonList(new CarVideo("File","content://docs/movie","video/mp4")));fail();}catch(IllegalArgumentException expected){}assertEquals("",writer.toString());}
}
