package dk.stream.android;

import android.os.Bundle;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.mediarouter.app.MediaRouteButton;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import com.google.android.gms.cast.*;
import com.google.android.gms.cast.framework.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

@androidx.media3.common.util.UnstableApi
public final class MainActivity extends AppCompatActivity {
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private List<Playlist.Channel> channels=new ArrayList<>(), visible=new ArrayList<>();
 private EditText link, search; private TextView status; private Button load; private ListView list;
 private ExoPlayer player; private PlayerView video; private CastContext cast; private Playlist.Channel selected;
 private final SessionManagerListener<CastSession> sessions=new SessionManagerListener<CastSession>() {
  public void onSessionStarted(CastSession s,String id) { if(selected!=null) play(selected); }
  public void onSessionResumed(CastSession s,boolean suspended) { if(selected!=null) play(selected); }
  public void onSessionEnded(CastSession s,int error) { status.setText(getString(R.string.cast_ended)); }
  public void onSessionStarting(CastSession s) {} public void onSessionStartFailed(CastSession s,int e) { status.setText(getString(R.string.cast_failed)); }
  public void onSessionEnding(CastSession s) {} public void onSessionResuming(CastSession s,String id) {}
  public void onSessionResumeFailed(CastSession s,int e) {} public void onSessionSuspended(CastSession s,int reason) {}
 };
 @Override public void onCreate(Bundle state) {
  androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
  super.onCreate(state);
  LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(16),dp(16),dp(12)); root.setBackgroundColor(Color.rgb(16,24,32));
  setContentView(root);
  androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{
   androidx.core.graphics.Insets bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
   v.setPadding(bars.left+dp(16),bars.top+dp(12),bars.right+dp(16),bars.bottom+dp(8)); return insets;
  });
  LinearLayout header=new LinearLayout(this); TextView title=new TextView(this); title.setText(getString(R.string.app_name)); title.setTextSize(28); title.setTextColor(Color.WHITE); header.addView(title,new LinearLayout.LayoutParams(0,dp(56),1));
  MediaRouteButton castButton=new MediaRouteButton(this); header.addView(castButton,new LinearLayout.LayoutParams(dp(56),dp(56))); root.addView(header);
  link=new EditText(this); link.setSingleLine(); link.setInputType(17); link.setHint(getString(R.string.playlist_hint)); link.setText(getPreferences(0).getString("playlist","")); root.addView(link);
  load=new Button(this); load.setText(getString(R.string.load)); root.addView(load);
  status=new TextView(this); status.setTextColor(Color.WHITE); status.setText(getString(R.string.welcome)); root.addView(status);
  video=new PlayerView(this); root.addView(video,new LinearLayout.LayoutParams(-1,dp(200)));
  search=new EditText(this); search.setSingleLine(); search.setHint(getString(R.string.search_hint)); root.addView(search);
  list=new ListView(this); root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
  list.setOnItemClickListener((a,v,p,id)->play(visible.get(p)));
  search.addTextChangedListener(new TextWatcher(){ public void beforeTextChanged(CharSequence s,int a,int c,int f){} public void onTextChanged(CharSequence s,int a,int b,int c){filter();} public void afterTextChanged(Editable e){} });
  load.setOnClickListener(v->load());
  try { cast=CastContext.getSharedInstance(this); CastButtonFactory.setUpMediaRouteButton(this,castButton); cast.getSessionManager().addSessionManagerListener(sessions,CastSession.class); }
  catch(Exception e) { castButton.setVisibility(View.GONE); status.setText(getString(R.string.cast_unavailable)); }
 }
 private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
 private void filter() {
  String q=search.getText().toString().toLowerCase(Locale.ROOT); visible=new ArrayList<>();
  for(var c:channels) if((c.name+" "+c.group).toLowerCase(Locale.ROOT).contains(q)) visible.add(c);
  list.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,visible));
 }
 private void load() {
  final String url=link.getText().toString().trim();
  try { Playlist.httpUri(url); } catch(Exception e){status.setText(getString(R.string.invalid_url)); return;}
  load.setEnabled(false); status.setText(getString(R.string.loading));
  worker.execute(()->{
   try {
    HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection(); connection.setConnectTimeout(15000); connection.setReadTimeout(20000);
    String text;
    try {
     if(connection.getResponseCode()!=200) throw new IOException("HTTP-fejl");
     try(InputStream in=connection.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
      byte[] buffer=new byte[8192]; int n;
      while((n=in.read(buffer))!=-1) { if(out.size()+n>10*1024*1024) throw new IOException("Listen er for stor"); out.write(buffer,0,n); }
      text=out.toString("UTF-8");
     }
     final List<Playlist.Channel> parsed=Playlist.parse(text,connection.getURL().toString());
     if(parsed.isEmpty() || text.trim().startsWith("<")) throw new IOException("Ingen kanaler");
     runOnUiThread(()->{ if(isDestroyed()) return; channels=parsed; filter(); status.setText(getResources().getQuantityString(R.plurals.channel_count,channels.size(),channels.size())); getPreferences(0).edit().putString("playlist",url).apply(); });
    } finally { connection.disconnect(); }
   } catch(Exception e) { runOnUiThread(()->{if(!isDestroyed()) status.setText(getString(R.string.load_failed));}); }
   finally { runOnUiThread(()->{if(!isDestroyed()) load.setEnabled(true);}); }
  });
 }
 private void play(Playlist.Channel channel) {
  selected=channel;
  CastSession session=cast==null?null:cast.getSessionManager().getCurrentCastSession();
  if(session!=null && session.isConnected() && session.getRemoteMediaClient()!=null) {
   if(player!=null) player.pause();
   MediaMetadata metadata=new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE); metadata.putString(MediaMetadata.KEY_TITLE,channel.name);
   String path=URI.create(channel.url).getPath().toLowerCase(Locale.ROOT);
   String type=path.endsWith(".m3u8")?"application/x-mpegURL":path.endsWith(".mpd")?"application/dash+xml":path.endsWith(".mp4")?"video/mp4":"video/mp2t";
   MediaInfo media=new MediaInfo.Builder(channel.url).setContentType(type).setStreamType(MediaInfo.STREAM_TYPE_LIVE).setMetadata(metadata).build();
   session.getRemoteMediaClient().load(new MediaLoadRequestData.Builder().setMediaInfo(media).setAutoplay(true).build()).setResultCallback(result->{ if(!isDestroyed()) status.setText(result.getStatus().isSuccess()?getString(R.string.casting,channel.name):getString(R.string.cast_stream_failed)); });
  } else {
   if(player==null) { player=new ExoPlayer.Builder(this).build(); video.setPlayer(player); player.addListener(new Player.Listener(){ @Override public void onPlayerError(PlaybackException e){status.setText(getString(R.string.playback_failed));} }); }
   player.setMediaItem(MediaItem.fromUri(channel.url)); player.prepare(); player.play(); status.setText(getString(R.string.playing,channel.name));
  }
 }
 @Override protected void onStop(){super.onStop(); if(player!=null){player.release(); player=null; video.setPlayer(null);} }
 @Override protected void onDestroy(){if(cast!=null) cast.getSessionManager().removeSessionManagerListener(sessions,CastSession.class); worker.shutdownNow(); super.onDestroy();}
}
