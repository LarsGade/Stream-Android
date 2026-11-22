package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class RadioRecoveryStateTest {
 @Test public void streamFailureRetriesWithBackoffThenStops(){RadioRecoveryState state=new RadioRecoveryState();state.request(0);long now=0;for(int attempt=0;attempt<5;attempt++){state.failure(now);long delay=1000L<<attempt;assertFalse(state.retry(now+delay-1,0,false));assertTrue(state.retry(now+delay,0,false));now+=delay;}assertTrue(state.exhausted());state.failure(now);assertFalse(state.retry(now+100000,0,false));}
 @Test public void manualPauseOrFocusLossCancelsPendingRestart(){RadioRecoveryState state=new RadioRecoveryState();state.request(0);state.failure(1);state.pause();assertFalse(state.retry(100000,0,false));assertFalse(state.wanted());}
 @Test public void stalledRadioIsRecoveredButNormalProgressIsNot(){RadioRecoveryState state=new RadioRecoveryState();state.request(0);assertFalse(state.retry(0,0,false));assertFalse(state.retry(29999,0,false));assertFalse(state.retry(30000,0,false));assertTrue(state.retry(31000,0,false));state.request(0);for(int second=0;second<120;second++)assertFalse(state.retry(second*1000L,second*1000L,false));}
 @Test public void anotherAppHoldingAudioFocusDoesNotTriggerRecovery(){RadioRecoveryState state=new RadioRecoveryState();state.request(0);state.failure(0);assertFalse(state.retry(100000,0,true));state.pause();assertFalse(state.retry(200000,0,false));}
 @Test public void naturalStreamRecoveryCancelsScheduledRestart(){RadioRecoveryState state=new RadioRecoveryState();state.request(0);assertFalse(state.retry(0,0,false));state.failure(100);assertFalse(state.retry(500,1000,false));assertFalse(state.retry(1200,2000,false));}
 @Test public void explicitPlayCanTryAgainAfterRetryLimit(){RadioRecoveryState state=new RadioRecoveryState();state.request(0);long now=0;for(int attempt=0;attempt<5;attempt++){state.failure(now);now+=1000L<<attempt;assertTrue(state.retry(now,0,false));}state.request(now);assertFalse(state.exhausted());state.failure(now);assertTrue(state.retry(now+1000,0,false));}
 @Test public void seekingBackDoesNotLookLikeAStalledStream(){RadioRecoveryState state=new RadioRecoveryState();state.request(0);assertFalse(state.retry(0,100000,false));for(int second=1;second<=40;second++)assertFalse(state.retry(second*1000L,10000+second*1000L,false));}
}
