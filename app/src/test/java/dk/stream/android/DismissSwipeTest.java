package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class DismissSwipeTest {
 @Test public void bothDirectionsDismiss(){assertTrue(DismissSwipe.dismiss(100,5,400,1));assertTrue(DismissSwipe.dismiss(-100,5,400,1));}
 @Test public void shortSwipeDoesNotStopPlayback(){assertFalse(DismissSwipe.dismiss(30,0,400,1));assertFalse(DismissSwipe.dismiss(0,0,400,1));}
 @Test public void verticalAndDiagonalGesturesDoNotDismiss(){assertFalse(DismissSwipe.dismiss(100,150,400,1));assertFalse(DismissSwipe.horizontal(10,10,5));}
 @Test public void thresholdScalesWithDensityAndWidth(){assertFalse(DismissSwipe.dismiss(100,0,400,3));assertTrue(DismissSwipe.dismiss(150,0,400,3));assertFalse(DismissSwipe.dismiss(150,0,1000,1));assertTrue(DismissSwipe.dismiss(210,0,1000,1));}
}
