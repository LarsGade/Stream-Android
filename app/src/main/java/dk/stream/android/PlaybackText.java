package dk.stream.android;
final class PlaybackText {
 static final String SOURCE="Afspilles via Stream";
 static androidx.media3.common.MediaMetadata.Builder metadata(String title){return new androidx.media3.common.MediaMetadata.Builder().setTitle(title).setDisplayTitle(title).setArtist(SOURCE).setSubtitle(SOURCE).setAlbumTitle(title).setAlbumArtist(title);}
 private PlaybackText(){}
}
