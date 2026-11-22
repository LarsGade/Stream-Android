package dk.stream.android;
/** Active playback is white; saved partial and completed playback is yellow and green. */
final class PlaybackStatus {
 static int color(boolean playing,PlaybackMark mark){return playing?0xFFFFFFFF:mark.duration>0&&mark.complete?0xFF4CAF50:mark.duration>0&&mark.position>0?0xFFFFD600:0;}
 static boolean historyAllowed(String key,boolean eligible){return eligible&&!SaavnMusic.is(key);}
 private PlaybackStatus(){}
}
