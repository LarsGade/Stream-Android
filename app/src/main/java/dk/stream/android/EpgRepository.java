package dk.stream.android;
import android.content.Context;
import java.io.*;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import okhttp3.*;

public final class EpgRepository {
 private final File directory;
 private final OkHttpClient client=new OkHttpClient.Builder().connectTimeout(20,TimeUnit.SECONDS).readTimeout(60,TimeUnit.SECONDS).callTimeout(180,TimeUnit.SECONDS).build();
 public EpgRepository(Context context) {directory=new File(context.getFilesDir(),"tv-guide");}
 private String hash(String value) throws IOException {
  try {byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8"));StringBuilder result=new StringBuilder();for(byte b:bytes) result.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return result.toString();}
  catch(java.security.NoSuchAlgorithmException e) {throw new IOException(e);}
 }
 private File file(Library.Guide guide) throws IOException {return new File(directory,hash(guide.id)+".bin");}
 public boolean fresh(Library.Guide guide,long now) {try {return now-file(guide).lastModified()<30*60*1000L;} catch(IOException e) {return false;}}
 public EpgData cached(Library.Guide guide) {
  try(DataInputStream input=new DataInputStream(new BufferedInputStream(new FileInputStream(file(guide))))) {if(!input.readUTF().equals(hash(guide.url))) return null;return EpgData.read(input);}
  catch(IOException e) {return null;}
 }
 public void remove(Library.Guide guide) {try {file(guide).delete();} catch(IOException ignored) { }}
 public synchronized EpgData fetch(Library.Guide guide,long now) throws IOException {
  try(Response response=client.newCall(new Request.Builder().url(guide.url).header("User-Agent","Stream-Android/1.5.0").build()).execute()) {
   if(!response.isSuccessful()||response.body()==null) throw new IOException("EPG HTTP failure");
   EpgData result;
   try(PushbackInputStream input=new PushbackInputStream(response.body().byteStream(),2)) {
    int a=input.read(),b=input.read();if(b>=0) input.unread(b);if(a>=0) input.unread(a);
    InputStream decoded=a==31 && b==139?new GZIPInputStream(input):input;
    InputStream bounded=new FilterInputStream(decoded) {
     long count;
     private void add(long n) throws IOException {if(n>0 && (count+=n)>256L*1024*1024) throw new IOException("EPG too large");}
     @Override public int read() throws IOException {int value=in.read();if(value>=0) add(1);return value;}
     @Override public int read(byte[] bytes,int offset,int length) throws IOException {int n=in.read(bytes,offset,length);add(n);return n;}
    };
    result=EpgData.parse(bounded,now);
   }
   if(!directory.exists()&&!directory.mkdirs()) throw new IOException("Cannot cache EPG");
   File destination=file(guide),temporary=File.createTempFile("epg-",".tmp",directory);
   try {
    try(DataOutputStream output=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(temporary)))) {output.writeUTF(hash(guide.url));result.write(output);}
    if(!temporary.renameTo(destination)) throw new IOException("Cannot replace EPG cache");
   } finally {temporary.delete();}
   return result;
  }
 }
}
