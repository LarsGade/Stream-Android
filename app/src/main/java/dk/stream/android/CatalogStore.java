package dk.stream.android;
import android.content.Context;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Persistent encrypted catalogs; never expires or fetches the network. */
final class CatalogStore {
 private final Context context;
 CatalogStore(Context context){this.context=context.getApplicationContext();}
 private AtomicFile file(Library.Source source)throws Exception{
  byte[] digest=MessageDigest.getInstance("SHA-256").digest(source.id.getBytes(StandardCharsets.UTF_8));StringBuilder name=new StringBuilder();for(byte b:digest)name.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
  File folder=new File(context.getFilesDir(),"stream_catalogs");if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("Catalog directory");return new AtomicFile(new File(folder,name+".bin"));
 }
 synchronized Catalog load(Library.Source source){
  try(InputStream raw=file(source).openRead()){
   int length=raw.read();if(length!=12)throw new IOException("Invalid encryption header");byte[] iv=new byte[length];new DataInputStream(raw).readFully(iv);
   javax.crypto.Cipher cipher=javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");cipher.init(javax.crypto.Cipher.DECRYPT_MODE,LibraryStore.key(),new javax.crypto.spec.GCMParameterSpec(128,iv));
   try(BufferedInputStream input=new BufferedInputStream(new javax.crypto.CipherInputStream(raw,cipher))){
    input.mark(4);int magic=new DataInputStream(input).readInt();input.reset();if(magic==CatalogBinary.MAGIC)return CatalogBinary.read(input,source);
    // Migrate snapshots written by 1.27.0 without downloading the source again.
    ByteArrayOutputStream output=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))!=-1)output.write(buffer,0,n);return CatalogSnapshot.decode(source,new String(output.toByteArray(),StandardCharsets.UTF_8));
   }
  }catch(Exception missing){return null;}
 }
 synchronized boolean save(Library.Source source,Catalog catalog){
  AtomicFile file=null;FileOutputStream output=null;
  try{
   javax.crypto.Cipher cipher=javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");cipher.init(javax.crypto.Cipher.ENCRYPT_MODE,LibraryStore.key());byte[] iv=cipher.getIV();
   file=file(source);output=file.startWrite();output.write(iv.length);output.write(iv);
   OutputStream nonClosing=new NonClosingOutputStream(output);
   try(OutputStream encrypted=new BufferedOutputStream(new javax.crypto.CipherOutputStream(nonClosing,cipher),64*1024)){CatalogBinary.write(encrypted,source,catalog);}
   file.finishWrite(output);return true;
  }catch(Exception failure){if(file!=null&&output!=null)file.failWrite(output);return false;}
 }
}
