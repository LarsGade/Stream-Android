package dk.stream.android;
import java.io.*;
import javax.crypto.Cipher;

/** Uses bounded block updates instead of thousands of small Android Keystore calls. */
final class BlockCipherInputStream extends InputStream {
 private final InputStream input;private final Cipher cipher;private final byte[] block=new byte[64*1024];
 private byte[] ready=new byte[0];private int position;private boolean ended;
 BlockCipherInputStream(InputStream input,Cipher cipher){this.input=input;this.cipher=cipher;}
 private boolean fill()throws IOException{
  while(position>=ready.length){
   if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Catalog read cancelled");
   if(ended)return false;
   int count=input.read(block);
   try{byte[] bytes;if(count<0){bytes=cipher.doFinal();ended=true;}else if(count==0)continue;else bytes=cipher.update(block,0,count);ready=bytes==null?new byte[0]:bytes;position=0;}
   catch(java.security.GeneralSecurityException failure){throw new IOException("Catalog authentication failed",failure);}
  }return true;
 }
 @Override public int read()throws IOException{return fill()?ready[position++]&255:-1;}
 @Override public int read(byte[] bytes,int offset,int length)throws IOException{if(offset<0||length<0||offset>bytes.length-length)throw new IndexOutOfBoundsException();if(length==0)return 0;if(!fill())return -1;int count=Math.min(length,ready.length-position);System.arraycopy(ready,position,bytes,offset,count);position+=count;return count;}
 @Override public void close()throws IOException{input.close();}
}
