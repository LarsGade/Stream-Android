package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class DownloadEligibilityTest {
 @Test public void liveTvAndRadioAreNeverOfferedForDownload(){assertFalse(MediaDownloads.eligible(FavoriteLists.of(new RadioStation("one","Radio","https://example.com/radio",""))));assertFalse(MediaDownloads.eligible(FavoriteLists.of(new Library.Entry("source",new Playlist.Channel("TV","https://example.com/live","",Playlist.Kind.TV,null)))));}
 @Test public void filesImagesAndFoldersCanBeDownloaded(){for(LocalMedia file:new LocalMedia[]{new LocalMedia("content://files/one","Song.mp3","audio/mpeg",false),new LocalMedia("content://files/two","Photo.jpg","image/jpeg",false),new LocalMedia("content://files/root","Folder","",true)})assertTrue(MediaDownloads.eligible(FavoriteLists.of(file)));}
 @Test public void moviesEpisodesAndWholeSeriesAreOffered(){for(Playlist.Channel channel:new Playlist.Channel[]{new Playlist.Channel("Movie","https://example.com/movie.mp4","",Playlist.Kind.FILM,null),new Playlist.Channel("Episode","https://example.com/episode.mp4","",Playlist.Kind.SERIES,null),new Playlist.Channel("Series","","",Playlist.Kind.SERIES,"42")})assertTrue(MediaDownloads.eligible(FavoriteLists.of(new Library.Entry("source",channel))));}
}
