package dk.stream.android;
/** Shared marker colors: active playback takes precedence over saved progress. */
final class PlaybackStatus {
 static int color(boolean playing,PlaybackMark mark){return playing?0xFFFFFFFF:mark.duration>0&&mark.complete?0xFF4CAF50:mark.duration>0&&mark.position>0?0xFFFFD600:0;}
 private PlaybackStatus(){}
}
