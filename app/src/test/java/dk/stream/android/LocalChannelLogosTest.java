package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class LocalChannelLogosTest {
 @Test public void matchesCaseAndSpacingWithoutChangingChannelIdentity() {
  assertEquals(LocalChannelLogos.key("DR1"),LocalChannelLogos.key(" dr 1 "));
  assertEquals(LocalChannelLogos.key("6'eren"),LocalChannelLogos.key("6’eren"));
  assertEquals(LocalChannelLogos.key("TV3+"),LocalChannelLogos.key("tv3 +"));
  assertNotEquals(LocalChannelLogos.key("TV3"),LocalChannelLogos.key("TV3+"));
  assertNotEquals(LocalChannelLogos.key("TV2 Sport"),LocalChannelLogos.key("TV2 Sport X"));
 }
}
