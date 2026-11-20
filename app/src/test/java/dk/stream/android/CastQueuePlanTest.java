package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class CastQueuePlanTest {
 @Test public void startsAtSelectedEpisodeInLargeFolder(){CastQueuePlan plan=new CastQueuePlan(5000,2345);assertEquals(2345,plan.from+plan.start);assertTrue(plan.to-plan.from<=12);assertTrue(plan.start>=0&&plan.start<plan.to-plan.from);}
 @Test public void appendsEveryRemainingEpisodeOnceInBoundedMessages(){CastQueuePlan plan=new CastQueuePlan(5000,0);int covered=plan.to;while(plan.hasNext()){int from=plan.nextFrom(),to=plan.nextTo();assertEquals(covered,from);assertTrue(to-from>0&&to-from<=12);plan.accepted(from,to);covered=to;}assertEquals(5000,covered);}
 @Test public void worksAtEndOfFolder(){CastQueuePlan plan=new CastQueuePlan(5000,4999);assertEquals(4999,plan.from+plan.start);assertEquals(5000,plan.to);assertFalse(plan.hasNext());}
 @Test public void smallFolderStaysIntact(){CastQueuePlan plan=new CastQueuePlan(3,1);assertEquals(0,plan.from);assertEquals(3,plan.to);assertEquals(1,plan.start);assertFalse(plan.hasNext());}
 @Test public void failedAppendCanBeRetriedWithoutSkippingFiles(){CastQueuePlan plan=new CastQueuePlan(50,0);int from=plan.nextFrom(),to=plan.nextTo();assertEquals(from,plan.nextFrom());plan.accepted(from,to);assertEquals(to,plan.nextFrom());}
 @Test(expected=IllegalArgumentException.class)public void rejectsStaleAppendAcknowledgement(){CastQueuePlan plan=new CastQueuePlan(100,0);int from=plan.nextFrom(),to=plan.nextTo();plan.accepted(from,to);plan.accepted(from,to);}
}
