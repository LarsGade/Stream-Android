package dk.stream.android;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

public final class LogoRequests {
 private LogoRequests() { }
 public static GlideUrl model(String url,String source) {
  if(url==null || url.isEmpty()) return null;
  LazyHeaders.Builder headers=new LazyHeaders.Builder()
   .setHeader("User-Agent","Mozilla/5.0 (Linux; Android) Stream/1.2.3")
   .setHeader("Accept","image/svg+xml,image/*,*/*;q=0.8");
  if(source!=null) {
   try {
    // Send only the origin, never account parameters or credentials in source paths.
    String origin=Playlist.httpUrl(source).newBuilder().username("").password("").encodedPath("/").query(null).fragment(null).build().toString();
    headers.setHeader("Referer",origin);
   } catch(IllegalArgumentException ignored) { }
  }
  return new GlideUrl(url,headers.build());
 }
}
