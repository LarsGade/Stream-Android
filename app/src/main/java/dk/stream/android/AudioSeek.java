package dk.stream.android;
/** Relative seek positions stay within the current track. */
final class AudioSeek {
 static long target(long position,long duration,int direction){long current=Math.max(0,Math.min(position,Math.max(0,duration)));if(direction<0)return Math.max(0,current-10000);return current+Math.min(10000,Math.max(0,duration)-current);}
 private AudioSeek(){}
}
