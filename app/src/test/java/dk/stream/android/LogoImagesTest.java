package dk.stream.android;

import android.graphics.Bitmap;
import android.graphics.Color;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.load.model.GlideUrl;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import okhttp3.mockwebserver.*;
import okio.Buffer;
import java.io.InputStream;
import java.util.concurrent.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class LogoImagesTest {
 private MockWebServer server;
 private ExecutorService executor;
 @Before public void start() throws Exception {server=new MockWebServer();server.start();executor=Executors.newSingleThreadExecutor();}
 @After public void stop() throws Exception {executor.shutdownNow();server.shutdown();Glide.tearDown();}
 @Test public void mediaArtworkPreservesOriginalLogoTransparency() throws Exception {
  byte[] encoded=PlaybackArtwork.local(RuntimeEnvironment.getApplication(),"DR P1");
  try(InputStream original=RuntimeEnvironment.getApplication().getAssets().open("channel_logos/DR P1.png")) {assertArrayEquals(original.readAllBytes(),encoded);}
  Bitmap logo=android.graphics.BitmapFactory.decodeByteArray(encoded,0,encoded.length);assertEquals(0,Color.alpha(logo.getPixel(0,0)));assertEquals(283,logo.getWidth());assertEquals(273,logo.getHeight());
 }
 private int imagePixel(String path) throws Exception {
  GlideUrl model=LogoRequests.model(server.url(path).toString(),server.url("/get.php?username=user&password=secret").toString());
  return executor.submit(()->{
   FutureTarget<Bitmap> target=Glide.with(RuntimeEnvironment.getApplication()).asBitmap().load(model).submit(32,32);
   try {Bitmap bitmap=target.get(20,TimeUnit.SECONDS);return bitmap.getPixel(bitmap.getWidth()/2,bitmap.getHeight()/2);}
   finally {Glide.with(RuntimeEnvironment.getApplication()).clear(target);}
  }).get(25,TimeUnit.SECONDS);
 }
 private MockResponse fixture(String file,String type) throws Exception {
  try(InputStream input=getClass().getResourceAsStream("/logos/"+file)) {assertNotNull(input);return new MockResponse().setHeader("Content-Type",type).setBody(new Buffer().readFrom(input));}
 }
 @Test public void downloadsAndDisplaysPngWithProviderHeaders() throws Exception {
  server.enqueue(fixture("blue.png","image/png"));assertEquals(Color.BLUE,imagePixel("/logo.png"));
  RecordedRequest request=server.takeRequest();assertTrue(request.getHeader("User-Agent").contains("Stream/"));
  assertEquals(server.url("/").toString(),request.getHeader("Referer"));assertFalse(request.getHeaders().toString().contains("secret"));
 }
 @Test public void followsRedirectAndDisplaysJpeg() throws Exception {
  server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location","/actual.jpg"));server.enqueue(fixture("red.jpg","image/jpeg"));
  int pixel=imagePixel("/redirect");assertTrue(Color.red(pixel)>200);assertTrue(Color.green(pixel)<30);assertTrue(Color.blue(pixel)<30);
 }
 @Test public void displaysWebpLogo() throws Exception {
  server.enqueue(fixture("green.webp","image/webp"));assertEquals(Color.GREEN,imagePixel("/logo.webp"));
 }
 @Test public void downloadsAndRendersSvgLogoInsteadOfPlaceholder() throws Exception {
  server.enqueue(new MockResponse().setHeader("Content-Type","image/svg+xml").setBody("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" viewBox=\"0 0 16 16\"><rect width=\"16\" height=\"16\" fill=\"#0000ff\"/></svg>"));
  assertEquals(Color.BLUE,imagePixel("/logo.svg"));
 }
 @Test public void requestDoesNotLeakCredentialsFromSourcePathOrQuery() {
  GlideUrl request=LogoRequests.model("https://cdn.example.org/logo.png","https://user:password@example.com/live/account/secret/1.ts?token=private");
  assertEquals("https://example.com/",request.getHeaders().get("Referer"));
 }
 @Test public void bundledLogosLoadFromAssetsIncludingSpacesAndApostrophes() throws Exception {
  LocalChannelLogos logos=LocalChannelLogos.load(RuntimeEnvironment.getApplication());assertEquals(47,logos.size());
  assertNull(logos.find("Unknown channel"));
  for(String name:new String[]{"DR 1","TV2 News","6’eren","TV3+"}) {
   String model=logos.find(name);assertNotNull(name,model);
   boolean decoded=executor.submit(()->{
    FutureTarget<Bitmap> target=Glide.with(RuntimeEnvironment.getApplication()).asBitmap().load(model).submit(64,64);
    try {Bitmap bitmap=target.get(20,TimeUnit.SECONDS);return bitmap.getWidth()>0 && bitmap.getHeight()>0;}
    finally {Glide.with(RuntimeEnvironment.getApplication()).clear(target);}
   }).get(25,TimeUnit.SECONDS);
   assertTrue(name,decoded);
  }
 }
}
