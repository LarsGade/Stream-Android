package dk.stream.android;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.json.*;

/** Xtream catalog order is retained; credentials are never logged. */
public final class XtreamClient {
 private final HttpUrl base;
 private final String username,password;
 private String liveExtension="m3u8";
 private final OkHttpClient client=new OkHttpClient.Builder().connectTimeout(20,TimeUnit.SECONDS).readTimeout(45,TimeUnit.SECONDS).callTimeout(90,TimeUnit.SECONDS).build();
 public XtreamClient(String server,String username,String password) {
  HttpUrl playlist=HttpUrl.get(Playlist.xtreamUrl(server,username,password));
  this.username=username.trim(); this.password=password;
  String path=playlist.encodedPath();
  base=playlist.newBuilder().encodedPath(path.substring(0,path.lastIndexOf('/')+1)).query(null).fragment(null).build();
 }
 private String request(String action,String seriesId) throws IOException {
  HttpUrl.Builder url=base.newBuilder().addPathSegment("player_api.php").addQueryParameter("username",username).addQueryParameter("password",password);
  if(action!=null) url.addQueryParameter("action",action);
  if(seriesId!=null) url.addQueryParameter("series_id",seriesId);
  try(Response response=client.newCall(new Request.Builder().url(url.build()).header("User-Agent","Stream-Android/1.1").build()).execute()) {
   if(!response.isSuccessful()) throw new PlaylistLoader.Failure(response.code());
   if(response.body()==null) throw new PlaylistLoader.Failure(0);
   try(InputStream in=response.body().byteStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream()) {
    byte[] buffer=new byte[8192]; int count;
    while((count=in.read(buffer))!=-1) { if(bytes.size()+count>PlaylistLoader.MAX_BYTES) throw new PlaylistLoader.Failure(-1); bytes.write(buffer,0,count); }
    return bytes.toString("UTF-8").replace("\uFEFF","");
   }
  }
 }
 public Catalog load() throws IOException {
  try {
   JSONObject auth=new JSONObject(request(null,null));
   JSONObject user=auth.optJSONObject("user_info");
   if(user==null || user.optInt("auth",0)!=1) throw new PlaylistLoader.Failure(401);
   if(!user.optString("status","Active").equalsIgnoreCase("Active")) throw new PlaylistLoader.Failure(403);
   JSONArray formats=user.optJSONArray("allowed_output_formats");
   if(formats!=null && formats.length()>0) {
    boolean hls=false; for(int i=0;i<formats.length();i++) if(formats.optString(i).equalsIgnoreCase("m3u8")) hls=true;
    if(!hls) liveExtension="ts";
   }
   Catalog catalog=new Catalog();
   loadSection(catalog,Playlist.Kind.TV,"get_live_categories","get_live_streams");
   loadSection(catalog,Playlist.Kind.FILM,"get_vod_categories","get_vod_streams");
   loadSection(catalog,Playlist.Kind.SERIES,"get_series_categories","get_series");
   if(catalog.items.isEmpty()) throw new PlaylistLoader.Failure(0);
   return catalog;
  } catch(JSONException e) { throw new PlaylistLoader.Failure(0); }
 }
 private void loadSection(Catalog catalog,Playlist.Kind kind,String categoriesAction,String streamsAction) throws IOException,JSONException {
  JSONArray categories=new JSONArray(request(categoriesAction,null));
  Map<String,String> names=new LinkedHashMap<>();
  for(int i=0;i<categories.length();i++) {
   JSONObject category=categories.getJSONObject(i);
   String name=category.optString("category_name",""); names.put(category.optString("category_id"),name); catalog.addGroup(kind,name);
  }
  JSONArray streams=new JSONArray(request(streamsAction,null));
  for(int i=0;i<streams.length();i++) {
   JSONObject item=streams.getJSONObject(i);
   String id=item.optString(kind==Playlist.Kind.SERIES?"series_id":"stream_id","");
   if(!id.matches("[0-9]+")) continue;
   String group=names.get(item.optString("category_id")); if(group==null) group="";
   String url=kind==Playlist.Kind.SERIES?"":streamUrl(kind,id,kind==Playlist.Kind.TV?liveExtension:item.optString("container_extension","mp4"));
   String image=item.optString("stream_icon","");if(image.isEmpty()) image=item.optString("cover","");
   catalog.add(new Playlist.Channel(item.optString("name",id),url,group,kind,kind==Playlist.Kind.SERIES?id:null,logo(image)));
  }
 }
 private String logo(String value) {
  return LogoUrls.resolve(value,base);
 }
 private String streamUrl(Playlist.Kind kind,String id,String extension) {
  if(!extension.matches("[a-zA-Z0-9]+")) extension="mp4";
  return base.newBuilder().addPathSegment(kind==Playlist.Kind.TV?"live":kind==Playlist.Kind.FILM?"movie":"series")
   .addPathSegment(username).addPathSegment(password).addPathSegment(id+"."+extension).build().toString();
 }
 public List<Playlist.Channel> episodes(Playlist.Channel series) throws IOException {
  try {
   JSONObject response=new JSONObject(request("get_series_info",series.seriesId));
   JSONObject episodes=response.optJSONObject("episodes");
   if(episodes==null) throw new PlaylistLoader.Failure(0);
   LinkedHashSet<String> seasons=new LinkedHashSet<>();
   JSONArray seasonList=response.optJSONArray("seasons");
   if(seasonList!=null) for(int i=0;i<seasonList.length();i++) seasons.add(seasonList.getJSONObject(i).optString("season_number"));
   Iterator<String> keys=episodes.keys(); while(keys.hasNext()) seasons.add(keys.next());
   List<Playlist.Channel> result=new ArrayList<>();
   for(String season:seasons) {
    JSONArray entries=episodes.optJSONArray(season); if(entries==null) continue;
    for(int i=0;i<entries.length();i++) {
     JSONObject episode=entries.getJSONObject(i); String id=episode.optString("id",""); if(!id.matches("[0-9]+")) continue;
     String title=episode.optString("title",series.name);
     String label="S"+season+" · E"+episode.optString("episode_num",String.valueOf(i+1))+" · "+title;
     result.add(new Playlist.Channel(label,streamUrl(Playlist.Kind.SERIES,id,episode.optString("container_extension","mp4")),series.group,Playlist.Kind.SERIES,null,series.logoUrl));
    }
   }
   if(result.isEmpty()) throw new PlaylistLoader.Failure(0);
   return result;
  } catch(JSONException e) { throw new PlaylistLoader.Failure(0); }
 }
}
