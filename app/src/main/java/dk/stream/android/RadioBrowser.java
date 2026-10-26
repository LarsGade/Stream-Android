package dk.stream.android;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import com.google.gson.stream.*;

/** Public Radio Browser mirrors; only playable Danish entries with HTTP(S) streams are retained. */
public final class RadioBrowser {
 private final List<HttpUrl> servers;
 private final OkHttpClient client=new OkHttpClient.Builder().connectTimeout(10,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).callTimeout(30,TimeUnit.SECONDS).build();
 public RadioBrowser() {this(Arrays.asList(HttpUrl.get("https://de1.api.radio-browser.info/"),HttpUrl.get("https://nl1.api.radio-browser.info/"),HttpUrl.get("https://at1.api.radio-browser.info/")));}
 RadioBrowser(List<HttpUrl> servers) {this.servers=servers;}
 public List<RadioStation> loadDanish() throws IOException {
  IOException failure=new IOException("Radio Browser unavailable");
  for(HttpUrl server:servers) {
   try {return fetch(server);} catch(IOException|IllegalStateException e) {failure=new IOException("Radio Browser unavailable",e);}
  }
  throw failure;
 }
 private static String value(Map<String,String> fields,String key) {String result=fields.get(key);return result==null?"":result;}
 private List<RadioStation> fetch(HttpUrl server) throws IOException {
  HttpUrl url=server.resolve("json/stations/bycountrycodeexact/DK").newBuilder().addQueryParameter("hidebroken","true").addQueryParameter("order","name").build();
  try(Response response=client.newCall(new Request.Builder().url(url).header("User-Agent","Stream-Android/1.8 (Radio Browser)").build()).execute()) {
   if(!response.isSuccessful() || response.body()==null) throw new IOException("Radio Browser request failed");
   List<RadioStation> stations=new ArrayList<>();Set<String> ids=new HashSet<>();
   try(JsonReader reader=new JsonReader(response.body().charStream())) {
    reader.beginArray();
    while(reader.hasNext()) {
     Map<String,String> fields=new HashMap<>();reader.beginObject();
     while(reader.hasNext()) {String name=reader.nextName();JsonToken token=reader.peek();if(Arrays.asList("stationuuid","name","url","url_resolved","favicon","countrycode","lastcheckok","hls").contains(name) && (token==JsonToken.STRING||token==JsonToken.NUMBER)) fields.put(name,reader.nextString());else reader.skipValue();}
     reader.endObject();String id=value(fields,"stationuuid"),name=value(fields,"name").trim();
     if(!"DK".equalsIgnoreCase(value(fields,"countrycode")) || "0".equals(fields.get("lastcheckok")) || id.isEmpty() || name.isEmpty()) continue;
     HttpUrl stream=HttpUrl.parse(value(fields,"url_resolved"));if(stream==null) stream=HttpUrl.parse(value(fields,"url"));
     HttpUrl artwork=HttpUrl.parse(value(fields,"favicon"));
     if(stream!=null && ids.add(id)) stations.add(new RadioStation(id,name,stream.toString(),artwork==null?"":artwork.toString(),"","1".equals(value(fields,"hls"))));
    }
    reader.endArray();if(reader.peek()!=JsonToken.END_DOCUMENT) throw new IOException("Invalid Radio Browser response");
   }
   return stations;
  }
 }
}
