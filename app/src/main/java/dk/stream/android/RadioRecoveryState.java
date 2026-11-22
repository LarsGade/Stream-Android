package dk.stream.android;
/** Bounded recovery for live radio; manual pause/focus loss always wins. */
final class RadioRecoveryState {
 private static final int MAX_RETRIES=5;
 private boolean wanted;private int attempts;private long due=-1,lastProgress,lastPosition=-1,healthySince=-1;
 void request(long now){wanted=true;attempts=0;due=-1;resetProgress(now);}
 void pause(){wanted=false;due=-1;}
 boolean wanted(){return wanted;}
 boolean exhausted(){return attempts>=MAX_RETRIES;}
 void failure(long now){if(wanted&&!exhausted()&&due<0)due=now+Math.min(30000,1000L<<attempts);}
 void resetProgress(long now){lastProgress=now;lastPosition=-1;healthySince=-1;}
 boolean retry(long now,long position,boolean suppressed){if(!wanted)return false;if(suppressed){resetProgress(now);return false;}if(lastPosition<0){lastPosition=position;lastProgress=now;}else if(Math.abs(position-lastPosition)>250){lastPosition=position;lastProgress=now;due=-1;if(healthySince<0)healthySince=now;if(now-healthySince>=60000)attempts=0;}if(due<0&&now-lastProgress>=30000)failure(now);if(due>=0&&now>=due&&!exhausted()){attempts++;due=-1;resetProgress(now);return true;}return false;}
}
