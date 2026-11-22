package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class EpgDataTest {
 private final long now=EpgData.time("20261025120000 +0000");
 private EpgData parse(String xml) throws Exception {return EpgData.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),now);}
 private Playlist.Channel channel(String name,String id) {return new Playlist.Channel(name,"https://example.com/live","DK",Playlist.Kind.TV,null,"",id,"");}
 @Test public void matchesXmltvIdAndUsesCurrentProgrammeWithTimezoneOffsets() throws Exception {
  EpgData data=parse("<tv><channel id='dr1.dk'><display-name>DR 1</display-name></channel><programme channel='dr1.dk' start='20261025120000 +0100' stop='20261025140000 +0100'><title lang='da'>Nyheder &amp; vejret</title></programme></tv>");
  assertEquals("Nyheder & vejret",data.current(channel("Unrelated label","dr1.dk"),now).title);
  assertEquals("Nyheder & vejret",data.current(channel("dr1",""),now).title);
  assertNull(data.current(channel("DR1",""),EpgData.time("20261025130000 +0000")));
 }
 @Test public void usesTvgNameAndDoesNotGuessAmbiguousNames() throws Exception {
  EpgData data=parse("<tv><channel id='one'><display-name>Shared</display-name><display-name>Unique</display-name></channel><channel id='two'><display-name>Shared</display-name></channel><programme channel='one' start='20261025110000' stop='20261025130000'><title>First</title></programme><programme channel='two' start='20261025110000' stop='20261025130000'><title>Second</title></programme></tv>");
  assertNull(data.current(channel("Shared",""),now));
  assertEquals("Second",data.current(channel("Shared","two"),now).title);
  var alias=new Playlist.Channel("Label","https://example.com/live","",Playlist.Kind.TV,null,"","","Unique");
  assertEquals("First",data.current(alias,now).title);
 }
 @Test public void missingStopUsesFollowingStartAndChangesAtBoundary() throws Exception {
  EpgData data=parse("<tv><programme channel='c' start='20261025130000' stop='20261025140000'><title>Next</title></programme><programme channel='c' start='20261025110000'><title>Now</title></programme></tv>");
  assertEquals("Now",data.current(channel("c","c"),now).title);
  assertEquals("Next",data.current(channel("c","c"),EpgData.time("20261025130000")).title);
  assertNull(data.current(channel("c","c"),EpgData.time("20261025140000")));
 }
 @Test public void ongoingLongProgrammeIsNotDiscarded() throws Exception {
  EpgData data=parse("<tv><programme channel='c' start='20261023000000' stop='20261026000000'><title>Marathon</title></programme></tv>");
  assertEquals("Marathon",data.current(channel("c","c"),now).title);
 }
 @Test public void cacheRoundTripPreservesChannelMatchingAndSchedules() throws Exception {
  EpgData original=parse("<tv><channel id='c'><display-name>TV</display-name></channel><programme channel='c' start='20261025110000' stop='20261025130000'><title>Current</title></programme></tv>");
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();original.write(new DataOutputStream(bytes));
  EpgData restored=EpgData.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
  assertEquals("Current",restored.current(channel("TV",""),now).title);
 }
 @Test public void handlesDstExplicitOffsetsAndRejectsInvalidDates() {
  assertEquals(3600000L,EpgData.time("20261025023000 +0100")-EpgData.time("20261025023000 +0200"));
  assertEquals(EpgData.time("20261025120000Z"),EpgData.time("202610251200 +0000"));
  assertEquals(-1,EpgData.time("20260230120000 +0000"));assertEquals(-1,EpgData.time("20261025120000 +0165"));
 }
 @Test(expected=IOException.class) public void rejectsLoginHtml() throws Exception {parse("<html><title>Login</title></html>");}
 @Test public void doesNotReadExternalDtdOrEntities() throws Exception {
  EpgData data=parse("<!DOCTYPE tv SYSTEM 'file:///does-not-exist'><tv><programme channel='c' start='20261025110000' stop='20261025130000'><title>Safe</title></programme></tv>");
  assertEquals("Safe",data.current(channel("c","c"),now).title);
 }
}
