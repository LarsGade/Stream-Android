package dk.stream.android;

/** A saved non-live position; completed media starts again at the beginning. */
final class PlaybackMark {
 final long position,duration;final boolean complete;
 PlaybackMark(long position,long duration,boolean complete){this.duration=Math.max(0,duration);this.position=Math.max(0,Math.min(position,this.duration));this.complete=complete||this.duration>0&&this.position>=this.duration;}
 long resume(){return complete?0:position;}
 int progress(){return duration<=0?0:complete?1000:(int)Math.min(1000,position*1000.0/duration);}
}
