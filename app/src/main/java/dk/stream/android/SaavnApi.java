package dk.stream.android;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.json.*;
/** Saavn.dev-compatible search and details, with bounded reads and cancellable UI requests. */
final class SaavnApi {
 static final String DEFAULT="https://saavn.sumit.co/api/";
 static final OkHttpClient HTTP=new OkHttpClient.Builder().connectTimeout(15,TimeUnit.SECONDS).readTimeout(25,TimeUnit.SECONDS).callTimeout(35,TimeUnit.SECONDS).build();
 static final OkHttpClient STREAM=HTTP.newBuilder().callTimeout(0,TimeUnit.MILLISECONDS).build();
 private final HttpUrl base;private final OkHttpClient client;private volatile Call active;
 SaavnApi(String address){this(address,HTTP);}
 SaavnApi(String address,OkHttpClient client){base=base(address);this.client=client;}
 static HttpUrl base(String address){HttpUrl url=HttpUrl.parse(address==null?DEFAULT:address.trim());if(url==null||!url.username().isEmpty()||!url.password().isEmpty()||url.query()!=null||url.fragment()!=null)throw new IllegalArgumentException("Angiv en gyldig HTTP/HTTPS API-adresse uden login eller søgeparametre.");if(!url.encodedPath().endsWith("/"))url=url.newBuilder().addPathSegment("").build();if(url.host().equals("saavn.dev")&&url.encodedPath().equals("/api/"))return HttpUrl.parse(DEFAULT);return url;}
 static final class Page {
  final List<SaavnSong> songs;final int total;final String artist;
  Page(List<SaavnSong> songs,int total){this(songs,total,"");}
  Page(List<SaavnSong> songs,int total,String artist){this.songs=songs;this.total=total;this.artist=artist;}
  String summary(int count){if(count==0)return artist.isEmpty()?"Saavn fandt ingen sange. Kataloget kan mangle den ønskede originalindspilning.":"Saavn returnerede ingen sange fra "+artist+". Originalindspilningerne kan mangle i kataloget.";return count+" sange"+(artist.isEmpty()?"":" fra "+artist)+" · Tryk for at afspille";}
 }
 /** Resolve an exact artist before song-title search, which also finds tribute recordings. */
 Page searchMusic(String query,int page)throws IOException{
  if(query==null||query.trim().isEmpty()||page<0)throw new IllegalArgumentException("Indtast en søgning.");
  JSONObject response=get(base.newBuilder().addPathSegments("search/artists").addQueryParameter("query",query.trim()).addQueryParameter("page","0").addQueryParameter("limit","20").build());
  try{
   JSONArray artists=response.getJSONObject("data").getJSONArray("results");
   for(int i=0;i<artists.length();i++){
    JSONObject artist=artists.optJSONObject(i);if(artist==null)continue;
    String name=SaavnSong.text(artist.optString("name")),id=artist.optString("id");
    if(!normalized(name).equals(normalized(query))||!id.matches("[A-Za-z0-9_-]{1,128}"))continue;
    JSONObject detail=get(base.newBuilder().addPathSegment("artists").addPathSegment(id).addPathSegment("songs").addQueryParameter("page",String.valueOf(page)).addQueryParameter("sortBy","popularity").addQueryParameter("sortOrder","desc").build());
    JSONObject data=detail.getJSONObject("data");
    JSONObject shaped=new JSONObject().put("data",new JSONObject().put("results",data.getJSONArray("songs")).put("total",data.optInt("total",0)));
    Page found=parseSearch(shaped);return new Page(found.songs,found.total,name);
   }
  }catch(JSONException invalid){throw new IOException("Musikkilden returnerede ugyldige kunstnerdata.",invalid);}
  return search(query,page);
 }
 private static String normalized(String value){return java.text.Normalizer.normalize(value,java.text.Normalizer.Form.NFKC).trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);}
 Page search(String query,int page)throws IOException{if(query==null||query.trim().isEmpty()||page<0)throw new IllegalArgumentException("Indtast en søgning.");JSONObject response=get(base.newBuilder().addPathSegments("search/songs").addQueryParameter("query",query.trim()).addQueryParameter("page",String.valueOf(page)).addQueryParameter("limit","20").build());return parseSearch(response);}
 SaavnSong song(String id)throws IOException{try{JSONObject response=get(base.newBuilder().addPathSegment("songs").addPathSegment(id).build());Object data=response.opt("data");JSONArray items=data instanceof JSONArray?(JSONArray)data:null;JSONObject value=items==null?response.optJSONObject("data"):items.optJSONObject(0);if(value==null)throw new JSONException("Missing song");return SaavnSong.parse(value);}catch(JSONException invalid){throw new IOException("Musikkilden kunne ikke finde sangen.",invalid);}}
 static Page parseSearch(JSONObject response)throws IOException{try{if(response.has("success")&&!response.optBoolean("success"))throw new IOException("Musikkilden kunne ikke gennemføre søgningen.");JSONObject data=response.getJSONObject("data");JSONArray results=data.getJSONArray("results");List<SaavnSong> songs=new ArrayList<>();Set<String> seen=new HashSet<>();for(int i=0;i<results.length();i++){try{SaavnSong song=SaavnSong.parse(results.getJSONObject(i));if(seen.add(song.id))songs.add(song);}catch(JSONException ignored){}}return new Page(songs,Math.max(songs.size(),data.optInt("total",songs.size())));}catch(JSONException invalid){throw new IOException("Musikkilden returnerede et ugyldigt svar.",invalid);}}
 private JSONObject get(HttpUrl url)throws IOException{Call call=client.newCall(new Request.Builder().url(url).header("Accept","application/json").build());active=call;try(Response response=call.execute()){if(!response.isSuccessful())throw new IOException("Musikkilden svarede med fejl "+response.code()+". Prøv igen senere.");if(response.body()==null)throw new IOException("Tomt svar fra musikkilden.");ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream input=response.body().byteStream()){byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1){if(bytes.size()+count>4*1024*1024)throw new IOException("Svaret fra musikkilden er for stort.");bytes.write(buffer,0,count);}}try{JSONObject data=new JSONObject(bytes.toString("UTF-8"));if(data.has("success")&&!data.optBoolean("success"))throw new IOException("Musikkilden kunne ikke gennemføre forespørgslen.");return data;}catch(JSONException invalid){throw new IOException("Musikkilden returnerede ikke gyldige musikdata.",invalid);}}catch(java.net.UnknownHostException unavailable){throw new IOException("Musikkildens adresse kunne ikke findes. Kontrollér internetforbindelsen, eller vælg en anden musikkilde i indstillingerne.",unavailable);}catch(java.net.SocketTimeoutException timeout){throw new IOException("Musikkilden svarede ikke i tide. Prøv igen.",timeout);}catch(java.net.ConnectException unavailable){throw new IOException("Der kunne ikke oprettes forbindelse til musikkilden. Prøv igen senere.",unavailable);}finally{if(active==call)active=null;}}
 void cancel(){Call call=active;if(call!=null)call.cancel();}
}
