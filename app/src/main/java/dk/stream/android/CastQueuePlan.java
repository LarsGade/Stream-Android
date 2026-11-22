package dk.stream.android;
/** Keep Cast messages small regardless of the number of episodes in a folder. */
final class CastQueuePlan {
 static final int BATCH=12;
 final int total,from,to,start;
 private int next;
 CastQueuePlan(int total,int selected){if(total<1||selected<0||selected>=total)throw new IllegalArgumentException();this.total=total;from=Math.max(0,selected-3);to=Math.min(total,from+BATCH);start=selected-from;next=to;}
 int nextFrom(){return next;}int nextTo(){return Math.min(total,next+BATCH);}boolean hasNext(){return next<total;}
 void accepted(int from,int to){if(from!=next||to!=Math.min(total,next+BATCH))throw new IllegalArgumentException();next=to;}
}
