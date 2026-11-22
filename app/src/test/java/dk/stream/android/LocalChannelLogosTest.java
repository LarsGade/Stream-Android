package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class LocalChannelLogosTest {
 private LocalChannelLogos logos() throws Exception {java.util.Map<String,String> files=new java.util.HashMap<>();files.put("drp5","P5.png");files.put("drp6","P6.png");files.put("drp4","DR P4.png");java.lang.reflect.Constructor<LocalChannelLogos> constructor=LocalChannelLogos.class.getDeclaredConstructor(java.util.Map.class);constructor.setAccessible(true);return constructor.newInstance(files);}
 @Test public void everyP5VariantUsesP5Asset() throws Exception {LocalChannelLogos logos=logos();for(String name:new String[]{"P5","DR P5","P5 Fyn","DR P5 København","p5 jazz","P 5 Nord"})assertEquals(name,"file:///android_asset/channel_logos/P5.png",logos.find(name));}
 @Test public void everyP6VariantUsesP6Asset() throws Exception {LocalChannelLogos logos=logos();for(String name:new String[]{"P6","DR P6","P6 Beat","dr p6 musik"})assertEquals(name,"file:///android_asset/channel_logos/P6.png",logos.find(name));}
 @Test public void otherNamesStillUseTheirOwnMappings() throws Exception {assertEquals("file:///android_asset/channel_logos/DR P4.png",logos().find("P4 Fyn"));assertNull(logos().find("P7"));}

 @Test public void matchesCaseAndSpacingWithoutChangingChannelIdentity() {
  assertEquals(LocalChannelLogos.key("DR1"),LocalChannelLogos.key(" dr 1 "));
  assertEquals(LocalChannelLogos.key("6'eren"),LocalChannelLogos.key("6’eren"));
  assertEquals(LocalChannelLogos.key("TV3+"),LocalChannelLogos.key("tv3 +"));
  assertNotEquals(LocalChannelLogos.key("TV3"),LocalChannelLogos.key("TV3+"));
  assertNotEquals(LocalChannelLogos.key("TV2 Sport"),LocalChannelLogos.key("TV2 Sport X"));
 }
}
