package dk.stream.android;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.mediarouter.app.MediaRouteButton;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import com.bumptech.glide.Glide;
import com.google.android.gms.cast.*;
import com.google.android.gms.cast.framework.*;
import java.util.*;
import java.util.concurrent.*;

@androidx.media3.common.util.UnstableApi
public final class MainActivity extends AppCompatActivity {
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private Library library=new Library();
 private LibraryStore store;
 private boolean storageReadable=true,loading,favoritesMode;
 private Catalog catalog=new Catalog();
 private Library.Source activeSource;
 private List<Library.Entry> visible=new ArrayList<>();
 private Playlist.Kind currentKind=Playlist.Kind.TV;
 private final RadioButton[] kindButtons=new RadioButton[3];
 private Spinner groups;
 private EditText search;
 private TextView status,pageTitle;
 private ListView list;
 private ChannelAdapter adapter;
 private DrawerLayout drawer;
 private ExoPlayer player;
 private PlayerView video;
 private CastContext cast;
 private Playlist.Channel selected;
 private final SessionManagerListener<CastSession> sessions=new SessionManagerListener<CastSession>() {
  public void onSessionStarted(CastSession s,String id) { if(selected!=null) play(selected); }
  public void onSessionResumed(CastSession s,boolean suspended) { if(selected!=null) play(selected); }
  public void onSessionEnded(CastSession s,int error) { status.setText(R.string.cast_ended); }
  public void onSessionStarting(CastSession s) { }
  public void onSessionStartFailed(CastSession s,int error) { status.setText(R.string.cast_failed); }
  public void onSessionEnding(CastSession s) { }
  public void onSessionResuming(CastSession s,String id) { }
  public void onSessionResumeFailed(CastSession s,int error) { }
  public void onSessionSuspended(CastSession s,int reason) { }
 };

 @Override public void onCreate(Bundle state) {
  androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
  super.onCreate(state);
  store=new LibraryStore(this);
  try {
   library=store.load();
   String legacy=getPreferences(0).getString("playlist","");
   if(!legacy.isEmpty()) {
    if(library.sources.isEmpty()) { Library.Source migrated=library.addSource(Library.Source.m3u("",legacy));library.lastSourceId=migrated.id;store.save(library); }
    getPreferences(0).edit().remove("playlist").apply();
   }
  } catch(Exception e) { storageReadable=false; }
  favoritesMode=library.favoritesOnStart;
  drawer=new DrawerLayout(this);drawer.setBackgroundColor(Color.rgb(16,16,16));
  LinearLayout root=column();root.setBackgroundColor(Color.rgb(16,16,16));
  drawer.addView(root,new DrawerLayout.LayoutParams(-1,-1));setContentView(drawer);
  androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{
   androidx.core.graphics.Insets bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
   view.setPadding(bars.left+dp(12),bars.top+dp(8),bars.right+dp(12),bars.bottom+dp(8));return insets;
  });
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);root.addView(header);
  ImageButton menu=iconButton(R.drawable.ic_menu,getString(R.string.open_menu));header.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(56)));menu.setOnClickListener(v->drawer.openDrawer(androidx.core.view.GravityCompat.START));
  ImageView brand=new ImageView(this);brand.setImageResource(R.drawable.brand_icon);brand.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);header.addView(brand,new LinearLayout.LayoutParams(dp(36),dp(40)));
  TextView title=text(getString(R.string.app_name),26,Color.WHITE);title.setPadding(dp(10),0,0,0);header.addView(title,new LinearLayout.LayoutParams(0,dp(56),1));title.setGravity(Gravity.CENTER_VERTICAL);
  MediaRouteButton castButton=new MediaRouteButton(this);header.addView(castButton,new LinearLayout.LayoutParams(dp(48),dp(56)));
  createDrawer();
  androidx.activity.OnBackPressedCallback closeDrawer=new androidx.activity.OnBackPressedCallback(false) {
   @Override public void handleOnBackPressed() {drawer.closeDrawer(androidx.core.view.GravityCompat.START);}
  };
  getOnBackPressedDispatcher().addCallback(this,closeDrawer);
  drawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
   @Override public void onDrawerOpened(View view) {closeDrawer.setEnabled(true);}
   @Override public void onDrawerClosed(View view) {closeDrawer.setEnabled(false);}
  });
  pageTitle=text("",20,Color.WHITE);pageTitle.setPadding(dp(4),dp(12),0,dp(8));root.addView(pageTitle);
  status=text(getString(R.string.welcome),13,Color.LTGRAY);status.setPadding(dp(4),0,0,dp(8));root.addView(status);
  video=new PlayerView(this);root.addView(video,new LinearLayout.LayoutParams(-1,dp(180)));video.setVisibility(View.GONE);
  RadioGroup sections=new RadioGroup(this);sections.setOrientation(RadioGroup.HORIZONTAL);root.addView(sections);
  int[] labels={R.string.tv,R.string.films,R.string.series};
  for(int i=0;i<labels.length;i++) {
   RadioButton button=new RadioButton(this);button.setId(View.generateViewId());button.setText(labels[i]);kindButtons[i]=button;sections.addView(button,new RadioGroup.LayoutParams(0,-2,1));
  }
  kindButtons[0].setChecked(true);
  groups=new Spinner(this);groups.setContentDescription(getString(R.string.select_group));root.addView(groups,new LinearLayout.LayoutParams(-1,dp(48)));
  search=new EditText(this);search.setSingleLine();search.setHint(R.string.search_hint);root.addView(search);
  list=new ListView(this);list.setDivider(new android.graphics.drawable.ColorDrawable(Color.rgb(16,16,16)));list.setDividerHeight(dp(6));root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
  adapter=new ChannelAdapter();list.setAdapter(adapter);
  list.setOnItemClickListener((parent,view,position,id)->open(visible.get(position)));
  sections.setOnCheckedChangeListener((group,id)->{ for(int i=0;i<kindButtons.length;i++) if(kindButtons[i].getId()==id) currentKind=Playlist.Kind.values()[i];rebuildGroups(); });
  groups.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
   public void onItemSelected(AdapterView<?> parent,View view,int position,long id) { filter(); }
   public void onNothingSelected(AdapterView<?> parent) { }
  });
  search.addTextChangedListener(new TextWatcher() {
   public void beforeTextChanged(CharSequence s,int a,int count,int after) { }
   public void onTextChanged(CharSequence s,int start,int before,int count) { filter(); }
   public void afterTextChanged(Editable value) { }
  });
  chooseFirstKind();rebuildGroups();
  try { cast=CastContext.getSharedInstance(this);CastButtonFactory.setUpMediaRouteButton(this,castButton);cast.getSessionManager().addSessionManagerListener(sessions,CastSession.class); }
  catch(Exception e) { castButton.setVisibility(View.GONE); }
  if(!storageReadable) status.setText(R.string.storage_failed);
  else if(!favoritesMode) {
   Library.Source last=library.findSource(library.lastSourceId);
   if(last==null && !library.sources.isEmpty()) last=library.sources.get(0);
   if(last!=null) loadSource(last);
  }
 }
 private int dp(int value) {return Math.round(value*getResources().getDisplayMetrics().density);}
 private LinearLayout column() {LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);return layout;}
 private TextView text(String value,int size,int color) {TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setTextColor(color);return view;}
 private ImageButton iconButton(int icon,String description) {
  ImageButton button=new ImageButton(this);button.setImageResource(icon);button.setContentDescription(description);button.setBackgroundColor(Color.TRANSPARENT);button.setPadding(dp(12),dp(12),dp(12),dp(12));return button;
 }
 private Button button(int label,LinearLayout parent,Runnable action) {Button button=new Button(this);button.setText(label);parent.addView(button);button.setOnClickListener(v->action.run());return button;}
 private void createDrawer() {
  LinearLayout navigation=column();navigation.setBackgroundColor(Color.rgb(30,30,30));navigation.setPadding(dp(20),dp(56),dp(20),dp(20));
  navigation.addView(text(getString(R.string.app_name),28,Color.WHITE));
  button(R.string.library,navigation,()->navigate(false));button(R.string.favorites,navigation,()->navigate(true));
  button(R.string.settings,navigation,()->{drawer.closeDrawer(androidx.core.view.GravityCompat.START);showSettings();});
  DrawerLayout.LayoutParams params=new DrawerLayout.LayoutParams(dp(280),-1);params.gravity=androidx.core.view.GravityCompat.START;drawer.addView(navigation,params);
 }
 private void navigate(boolean favorites) {
  drawer.closeDrawer(androidx.core.view.GravityCompat.START);favoritesMode=favorites;search.setText("");chooseFirstKind();rebuildGroups();
  if(!favorites && catalog.items.isEmpty() && !loading) {Library.Source last=library.findSource(library.lastSourceId);if(last!=null) loadSource(last);}
 }
 private void chooseFirstKind() {
  if(favoritesMode && !library.favorites.isEmpty()) currentKind=library.favorites.get(0).channel.kind;
  else if(!favoritesMode && !catalog.items.isEmpty()) currentKind=catalog.items.get(0).kind;
  kindButtons[currentKind.ordinal()].setChecked(true);
 }
 private void rebuildGroups() {
  LinkedHashSet<String> names=new LinkedHashSet<>();
  if(favoritesMode) {for(Library.Entry entry:library.favorites) if(entry.channel.kind==currentKind && !entry.channel.group.isEmpty()) names.add(entry.channel.group);}
  else names.addAll(catalog.groups(currentKind));
  List<String> labels=new ArrayList<>();labels.add(getString(R.string.all_groups));labels.addAll(names);
  ArrayAdapter<String> choices=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,labels);choices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);groups.setAdapter(choices);filter();
 }
 private void filter() {
  if(adapter==null) return;
  pageTitle.setText(favoritesMode?getString(R.string.favorites):activeSource==null?getString(R.string.library):activeSource.name);
  String group=groups.getSelectedItemPosition()>0?String.valueOf(groups.getSelectedItem()):null;
  if(favoritesMode) visible=library.favoriteItems(currentKind,group,search.getText().toString());
  else {
   visible=new ArrayList<>();
   if(activeSource!=null) for(Playlist.Channel channel:catalog.filter(currentKind,group,search.getText().toString())) visible.add(new Library.Entry(activeSource.id,channel));
  }
  adapter.notifyDataSetChanged();
  if(!loading) {
   if(favoritesMode && library.favorites.isEmpty()) status.setText(R.string.no_favorites);
   else if(!favoritesMode && activeSource==null) status.setText(R.string.welcome);
   else status.setText(visible.isEmpty()?getString(R.string.no_section_items):getResources().getQuantityString(R.plurals.channel_count,visible.size(),visible.size()));
  }
 }
 private boolean changeLibrary(Runnable change) {
  if(!storageReadable) {status.setText(R.string.storage_failed);return false;}
  String previous;
  try {previous=library.toJson();} catch(Exception e) {status.setText(R.string.storage_failed);return false;}
  try {change.run();store.save(library);return true;}
  catch(Exception e) {
   try {library=Library.fromJson(previous);} catch(Exception ignored) {storageReadable=false;}
   status.setText(R.string.storage_failed);Toast.makeText(this,R.string.storage_failed,Toast.LENGTH_LONG).show();return false;
  }
 }
 private void showSettings() {
  LinearLayout content=column();content.setPadding(dp(20),dp(8),dp(20),dp(12));
  SwitchCompat start=new SwitchCompat(this);start.setText(R.string.start_favorites);start.setChecked(library.favoritesOnStart);start.setPadding(0,dp(12),0,dp(12));content.addView(start);
  boolean[] restoring={false};
  start.setOnCheckedChangeListener((button,checked)->{
   if(restoring[0]) return;
   if(!changeLibrary(()->library.favoritesOnStart=checked)) {restoring[0]=true;button.setChecked(library.favoritesOnStart);restoring[0]=false;}
  });
  content.addView(text(getString(R.string.storage_note),13,Color.LTGRAY));
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle(R.string.settings).setView(content).setPositiveButton(android.R.string.ok,null).create();
  button(R.string.sources,content,()->{dialog.dismiss();showSources();});dialog.show();
 }
 private void showSources() {
  if(loading) {Toast.makeText(this,R.string.wait_loading,Toast.LENGTH_SHORT).show();return;}
  List<Library.Source> sources=new ArrayList<>(library.sources);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle(R.string.sources)
   .setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,sources),(d,position)->loadSource(sources.get(position)))
   .setPositiveButton(R.string.add_m3u,(d,which)->showAddSource(false))
   .setNeutralButton(R.string.xtream_login,(d,which)->showAddSource(true)).setNegativeButton(android.R.string.cancel,null).create();
  dialog.setOnShowListener(d->dialog.getListView().setOnItemLongClickListener((parent,view,position,id)->{
   Library.Source source=sources.get(position);
   new AlertDialog.Builder(this).setTitle(R.string.remove_source).setMessage(getString(R.string.remove_source_note,source.name))
    .setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.remove_source,(confirmation,which)->{
     if(changeLibrary(()->library.removeSource(source.id))) {
      if(activeSource!=null && activeSource.id.equals(source.id)) {activeSource=null;catalog=new Catalog();}
      dialog.dismiss();rebuildGroups();showSources();
     }
    }).show();return true;
  }));dialog.show();
 }
 private EditText input(LinearLayout form,int hint,int type) {EditText field=new EditText(this);field.setHint(hint);field.setSingleLine();field.setInputType(type);form.addView(field);return field;}
 private void showAddSource(boolean xtream) {
  LinearLayout form=column();form.setPadding(dp(20),dp(8),dp(20),dp(12));
  EditText name=input(form,R.string.source_name,android.text.InputType.TYPE_CLASS_TEXT);
  EditText address=input(form,xtream?R.string.xtream_server:R.string.playlist_hint,android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);
  EditText username=xtream?input(form,R.string.xtream_user,android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS):null;
  EditText password=xtream?input(form,R.string.xtream_password,android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD):null;
  if(password!=null) password.setSaveEnabled(false);
  form.addView(text(getString(R.string.storage_note),13,Color.LTGRAY));
  ScrollView scroll=new ScrollView(this);scroll.addView(form);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle(xtream?R.string.xtream_login:R.string.add_m3u).setView(scroll).setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.save_source,null).create();
  dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   try {
    Library.Source source=xtream?Library.Source.xtream(name.getText().toString(),address.getText().toString(),username.getText().toString(),password.getText().toString()):Library.Source.m3u(name.getText().toString(),address.getText().toString());
    dialog.dismiss();loadSource(source);
   } catch(IllegalArgumentException e) {address.setError(getString(xtream?R.string.xtream_invalid:R.string.invalid_url));}
  }));dialog.show();
 }
 private void busy(boolean value) {loading=value;list.setEnabled(!value);}
 private void loadSource(Library.Source source) {
  if(loading) return;
  favoritesMode=false;busy(true);rebuildGroups();status.setText(source.xtream?R.string.loading_catalog:R.string.loading);
  worker.execute(()->{
   try {
    Catalog result=source.xtream?new XtreamClient(source.server,source.username,source.password).load():new Catalog(new PlaylistLoader().load(source.server));
    runOnUiThread(()->{
     if(isDestroyed()) return;
     Library.Source[] saved={source};
     if(changeLibrary(()->{saved[0]=library.addSource(source);library.lastSourceId=saved[0].id;library.refreshFavorites(saved[0].id,result.items);})) {
      activeSource=saved[0];catalog=result;search.setText("");chooseFirstKind();busy(false);rebuildGroups();
     }
    });
   } catch(Exception e) {runOnUiThread(()->{if(!isDestroyed()) showError(e);});}
   finally {runOnUiThread(()->{if(!isDestroyed()) {busy(false);}});}
  });
 }
 private void showError(Exception e) {
  String message;
  if(e instanceof PlaylistLoader.Failure) {
   int code=((PlaylistLoader.Failure)e).status;
   message=code==401||code==403?getString(R.string.auth_failed):code>0?getString(R.string.http_failed,code):code==-1?getString(R.string.list_too_large):getString(R.string.no_channels);
  } else if(e instanceof javax.net.ssl.SSLException) message=getString(R.string.tls_failed);
  else if(e instanceof java.io.InterruptedIOException) message=getString(R.string.timeout);
  else message=getString(R.string.network_failed);
  status.setText(message);
 }
 private void open(Library.Entry entry) {
  if(entry.channel.seriesId!=null) {
   Library.Source source=library.findSource(entry.sourceId);
   if(source!=null && source.xtream) showEpisodes(entry.channel,source);
   else status.setText(R.string.source_missing);
  } else play(entry.channel);
 }
 private void showEpisodes(Playlist.Channel series,Library.Source source) {
  busy(true);status.setText(R.string.loading_episodes);
  worker.execute(()->{
   try {
    List<Playlist.Channel> episodes=new XtreamClient(source.server,source.username,source.password).episodes(series);
    runOnUiThread(()->{
     if(isDestroyed()) return;
     new AlertDialog.Builder(this).setTitle(series.name).setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,episodes),(dialog,position)->play(episodes.get(position))).setNegativeButton(android.R.string.cancel,null).show();
     status.setText(R.string.choose_episode);
    });
   } catch(Exception e) {runOnUiThread(()->{if(!isDestroyed()) showError(e);});}
   finally {runOnUiThread(()->{if(!isDestroyed()) busy(false);});}
  });
 }
 private final class ChannelAdapter extends BaseAdapter {
  public int getCount() {return visible.size();}
  public Library.Entry getItem(int position) {return visible.get(position);}
  public long getItemId(int position) {return position;}
  public View getView(int position,View recycled,ViewGroup parent) {
   Row row;
   if(recycled==null) {
    LinearLayout layout=new LinearLayout(MainActivity.this);layout.setGravity(Gravity.CENTER_VERTICAL);layout.setPadding(dp(10),dp(8),dp(4),dp(8));layout.setBackgroundColor(Color.rgb(29,29,29));
    row=new Row();row.logo=new ImageView(MainActivity.this);row.logo.setScaleType(ImageView.ScaleType.FIT_CENTER);layout.addView(row.logo,new LinearLayout.LayoutParams(dp(48),dp(48)));
    LinearLayout labels=column();labels.setPadding(dp(12),0,dp(4),0);row.name=text("",16,Color.WHITE);row.name.setMaxLines(2);labels.addView(row.name);row.group=text("",12,Color.LTGRAY);row.group.setMaxLines(1);labels.addView(row.group);layout.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
    row.star=iconButton(R.drawable.ic_favorite_outline,"");row.star.setFocusable(false);layout.addView(row.star,new LinearLayout.LayoutParams(dp(48),dp(48)));layout.setTag(row);recycled=layout;
   } else row=(Row)recycled.getTag();
   Library.Entry entry=getItem(position);Playlist.Channel channel=entry.channel;row.name.setText(channel.name);
   Library.Source source=library.findSource(entry.sourceId);row.group.setText(favoritesMode && source!=null?(channel.group.isEmpty()?source.name:getString(R.string.group_source,channel.group,source.name)):channel.group);
   row.logo.setContentDescription(getString(R.string.logo_for,channel.name));
   Glide.with(MainActivity.this).load(channel.logoUrl.isEmpty()?null:channel.logoUrl).placeholder(R.drawable.ic_channel).error(R.drawable.ic_channel).override(dp(96),dp(96)).into(row.logo);
   boolean favorite=library.isFavorite(entry);row.star.setImageResource(favorite?R.drawable.ic_favorite:R.drawable.ic_favorite_outline);row.star.setContentDescription(getString(favorite?R.string.remove_favorite:R.string.add_favorite,channel.name));
   row.star.setOnClickListener(v->{if(changeLibrary(()->library.toggleFavorite(entry))) {if(favoritesMode) rebuildGroups();else notifyDataSetChanged();}});
   return recycled;
  }
 }
 private static final class Row {ImageView logo;TextView name,group;ImageButton star;}
 private void play(Playlist.Channel channel) {
  selected=channel;CastSession session=cast==null?null:cast.getSessionManager().getCurrentCastSession();
  if(session!=null && session.isConnected() && session.getRemoteMediaClient()!=null) {
   video.setVisibility(View.GONE);if(player!=null) player.pause();
   MediaMetadata metadata=new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);metadata.putString(MediaMetadata.KEY_TITLE,channel.name);
   if(!channel.logoUrl.isEmpty()) metadata.addImage(new com.google.android.gms.common.images.WebImage(android.net.Uri.parse(channel.logoUrl)));
   String path=Playlist.httpUrl(channel.url).encodedPath().toLowerCase(Locale.ROOT);
   String type=path.endsWith(".m3u8")?"application/x-mpegURL":path.endsWith(".mpd")?"application/dash+xml":path.endsWith(".mp4")?"video/mp4":path.endsWith(".mkv")?"video/x-matroska":path.endsWith(".avi")?"video/x-msvideo":"video/mp2t";
   MediaInfo media=new MediaInfo.Builder(channel.url).setContentType(type).setStreamType(channel.kind==Playlist.Kind.TV?MediaInfo.STREAM_TYPE_LIVE:MediaInfo.STREAM_TYPE_BUFFERED).setMetadata(metadata).build();
   session.getRemoteMediaClient().load(new MediaLoadRequestData.Builder().setMediaInfo(media).setAutoplay(true).build()).setResultCallback(result->{if(!isDestroyed()) status.setText(result.getStatus().isSuccess()?getString(R.string.casting,channel.name):getString(R.string.cast_stream_failed));});
  } else {
   video.setVisibility(View.VISIBLE);
   if(player==null) {player=new ExoPlayer.Builder(this).build();video.setPlayer(player);player.addListener(new Player.Listener(){@Override public void onPlayerError(PlaybackException e){status.setText(R.string.playback_failed);}});}
   player.setMediaItem(MediaItem.fromUri(channel.url));player.prepare();player.play();status.setText(getString(R.string.playing,channel.name));
  }
 }
 @Override protected void onStop() {super.onStop();if(player!=null){player.release();player=null;video.setPlayer(null);}}
 @Override protected void onDestroy() {if(cast!=null) cast.getSessionManager().removeSessionManagerListener(sessions,CastSession.class);worker.shutdownNow();super.onDestroy();}
}
