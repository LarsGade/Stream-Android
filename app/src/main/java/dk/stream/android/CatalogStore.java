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
 synchronized Catalog load(Library.Source source){try(InputStream input=file(source).openRead();ByteArrayOutputStream output=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))!=-1)output.write(buffer,0,n);return CatalogSnapshot.decode(source,new String(LibraryCrypto.decrypt(output.toByteArray(),LibraryStore.key()),StandardCharsets.UTF_8));}catch(Exception missing){return null;}}
 synchronized boolean save(Library.Source source,Catalog catalog){AtomicFile file=null;FileOutputStream output=null;try{byte[] bytes=LibraryCrypto.encrypt(CatalogSnapshot.encode(source,catalog).getBytes(StandardCharsets.UTF_8),LibraryStore.key());file=file(source);output=file.startWrite();output.write(bytes);file.finishWrite(output);return true;}catch(Exception failure){if(file!=null&&output!=null)file.failWrite(output);return false;}}
}
