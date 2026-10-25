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
 private Catalog catalog=new Catalog();
 private List<Playlist.Channel> visible=new ArrayList<>();
 private Playlist.Kind currentKind=Playlist.Kind.TV;
 private Spinner groups;
 private final RadioButton[] kindButtons=new RadioButton[3];
 private XtreamClient sourceClient;
 private LinearLayout sources;
 private EditText link, search; private TextView status; private Button load, xtream; private ListView list;
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
  Button sourceToggle=new Button(this); sourceToggle.setText(R.string.change_source); root.addView(sourceToggle);
  sources=new LinearLayout(this); sources.setOrientation(LinearLayout.VERTICAL); root.addView(sources);
  sourceToggle.setOnClickListener(v->sources.setVisibility(sources.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));
  link=new EditText(this); link.setSingleLine(); link.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI); link.setHint(R.string.playlist_hint); link.setText(getPreferences(0).getString("playlist","")); sources.addView(link);
  LinearLayout actions=new LinearLayout(this); sources.addView(actions);
  load=new Button(this); load.setText(R.string.load); actions.addView(load,new LinearLayout.LayoutParams(0,-2,1));
  xtream=new Button(this); xtream.setText(R.string.xtream_login); actions.addView(xtream,new LinearLayout.LayoutParams(0,-2,1)); xtream.setOnClickListener(v->showXtream());
  status=new TextView(this); status.setTextColor(Color.WHITE); status.setText(getString(R.string.welcome)); root.addView(status);
  video=new PlayerView(this); root.addView(video,new LinearLayout.LayoutParams(-1,dp(200))); video.setVisibility(View.GONE);
  RadioGroup sections=new RadioGroup(this); sections.setOrientation(RadioGroup.HORIZONTAL); root.addView(sections);
  int[] labels={R.string.tv,R.string.films,R.string.series};
  for(int i=0;i<kindButtons.length;i++) { RadioButton button=new RadioButton(this); button.setId(View.generateViewId()); button.setText(labels[i]); kindButtons[i]=button; sections.addView(button,new RadioGroup.LayoutParams(0,-2,1)); }
  kindButtons[0].setChecked(true);
  groups=new Spinner(this); root.addView(groups);
  search=new EditText(this); search.setSingleLine(); search.setHint(getString(R.string.search_hint)); root.addView(search);
  list=new ListView(this); root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
  list.setOnItemClickListener((a,v,p,id)->{
   Playlist.Channel item=visible.get(p);
   if(item.seriesId!=null && sourceClient!=null) showEpisodes(item); else play(item);
  });
  sections.setOnCheckedChangeListener((group,id)->{ for(int i=0;i<kindButtons.length;i++) if(kindButtons[i].getId()==id) currentKind=Playlist.Kind.values()[i]; rebuildGroups(); });
  groups.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
   public void onItemSelected(AdapterView<?> parent,View view,int position,long id) { filter(); }
   public void onNothingSelected(AdapterView<?> parent) { }
  });
  rebuildGroups();
  search.addTextChangedListener(new TextWatcher(){ public void beforeTextChanged(CharSequence s,int a,int c,int f){} public void onTextChanged(CharSequence s,int a,int b,int c){filter();} public void afterTextChanged(Editable e){} });
  load.setOnClickListener(v->load());
  try { cast=CastContext.getSharedInstance(this); CastButtonFactory.setUpMediaRouteButton(this,castButton); cast.getSessionManager().addSessionManagerListener(sessions,CastSession.class); }
  catch(Exception e) { castButton.setVisibility(View.GONE); status.setText(getString(R.string.cast_unavailable)); }
 }
 private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
 private void rebuildGroups() {
  List<String> names=new ArrayList<>(); names.add(getString(R.string.all_groups)); names.addAll(catalog.groups(currentKind));
  ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,names); adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); groups.setAdapter(adapter); filter();
 }
 private void filter() {
  int position=groups.getSelectedItemPosition();
  String group=position>0?String.valueOf(groups.getSelectedItem()):null;
  visible=catalog.filter(currentKind,group,search.getText().toString());
  list.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,visible));
  if(!catalog.items.isEmpty()) status.setText(visible.isEmpty()?getString(R.string.no_section_items):getResources().getQuantityString(R.plurals.channel_count,visible.size(),visible.size()));
 }
 private void showCatalog(Catalog result,XtreamClient client) {
  catalog=result; sourceClient=client; sources.setVisibility(View.GONE); search.setText("");
  if(!result.items.isEmpty()) { currentKind=result.items.get(0).kind; kindButtons[currentKind.ordinal()].setChecked(true); }
  rebuildGroups();
 }
 private void busy(boolean loading) { load.setEnabled(!loading); xtream.setEnabled(!loading); list.setEnabled(!loading); }
 private void showError(Exception e) {
  final String message;
  if(e instanceof PlaylistLoader.Failure) {
   int code=((PlaylistLoader.Failure)e).status;
   message=code==401 || code==403?getString(R.string.auth_failed):code>0?getString(R.string.http_failed,code):code==-1?getString(R.string.list_too_large):getString(R.string.no_channels);
  } else if(e instanceof javax.net.ssl.SSLException) message=getString(R.string.tls_failed);
  else if(e instanceof java.io.InterruptedIOException) message=getString(R.string.timeout);
  else message=getString(R.string.network_failed);
  status.setText(message);
 }
 private void loadXtream(XtreamClient client) {
  busy(true); status.setText(R.string.loading_catalog);
  worker.execute(()->{
   try { Catalog result=client.load(); runOnUiThread(()->{if(!isDestroyed()) showCatalog(result,client);}); }
   catch(Exception e) { runOnUiThread(()->{if(!isDestroyed()) showError(e);}); }
   finally { runOnUiThread(()->{if(!isDestroyed()) busy(false);}); }
  });
 }
 private void showEpisodes(Playlist.Channel series) {
  XtreamClient client=sourceClient; busy(true); status.setText(R.string.loading_episodes);
  worker.execute(()->{
   try {
    List<Playlist.Channel> episodes=client.episodes(series);
    runOnUiThread(()->{
     if(isDestroyed()) return;
     new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(series.name)
      .setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,episodes),(dialog,position)->play(episodes.get(position)))
      .setNegativeButton(android.R.string.cancel,null).show();
     status.setText(R.string.choose_episode);
    });
   } catch(Exception e) { runOnUiThread(()->{if(!isDestroyed()) showError(e);}); }
   finally { runOnUiThread(()->{if(!isDestroyed()) busy(false);}); }
  });
 }
 private void showXtream() {
  LinearLayout form=new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(20),0,dp(20),0);
  EditText server=new EditText(this); server.setHint(R.string.xtream_server); server.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI); server.setSingleLine(); form.addView(server);
  EditText username=new EditText(this); username.setHint(R.string.xtream_user); username.setSingleLine(); username.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS); form.addView(username);
  EditText password=new EditText(this); password.setHint(R.string.xtream_password); password.setSingleLine(); password.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD); password.setSaveEnabled(false); form.addView(password);
  TextView note=new TextView(this); note.setText(R.string.xtream_note); form.addView(note);
  androidx.appcompat.app.AlertDialog dialog=new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(R.string.xtream_login).setView(form)
   .setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.load,null).create();
  dialog.setOnShowListener(d->dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   try {
    XtreamClient client=new XtreamClient(server.getText().toString(),username.getText().toString(),password.getText().toString());
    dialog.dismiss(); loadXtream(client);
   } catch(IllegalArgumentException e) { server.setError(getString(R.string.xtream_invalid)); }
  })); dialog.show();
 }
 private void load() {
  try { loadUrl(Playlist.httpUrl(link.getText().toString()).toString(),true); }
  catch(IllegalArgumentException e){status.setText(R.string.invalid_url);}
 }
 private void loadUrl(String url,boolean remember) {
  busy(true); status.setText(R.string.loading);
  worker.execute(()->{
   try {
    List<Playlist.Channel> parsed=new PlaylistLoader().load(url);
    runOnUiThread(()->{ if(isDestroyed()) return; showCatalog(new Catalog(parsed),null);
     if(remember) { link.setText(url); getPreferences(0).edit().putString("playlist",url).apply(); }
    });
   } catch(Exception e) { runOnUiThread(()->{if(!isDestroyed()) showError(e);}); }
   finally { runOnUiThread(()->{if(!isDestroyed()) busy(false);}); }
  });
 }
 private void play(Playlist.Channel channel) {
  selected=channel;
  CastSession session=cast==null?null:cast.getSessionManager().getCurrentCastSession();
  if(session!=null && session.isConnected() && session.getRemoteMediaClient()!=null) {
   video.setVisibility(View.GONE);
   if(player!=null) player.pause();
   MediaMetadata metadata=new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE); metadata.putString(MediaMetadata.KEY_TITLE,channel.name);
   String path=URI.create(channel.url).getPath().toLowerCase(Locale.ROOT);
   String type=path.endsWith(".m3u8")?"application/x-mpegURL":path.endsWith(".mpd")?"application/dash+xml":path.endsWith(".mp4")?"video/mp4":path.endsWith(".mkv")?"video/x-matroska":path.endsWith(".avi")?"video/x-msvideo":"video/mp2t";
   MediaInfo media=new MediaInfo.Builder(channel.url).setContentType(type).setStreamType(channel.kind==Playlist.Kind.TV?MediaInfo.STREAM_TYPE_LIVE:MediaInfo.STREAM_TYPE_BUFFERED).setMetadata(metadata).build();
   session.getRemoteMediaClient().load(new MediaLoadRequestData.Builder().setMediaInfo(media).setAutoplay(true).build()).setResultCallback(result->{ if(!isDestroyed()) status.setText(result.getStatus().isSuccess()?getString(R.string.casting,channel.name):getString(R.string.cast_stream_failed)); });
  } else {
   video.setVisibility(View.VISIBLE);
   if(player==null) { player=new ExoPlayer.Builder(this).build(); video.setPlayer(player); player.addListener(new Player.Listener(){ @Override public void onPlayerError(PlaybackException e){status.setText(getString(R.string.playback_failed));} }); }
   player.setMediaItem(MediaItem.fromUri(channel.url)); player.prepare(); player.play(); status.setText(getString(R.string.playing,channel.name));
  }
 }
 @Override protected void onStop(){super.onStop(); if(player!=null){player.release(); player=null; video.setPlayer(null);} }
 @Override protected void onDestroy(){if(cast!=null) cast.getSessionManager().removeSessionManagerListener(sessions,CastSession.class); worker.shutdownNow(); super.onDestroy();}
}
