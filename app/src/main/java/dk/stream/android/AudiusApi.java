package dk.stream.android;

import java.io.*;
import java.net.*;
import java.util.*;
import okhttp3.*;
import org.json.*;

/** Public Audius read-only API. Track IDs are namespaced to preserve older music favorites. */
final class AudiusApi {
 static final String DEFAULT="https://api.audius.co/v1/";
 static final String PREFIX="audius_";
 static final int LIMIT=20;
 private final HttpUrl base;
 private final OkHttpClient client;
 private volatile Call active;
 AudiusApi(String address){this(address,SaavnApi.HTTP);}
 AudiusApi(String address,OkHttpClient client){this.base=SaavnApi.base(address==null?DEFAULT:address);this.client=client;}
 static boolean owns(String id){return id!=null&&id.startsWith(PREFIX);}
 static final class Page {
  final List<SaavnSong> songs;final boolean hasMore;
  Page(List<SaavnSong> songs,boolean hasMore){this.songs=songs;this.hasMore=hasMore;}
  String summary(int count){return count==0?"Audius fandt ingen frit tilgængelige sange. Prøv en anden søgning.":count+" sange fra Audius · Tryk for at afspille";}
 }
 Page searchMusic(String query,int page)throws IOException{
  if(query==null||query.trim().isEmpty()||page<0||page>Integer.MAX_VALUE/LIMIT)throw new IllegalArgumentException("Indtast en søgning.");
  JSONObject response=get(route("tracks/search").addQueryParameter("query",query.trim()).addQueryParameter("limit",String.valueOf(LIMIT)).addQueryParameter("offset",String.valueOf(page*LIMIT)).build());
  try{
   JSONArray items=response.getJSONArray("data");List<SaavnSong> songs=new ArrayList<>();Set<String> seen=new HashSet<>();
   for(int i=0;i<items.length();i++){JSONObject item=items.optJSONObject(i);if(item==null||!available(item))continue;try{SaavnSong song=parse(item);if(seen.add(song.id))songs.add(song);}catch(JSONException ignored){}}
   return new Page(songs,items.length()>=LIMIT);
  }catch(JSONException invalid){throw new IOException("Audius returnerede ugyldige musikdata.",invalid);}
 }
 SaavnSong song(String reference)throws IOException{
  String id=owns(reference)?reference.substring(PREFIX.length()):"";
  if(!id.matches("[A-Za-z0-9_-]{1,100}"))throw new IOException("Ugyldig Audius-reference.");
  try{JSONObject item=get(route("tracks/"+id).build()).getJSONObject("data");if(!available(item))throw new IOException("Dette nummer er ikke frit tilgængeligt på Audius.");return parse(item);}
  catch(JSONException invalid){throw new IOException("Audius kunne ikke finde nummeret.",invalid);}
 }
 private HttpUrl.Builder route(String path){return base.newBuilder().addPathSegments(path).addQueryParameter("app_name","Stream");}
 private static boolean available(JSONObject item){return item.optBoolean("is_streamable",false)&&!item.optBoolean("is_stream_gated",false)&&!item.optBoolean("is_delete",false)&&!item.optBoolean("is_unlisted",false);}
 private SaavnSong parse(JSONObject item)throws JSONException{
  String id=item.getString("id");if(!id.matches("[A-Za-z0-9_-]{1,100}"))throw new JSONException("Invalid Audius ID");
  String title=item.getString("title").trim();if(title.isEmpty())throw new JSONException("Missing title");
  JSONObject user=item.optJSONObject("user"),art=item.optJSONObject("artwork");String image="";
  if(art!=null)for(String quality:new String[]{"1000x1000","480x480","150x150"}){HttpUrl url=HttpUrl.parse(art.optString(quality));if(url!=null){image=url.toString();break;}}
  return new SaavnSong(PREFIX+id,title,user==null?"":user.optString("name",user.optString("handle","")),"",image,route("tracks/"+id+"/stream").build().toString(),item.optLong("duration",0));
 }
 private JSONObject get(HttpUrl url)throws IOException{
  Call call=client.newCall(new Request.Builder().url(url).header("Accept","application/json").build());active=call;
  try(Response response=call.execute()){
   if(response.code()==429)throw new IOException("Audius har nået grænsen for forespørgsler. Prøv igen senere.");
   if(response.code()==401||response.code()==403)throw new IOException("Audius afviste adgang til musikken. Prøv igen senere.");
   if(!response.isSuccessful())throw new IOException("Audius svarede med fejl "+response.code()+". Prøv igen senere.");
   if(response.body()==null)throw new IOException("Audius returnerede et tomt svar.");
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream input=response.body().byteStream()){byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1){if(bytes.size()+count>4*1024*1024)throw new IOException("Svaret fra Audius er for stort.");bytes.write(buffer,0,count);}}
   try{JSONObject data=new JSONObject(bytes.toString("UTF-8"));if(data.has("error"))throw new IOException("Audius kunne ikke gennemføre forespørgslen.");return data;}catch(JSONException invalid){throw new IOException("Audius returnerede ugyldige musikdata.",invalid);}
  }catch(UnknownHostException unavailable){throw new IOException("Audius-adressen kunne ikke findes. Kontrollér internetforbindelsen.",unavailable);}
  catch(SocketTimeoutException timeout){throw new IOException("Audius svarede ikke i tide. Prøv igen.",timeout);}
  catch(ConnectException unavailable){throw new IOException("Der kunne ikke oprettes forbindelse til Audius. Prøv igen senere.",unavailable);}
  finally{if(active==call)active=null;}
 }
 void cancel(){Call call=active;if(call!=null)call.cancel();}
}
