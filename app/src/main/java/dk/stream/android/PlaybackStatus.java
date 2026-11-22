package dk.stream.android;
/** Only active playback has a marker; history remains visible in the row progress bar. */
final class PlaybackStatus {
 static int color(boolean playing,PlaybackMark mark){return playing?0xFFFFFFFF:0;}
 private PlaybackStatus(){}
}
