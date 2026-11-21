package dk.stream.android;
/** Returning focus, rather than a chooser result alone, allows playback to resume safely. */
final class ExternalPlaybackReturn {
 boolean launched,left,finished,foreground;
 void paused(){foreground=false;}
 void covered(){if(launched)left=true;}
 boolean canResume(boolean focused){return launched&&left&&foreground&&focused&&!finished;}
 boolean finish(){if(finished)return false;finished=true;return true;}
}
