package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class GuideRecoveryTest {
 @Test public void missingRowsForSameGuideStartOnlyOneRetry(){GuideRecovery recovery=new GuideRecovery();assertTrue(recovery.request("guide",1000,false));for(int row=0;row<100;row++)assertFalse(recovery.request("guide",1000+row,false));}
 @Test public void alreadyLoadingGuideIsNotQueuedAgain(){GuideRecovery recovery=new GuideRecovery();assertFalse(recovery.request("guide",1000,true));assertTrue(recovery.request("guide",1001,false));}
 @Test public void failedOrIncompleteGuideCanBeRetriedAfterInterval(){GuideRecovery recovery=new GuideRecovery();assertTrue(recovery.request("guide",1000,false));assertFalse(recovery.request("guide",1000+GuideRecovery.RETRY_MS-1,false));assertTrue(recovery.request("guide",1000+GuideRecovery.RETRY_MS,false));}
 @Test public void unrelatedGuidesLoadIndependently(){GuideRecovery recovery=new GuideRecovery();assertTrue(recovery.request("one",1000,false));assertTrue(recovery.request("two",1000,false));assertFalse(recovery.request("one",1001,false));}
}
