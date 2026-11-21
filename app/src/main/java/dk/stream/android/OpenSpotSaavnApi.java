package dk.stream.android;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import okhttp3.*;
import org.json.*;

/** JioSaavn calls and search transport used by OpenSpot 3.1.5 (see third-party notices). */
final class OpenSpotSaavnApi {
 static final String DEFAULT="https://www.jiosaavn.com/api.php";
 static final String PROXY="https://nextjs-proxy-gamma.vercel.app/api/proxy";
 static final String PREFIX="jiosaavn_";
 private static final String USER_AGENT="Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
 private final HttpUrl base,proxy;private final OkHttpClient client;private volatile Call active;
 OpenSpotSaavnApi(String address,String proxy){this(address,proxy,SaavnApi.HTTP);}
 OpenSpotSaavnApi(String address,String proxy,OkHttpClient client){this.base=endpoint(address);this.proxy=endpoint(proxy);this.client=client;}
 static HttpUrl endpoint(String address){HttpUrl url=HttpUrl.parse(address==null?"":address.trim());if(url==null||!url.username().isEmpty()||!url.password().isEmpty()||url.query()!=null||url.fragment()!=null)throw new IllegalArgumentException("Angiv en gyldig HTTP/HTTPS-adresse uden login eller søgeparametre.");return url;}
 static boolean owns(String id){return id!=null&&id.startsWith(PREFIX);}
 static final class Page {
  final List<SaavnSong> songs;final boolean hasMore;
  Page(List<SaavnSong> songs,boolean more){this.songs=songs;this.hasMore=more;}
  String summary(int count){return count+" resultater";}
 }
 Page searchMusic(String query,int page)throws IOException{
  if(query==null||query.trim().isEmpty()||page<0||page==Integer.MAX_VALUE)throw new IllegalArgumentException("Indtast en søgning.");
  HttpUrl url=route("search.getResults").addQueryParameter("q",query.trim()).addQueryParameter("p",String.valueOf(page+1)).addQueryParameter("n","20").build();
  JSONObject data=proxied(url);JSONObject response=data.optJSONObject("response");if(response==null)response=data;
   JSONArray rows=response.optJSONArray("songs");if(rows==null)rows=response.optJSONArray("results");
   if(rows==null)throw new IOException("Saavn returnerede ugyldige søgeresultater.");
   List<SaavnSong> songs=new ArrayList<>();Set<String> seen=new HashSet<>();
   for(int i=0;i<rows.length();i++){try{SaavnSong song=parse(rows.getJSONObject(i),false);if(seen.add(song.id))songs.add(song);}catch(JSONException ignored){}}
   int total=response.optInt("total",-1);return new Page(songs,total>=0?(long)(page+1)*20<total:rows.length()>=20);
 }
 static final int SONG=0,ARTIST=1,ALBUM=2;
 static final class Entity {
  final int type;final String id,name,detail,image;
  Entity(int type,String id,String name,String detail,String image){this.type=type;this.id=id;this.name=name;this.detail=detail;this.image=image;}
 }
 static final class EntityPage {
  final List<Entity> items;final boolean hasMore;
  EntityPage(List<Entity> items,boolean more){this.items=items;this.hasMore=more;}
 }
 EntityPage searchEntities(String query,int page,int type)throws IOException{
  if(query==null||query.trim().isEmpty()||page<0||page==Integer.MAX_VALUE||(type!=ARTIST&&type!=ALBUM))throw new IllegalArgumentException("Ugyldig musiksøgning.");
  String call=type==ARTIST?"search.getArtistResults":"search.getAlbumResults";
  JSONObject data=proxied(route(call).addQueryParameter("q",query.trim()).addQueryParameter("p",String.valueOf(page+1)).addQueryParameter("n","20").build());
  JSONObject response=data.optJSONObject("response");if(response==null)response=data;
  JSONArray rows=response.optJSONArray(type==ARTIST?"artists":"albums");if(rows==null)rows=response.optJSONArray("results");if(rows==null)throw new IOException("Saavn returnerede ugyldige søgeresultater.");
  List<Entity> items=new ArrayList<>();Set<String> seen=new HashSet<>();
  for(int i=0;i<rows.length();i++){
   JSONObject item=rows.optJSONObject(i);if(item==null)continue;
   String id=item.optString("id",item.optString(type==ARTIST?"artistId":"albumid",""));String name=SaavnSong.text(item.optString("name",item.optString("title","")));
   if(!id.matches("[A-Za-z0-9_-]{1,100}")||name.isEmpty()||!seen.add(id))continue;
   String detail=type==ARTIST?"Kunstner":"Album";if(type==ALBUM){String year=item.optString("year","");if(!year.isEmpty())detail+=" · "+year;}
   items.add(new Entity(type,id,name,detail,image(item.optString("image",item.optString("artistImage","")))));
  }
  int total=response.optInt("total",-1);return new EntityPage(items,total>=0?(long)(page+1)*20<total:rows.length()>=20);
 }
 Page collection(Entity entity,int page)throws IOException{
  if(entity==null||!entity.id.matches("[A-Za-z0-9_-]{1,100}")||page<0||page==Integer.MAX_VALUE||(entity.type!=ARTIST&&entity.type!=ALBUM))throw new IllegalArgumentException("Ugyldig musiksamling.");
  HttpUrl.Builder url=route(entity.type==ARTIST?"artist.getArtistMoreSong":"content.getAlbumDetails");
  if(entity.type==ARTIST)url.addQueryParameter("artistId",entity.id).addQueryParameter("page",String.valueOf(page+1)).addQueryParameter("n_song","50");else url.addQueryParameter("albumid",entity.id);
  JSONObject data=proxied(url.build());JSONArray rows=collectionRows(data,0);if(rows==null)throw new IOException("Saavn kunne ikke hente sangene.");
  List<SaavnSong> songs=new ArrayList<>();Set<String> seen=new HashSet<>();
  for(int i=0;i<rows.length();i++){try{SaavnSong song=parse(rows.getJSONObject(i),false);if(seen.add(song.id))songs.add(song);}catch(JSONException ignored){}}
  JSONObject response=data.optJSONObject("response");if(response==null)response=data;JSONObject top=response.optJSONObject("topSongs");int total=top==null?response.optInt("total_songs",response.optInt("song_count",response.optInt("total",-1))):top.optInt("total",-1);
  return new Page(songs,entity.type==ARTIST&&(total>=0?(long)(page+1)*50<total:rows.length()>=50));
 }
 private static JSONArray collectionRows(JSONObject value,int depth){
  if(depth>3)return null;for(String key:new String[]{"songs","topSongs","list"}){JSONArray rows=value.optJSONArray(key);if(rows!=null)return rows;}
  for(String key:new String[]{"topSongs","response","artistPage"}){JSONObject nested=value.optJSONObject(key);if(nested!=null){JSONArray rows=collectionRows(nested,depth+1);if(rows!=null)return rows;}}return null;
 }
 private JSONObject proxied(HttpUrl url)throws IOException{
  try{
   JSONObject payload=new JSONObject().put("url",url.toString()).put("method","GET").put("headers",new JSONObject().put("User-Agent",USER_AGENT));
   Request request=new Request.Builder().url(proxy).post(RequestBody.create(payload.toString(),MediaType.get("application/json; charset=utf-8"))).build();
   JSONObject envelope=get(request);int code=envelope.optInt("status",200);if(code<200||code>=300)throw new IOException("Saavn-søgeserveren svarede med fejl "+code+". Prøv igen senere.");
   String body=envelope.optString("body","");if(body.isEmpty())throw new IOException("Saavn-søgeserveren returnerede intet svar.");return new JSONObject(body);
  }catch(JSONException invalid){throw new IOException("Saavn-søgeserveren returnerede ugyldige musikdata.",invalid);}
 }
 private static String image(String value){String image=value.replaceFirst("^http://","https://").replace("150x150","500x500").replace("50x50","500x500");return HttpUrl.parse(image)==null?"":image;}
 SaavnSong song(String reference)throws IOException{
  String id=owns(reference)?reference.substring(PREFIX.length()):"";if(!id.matches("[A-Za-z0-9_-]{1,100}"))throw new IOException("Ugyldig Saavn-reference.");
  JSONObject response=get(new Request.Builder().url(route("song.getDetails").addQueryParameter("pids",id).build()).header("User-Agent",USER_AGENT).build());
  try{JSONArray rows=response.getJSONArray("songs");JSONObject matching=null;for(int i=0;i<rows.length();i++){JSONObject item=rows.optJSONObject(i);if(item!=null&&id.equals(item.optString("id"))){matching=item;break;}}if(matching==null)throw new JSONException("Missing requested song");return parse(matching,true);}
  catch(JSONException invalid){throw new IOException("Saavn kunne ikke hente den valgte sang.",invalid);}
 }
 private HttpUrl.Builder route(String call){return base.newBuilder().addQueryParameter("__call",call).addQueryParameter("_format","json").addQueryParameter("_marker","0").addQueryParameter("api_version","4").addQueryParameter("ctx","web6dot0");}
 private static SaavnSong parse(JSONObject item,boolean stream)throws JSONException,IOException{
  String id=item.getString("id");if(!id.matches("[A-Za-z0-9_-]{1,100}"))throw new JSONException("Invalid ID");
  JSONObject more=item.optJSONObject("more_info");if(more==null)more=new JSONObject();JSONObject map=more.optJSONObject("artistMap"),artists=item.optJSONObject("artists");
  JSONArray primary=map==null?null:map.optJSONArray("primary_artists");if(primary==null&&artists!=null)primary=artists.optJSONArray("primary");
  List<String> names=new ArrayList<>();if(primary!=null)for(int i=0;i<primary.length();i++){JSONObject artist=primary.optJSONObject(i);if(artist!=null&&!artist.optString("name").isEmpty())names.add(SaavnSong.text(artist.optString("name")));}
  String artist=names.isEmpty()?SaavnSong.text(item.optString("primary_artists","")):join(names);
  String title=SaavnSong.text(item.optString("title",item.optString("song","")));if(title.isEmpty())throw new JSONException("Missing title");
  String image=item.optString("image","").replaceFirst("^http://","https://").replace("150x150","500x500").replace("50x50","500x500");HttpUrl cover=HttpUrl.parse(image);if(cover==null)image="";
  String media="";if(stream){media=decodeMedia(more.optString("encrypted_media_url"));if("true".equalsIgnoreCase(more.optString("320kbps"))){HttpUrl url=HttpUrl.get(media);String path=url.encodedPath();if(path.endsWith("_96.mp4"))media=url.newBuilder().encodedPath(path.substring(0,path.length()-7)+"_320.mp4").build().toString();}}
  return new SaavnSong(PREFIX+id,title,artist,SaavnSong.text(more.optString("album",item.optString("album",""))),image,media,more.optLong("duration",item.optLong("duration",0)),more.optString("album_id",more.optString("albumid",item.optString("albumid",""))));
 }
 private static String join(List<String> values){StringBuilder result=new StringBuilder();for(String value:values){if(result.length()>0)result.append(", ");result.append(value);}return result.toString();}
 /** Decode the API's obfuscated media address using the format also implemented by OpenSpot. */
 static String decodeMedia(String encoded)throws IOException{
  if(encoded==null||encoded.isEmpty()||encoded.length()>16384)throw new IOException("Saavn har ingen gyldig afspilningsadresse til denne sang.");
  try{Cipher cipher=Cipher.getInstance("DES/ECB/PKCS5Padding");cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec("38346591".getBytes(StandardCharsets.UTF_8),"DES"));
   String decoded=new String(cipher.doFinal(decodeBase64(encoded)),StandardCharsets.UTF_8).trim();HttpUrl url=HttpUrl.parse(decoded);if(url==null||!url.username().isEmpty()||!url.password().isEmpty())throw new IOException("Ugyldig medieadresse");return url.toString();
  }catch(Exception invalid){throw new IOException("Saavn returnerede en ugyldig afspilningsadresse.",invalid);}
 }
 // Okio Base64 works on Android 6 as well as the JVM (java.util.Base64 requires Android 8).
 private static byte[] decodeBase64(String value)throws IOException{okio.ByteString bytes=okio.ByteString.decodeBase64(value);if(bytes==null)throw new IOException("Invalid media address");return bytes.toByteArray();}
 private JSONObject get(Request request)throws IOException{
  Call call=client.newCall(request.newBuilder().header("Accept","application/json").build());active=call;
  try(Response response=call.execute()){
   if(!response.isSuccessful())throw new IOException("Musikkilden svarede med fejl "+response.code()+". Prøv igen senere.");if(response.body()==null)throw new IOException("Musikkilden returnerede et tomt svar.");
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream input=response.body().byteStream()){byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1){if(bytes.size()+count>4*1024*1024)throw new IOException("Svaret fra musikkilden er for stort.");bytes.write(buffer,0,count);}}
   try{return new JSONObject(bytes.toString("UTF-8"));}catch(JSONException invalid){throw new IOException("Musikkilden returnerede ugyldige musikdata.",invalid);}
  }catch(UnknownHostException unavailable){throw new IOException("Musikkildens adresse kunne ikke findes. Kontrollér internetforbindelsen.",unavailable);}
  catch(SocketTimeoutException timeout){throw new IOException("Musikkilden svarede ikke i tide. Prøv igen.",timeout);}
  catch(ConnectException unavailable){throw new IOException("Der kunne ikke oprettes forbindelse til musikkilden. Prøv igen senere.",unavailable);}
  finally{if(active==call)active=null;}
 }
 void cancel(){Call call=active;if(call!=null)call.cancel();}
}
