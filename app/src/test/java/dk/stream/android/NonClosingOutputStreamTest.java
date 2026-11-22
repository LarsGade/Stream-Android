package dk.stream.android;
import java.io.*;
import javax.crypto.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class NonClosingOutputStreamTest {
 private static class Sink extends ByteArrayOutputStream{
  int singleWrites,blockWrites;boolean closed;
  @Override public synchronized void write(int value){singleWrites++;super.write(value);}
  @Override public synchronized void write(byte[] bytes,int offset,int length){blockWrites++;super.write(bytes,offset,length);}
  @Override public void close(){closed=true;}
 }
 @Test public void blocksAreNotExpandedIntoIndividualWrites()throws Exception{Sink sink=new Sink();NonClosingOutputStream output=new NonClosingOutputStream(sink);byte[] block=new byte[65536];output.write(block);output.write(block,7,1024);output.close();assertEquals(0,sink.singleWrites);assertEquals(2,sink.blockWrites);assertEquals(66560,sink.size());assertFalse(sink.closed);}
 @Test public void encryptedCatalogKeepsBlockWritesAndFileOpen()throws Exception{
  Sink sink=new Sink();KeyGenerator generator=KeyGenerator.getInstance("AES");generator.init(256);javax.crypto.SecretKey key=generator.generateKey();Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key);byte[] iv=cipher.getIV();sink.write(iv.length);sink.write(iv);
  Library.Source source=Library.Source.m3u("Test","https://example.com/list");Catalog catalog=new Catalog();for(int i=0;i<50000;i++)catalog.add(new Playlist.Channel("Kanal "+i,"https://example.com/"+i,"TV"));
  try(OutputStream encrypted=new BufferedOutputStream(new CipherOutputStream(new NonClosingOutputStream(sink),cipher),65536)){CatalogBinary.write(encrypted,source,catalog);}
  assertEquals(1,sink.singleWrites);assertTrue(sink.blockWrites<200);assertFalse(sink.closed);byte[] plain=LibraryCrypto.decrypt(sink.toByteArray(),key);assertEquals(50000,CatalogBinary.read(new ByteArrayInputStream(plain),source).items.size());
 }
}
