package dk.stream.android;
/** One playback owner, regardless of whether it is radio, a podcast, music or video. */
final class BluetoothResumeState<T> {
 private T owner,pending;private boolean eligible;
 void started(T value){owner=value;pending=null;eligible=true;}
 void interrupted(T value){if(owner==value&&eligible)pending=value;}
 void paused(T value){if(pending==value)pending=null;if(owner==value)eligible=false;}
 boolean eligible(){return owner!=null&&eligible;}
 void cancelPending(){pending=null;}
 void stopped(T value){if(owner==value){owner=null;eligible=false;}if(pending==value)pending=null;}
 T owner(){return owner;}
 T pending(){return pending;}
 T take(){T value=pending;pending=null;return value;}
 void clear(){owner=null;pending=null;eligible=false;}
}
