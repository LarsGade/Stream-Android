package dk.stream.android;
import java.io.*;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import org.junit.Test;
import static org.junit.Assert.*;
public class BlockCipherInputStreamTest {
 private final javax.crypto.SecretKey key;
 public BlockCipherInputStreamTest()throws Exception{KeyGenerator keys=KeyGenerator.getInstance("AES");keys.init(256);key=keys.generateKey();}
 private Cipher decrypt(byte[] data)throws Exception{Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Arrays.copyOfRange(data,1,13)));return cipher;}
 @Test public void readsLargeCatalogInBlocksAndAuthenticates()throws Exception{
  Library.Source source=Library.Source.m3u("Test","https://example.com/list");Catalog catalog=new Catalog();for(int i=0;i<50000;i++)catalog.add(new Playlist.Channel("Station "+i,"https://example.com/"+i,"TV"));ByteArrayOutputStream plain=new ByteArrayOutputStream();CatalogBinary.write(plain,source,catalog);byte[] data=LibraryCrypto.encrypt(plain.toByteArray(),key);final int[] calls={0};
  InputStream raw=new ByteArrayInputStream(data,13,data.length-13){@Override public synchronized int read(byte[] bytes,int offset,int length){assertEquals(65536,length);calls[0]++;return super.read(bytes,offset,length);}};
  try(InputStream input=new BlockCipherInputStream(raw,decrypt(data))){assertEquals(50000,CatalogBinary.read(input,source).items.size());}assertTrue(calls[0]<200);
 }
 @Test(expected=IOException.class)public void rejectsTamperedAuthenticationTag()throws Exception{byte[] data=LibraryCrypto.encrypt(new byte[100000],key);data[data.length-1]^=1;try(InputStream input=new BlockCipherInputStream(new ByteArrayInputStream(data,13,data.length-13),decrypt(data))){while(input.read()!=-1){}}}
 @Test public void interruptedReadCancelsBeforeReading()throws Exception{byte[] data=LibraryCrypto.encrypt(new byte[10],key);try(InputStream input=new BlockCipherInputStream(new ByteArrayInputStream(data,13,data.length-13),decrypt(data))){Thread.currentThread().interrupt();try{input.read();fail("Expected cancellation");}catch(InterruptedIOException expected){}finally{Thread.interrupted();}}}
}
