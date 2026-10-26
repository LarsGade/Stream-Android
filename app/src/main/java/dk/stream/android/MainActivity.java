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
 private final ExecutorService epgWorker=Executors.newSingleThreadExecutor();
 private EpgRepository epgRepository;
 private final Map<String,EpgData> epgData=new HashMap<>();
 private final Set<String> epgPending=new HashSet<>();
 private final Map<String,Long> epgAttempted=new HashMap<>();
 private boolean updatingAll,radioMode,favoritesPage,choosingFavoriteTab;
 private RadioGroup favoriteTabs;
 private RadioButton favoriteTv,favoriteRadio;
 private RadioPanel radioPanel;
 private final Runnable epgTick=new Runnable() {
  public void run() {if(isDestroyed()) return;if(adapter!=null) adapter.notifyDataSetChanged();loadAssignedGuides(false);list.postDelayed(this,60000);}
 };
 private Library library=new Library();
 private LibraryPersistence store;
 private boolean storageReadable=true,loading,favoritesMode;
 private Catalog catalog=new Catalog();
 private Library.Source activeSource;
 private List<Library.Entry> visible=new ArrayList<>();
 private Playlist.Kind currentKind=Playlist.Kind.TV;
 private final RadioButton[] kindButtons=new RadioButton[3];
 private Spinner groups;
 private RadioGroup sections;
 private EditText search;
 private TextView status,pageTitle;
 private FavoriteListView list;
 private ChannelAdapter adapter;
 private DrawerLayout drawer;
 private ExoPlayer player;
 private PlayerView video;
 private PlaybackPanel playbackPanel;
 private LinearLayout content;
 private FrameLayout fullscreenHost;
 private boolean fullscreen;
 private int panelIndex,previousOrientation,previousCutoutMode;
 private androidx.media3.session.MediaSession videoSession;
 private boolean backgroundPlayback;
 private int videoArtworkGeneration;
 private final ExecutorService videoArtworkWorker=Executors.newSingleThreadExecutor();
 private static final String VIDEO_TOGGLE="dk.stream.android.VIDEO_TOGGLE",VIDEO_STOP="dk.stream.android.VIDEO_STOP";
 private final androidx.activity.result.ActivityResultLauncher<String> notificationPermission=registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),granted->{});
 private final android.content.BroadcastReceiver videoTransport=new android.content.BroadcastReceiver() {
  @Override public void onReceive(android.content.Context context,android.content.Intent intent) {if(VIDEO_STOP.equals(intent.getAction())) stopPlaybackState();else if(VIDEO_TOGGLE.equals(intent.getAction())) togglePlaybackState();}
 };
 private long resumePosition;
 private boolean resumeLocalPlayback,pipLayout,pipWasFullscreen;
 private androidx.activity.OnBackPressedCallback miniBack;
 private LocalChannelLogos localLogos;
 private androidx.activity.OnBackPressedCallback fullscreenBack;
 private com.google.android.gms.cast.framework.media.RemoteMediaClient watchedRemote;
 private final com.google.android.gms.cast.framework.media.RemoteMediaClient.Callback remoteUpdates=new com.google.android.gms.cast.framework.media.RemoteMediaClient.Callback() {
  @Override public void onStatusUpdated() {if(playbackPanel!=null) playbackPanel.refresh();if(adapter!=null) adapter.notifyDataSetChanged();}
  @Override public void onMetadataUpdated() {if(playbackPanel!=null) playbackPanel.refresh();}
 };
 private CastContext cast;
 private Playlist.Channel selected;
 private final SessionManagerListener<CastSession> sessions=new SessionManagerListener<CastSession>() {
  public void onSessionStarted(CastSession s,String id) { if(selected!=null) play(selected); }
  public void onSessionResumed(CastSession s,boolean suspended) { if(selected!=null) play(selected); }
  public void onSessionEnded(CastSession s,int error) {watchRemote();if(playbackPanel!=null) playbackPanel.refresh();if(adapter!=null) adapter.notifyDataSetChanged();status.setText(R.string.cast_ended); }
  public void onSessionStarting(CastSession s) { }
  public void onSessionStartFailed(CastSession s,int error) { status.setText(R.string.cast_failed); }
  public void onSessionEnding(CastSession s) { }
  public void onSessionResuming(CastSession s,String id) { }
  public void onSessionResumeFailed(CastSession s,int error) { }
  public void onSessionSuspended(CastSession s,int reason) {if(adapter!=null) adapter.notifyDataSetChanged();}
 };

 @Override public void onCreate(Bundle state) {
  androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
  super.onCreate(state);androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
  android.content.IntentFilter transport=new android.content.IntentFilter(VIDEO_TOGGLE);transport.addAction(VIDEO_STOP);androidx.core.content.ContextCompat.registerReceiver(this,videoTransport,transport,androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
  epgRepository=new EpgRepository(this);store=new LibraryStore(this);localLogos=LocalChannelLogos.load(this);setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
  try {
   library=store.load();
   String legacy=getPreferences(0).getString("playlist","");
   if(!legacy.isEmpty()) {
    if(library.sources.isEmpty()) { Library.Source migrated=library.addSource(Library.Source.m3u("",legacy));library.lastSourceId=migrated.id;store.save(library); }
    getPreferences(0).edit().remove("playlist").apply();
   }
  } catch(Exception e) { storageReadable=false; }
  favoritesPage=library.favoritesOnStart;favoritesMode=favoritesPage;
  drawer=new DrawerLayout(this);drawer.setBackgroundColor(Color.rgb(16,16,16));
  LinearLayout root=column();root.setBackgroundColor(Color.rgb(16,16,16));
  content=root;drawer.addView(root,new DrawerLayout.LayoutParams(-1,-1));
  FrameLayout screen=new FrameLayout(this);screen.addView(drawer,new FrameLayout.LayoutParams(-1,-1));
  fullscreenHost=new FrameLayout(this);fullscreenHost.setBackgroundColor(Color.BLACK);fullscreenHost.setVisibility(View.GONE);screen.addView(fullscreenHost,new FrameLayout.LayoutParams(-1,-1));setContentView(screen);
  miniBack=new androidx.activity.OnBackPressedCallback(false) {@Override public void handleOnBackPressed() {if(!enterMiniPlayer()) finish();}};
  getOnBackPressedDispatcher().addCallback(this,miniBack);
  fullscreenBack=new androidx.activity.OnBackPressedCallback(false) {@Override public void handleOnBackPressed() {exitFullscreen();}};
  getOnBackPressedDispatcher().addCallback(this,fullscreenBack);
  androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{
   androidx.core.graphics.Insets bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
   view.setPadding(bars.left+dp(12),bars.top+dp(8),bars.right+dp(12),bars.bottom+dp(8));return insets;
  });
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);root.addView(header);
  ImageButton menu=iconButton(R.drawable.ic_menu,getString(R.string.open_menu));header.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(56)));menu.setOnClickListener(v->drawer.openDrawer(androidx.core.view.GravityCompat.START));
  ImageView brand=new ImageView(this);brand.setImageResource(R.drawable.header_banner);brand.setScaleX(0.9f);brand.setScaleY(0.9f);brand.setScaleType(ImageView.ScaleType.FIT_CENTER);brand.setAdjustViewBounds(true);brand.setContentDescription(getString(R.string.app_name));brand.setPadding(dp(8),dp(4),dp(8),dp(4));header.addView(brand,new LinearLayout.LayoutParams(0,dp(64),1));
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
  status=text("",13,Color.LTGRAY);status.setVisibility(View.GONE);status.setPadding(dp(4),0,0,dp(8));root.addView(status);
  playbackPanel=new PlaybackPanel(this,new PlaybackPanel.Actions() {
   public void togglePlayback() {togglePlaybackState();}
   public void stopPlayback() {stopPlaybackState();}
   public void toggleFullscreen() {if(fullscreen) exitFullscreen();else enterFullscreen();}
   public void volumeFailed() {Toast.makeText(MainActivity.this,R.string.volume_failed,Toast.LENGTH_SHORT).show();}
   public boolean playing() {CastSession session=connectedCast();return session!=null && session.getRemoteMediaClient()!=null?session.getRemoteMediaClient().isPlaying():player!=null && player.getPlayWhenReady();}
   public boolean fullscreen() {return fullscreen;}
   public CastSession castSession() {return connectedCast();}
  });
  video=playbackPanel.video;video.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->updateMiniBack());root.addView(playbackPanel,new LinearLayout.LayoutParams(-1,-2));panelIndex=root.indexOfChild(playbackPanel);
  sections=new RadioGroup(this);sections.setOrientation(RadioGroup.HORIZONTAL);root.addView(sections);
  int[] labels={R.string.tv,R.string.films,R.string.series};
  for(int i=0;i<labels.length;i++) {
   RadioButton button=new RadioButton(this);button.setId(View.generateViewId());button.setText(labels[i]);kindButtons[i]=button;sections.addView(button,new RadioGroup.LayoutParams(0,-2,1));
  }
  kindButtons[0].setChecked(true);
  favoriteTabs=new RadioGroup(this);favoriteTabs.setOrientation(RadioGroup.HORIZONTAL);favoriteTabs.setGravity(Gravity.CENTER);root.addView(favoriteTabs);
  favoriteTv=new RadioButton(this);favoriteTv.setId(View.generateViewId());favoriteTv.setText(R.string.tv);favoriteTabs.addView(favoriteTv,new RadioGroup.LayoutParams(-2,-2));
  favoriteRadio=new RadioButton(this);favoriteRadio.setId(View.generateViewId());favoriteRadio.setText(R.string.radio);favoriteTabs.addView(favoriteRadio,new RadioGroup.LayoutParams(-2,-2));favoriteTv.setChecked(true);
  groups=new Spinner(this);groups.setContentDescription(getString(R.string.select_group));root.addView(groups,new LinearLayout.LayoutParams(-1,dp(48)));
  search=new EditText(this);search.setSingleLine();search.setHint(R.string.search_hint);root.addView(search);
  list=new FavoriteListView(this);list.setDivider(new android.graphics.drawable.ColorDrawable(Color.rgb(16,16,16)));list.setDividerHeight(dp(6));root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
  radioPanel=new RadioPanel(this,()->library,this::changeLibrary,()->{requestNotificationPermission();if(selected!=null) stopPlaybackState();});radioPanel.setVisibility(View.GONE);root.addView(radioPanel,new LinearLayout.LayoutParams(-1,0,1));
  TextView credit=text(getString(R.string.developer_credit),11,Color.rgb(136,136,136));credit.setGravity(Gravity.CENTER);credit.setPadding(0,dp(6),0,dp(4));root.addView(credit,new LinearLayout.LayoutParams(-1,-2));
  adapter=new ChannelAdapter();list.setAdapter(adapter);
  list.setFavoriteListener(new FavoriteListView.Listener() {
   public boolean enabled() {return favoritesMode&&!loading&&!updatingAll&&list.isEnabled();}
   public void showActions(int position,View anchor) {
    if(!favoritesMode||position<0||position>=visible.size()) return;
    Library.Entry entry=visible.get(position);PopupMenu menu=new PopupMenu(MainActivity.this,anchor);
    menu.getMenu().add(R.string.remove_from_favorites).setOnMenuItemClickListener(item->{if(changeLibrary(()->library.removeFavorite(entry))) filter();return true;});menu.show();
   }
   public void move(int from,int to) {if(from>=0&&from<visible.size()&&to>=0&&to<visible.size()) {Library.Entry entry=visible.remove(from);visible.add(to,entry);adapter.notifyDataSetChanged();}}
   public void commit() {List<Library.Entry> ordered=new ArrayList<>(visible);changeLibrary(()->library.reorderFavorites(ordered));filter();}
   public void cancel() {filter();}
  });
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
  favoriteTabs.setOnCheckedChangeListener((group,id)->{if(choosingFavoriteTab) return;boolean radio=id==favoriteRadio.getId();if(changeLibrary(()->library.favoritesRadio=radio)) showFavoriteKind(radio);else showFavoriteKind(library.favoritesRadio);});
  chooseFirstKind();rebuildGroups();loadAssignedGuides(false);if(favoritesPage && library.favoritesRadio) showFavoriteKind(true);
  try { cast=CastContext.getSharedInstance(this);CastButtonFactory.setUpMediaRouteButton(this,castButton);cast.getSessionManager().addSessionManagerListener(sessions,CastSession.class); }
  catch(Exception e) { castButton.setVisibility(View.GONE); }
  if(!storageReadable) {status.setVisibility(View.VISIBLE);status.setText(R.string.storage_failed);}
  else if(!favoritesMode) {
   Library.Source last=library.findSource(library.lastSourceId);
   if(last==null && !library.sources.isEmpty()) last=library.sources.get(0);
   if(last!=null) loadSource(last);
  }
  restoreLastPlayback();if(getIntent().getBooleanExtra("openRadio",false)) openRadio(true);
 }
 @Override protected void onNewIntent(android.content.Intent intent) {super.onNewIntent(intent);setIntent(intent);if(intent.getBooleanExtra("openRadio",false)) openRadio(true);}
 private int dp(int value) {return Math.round(value*getResources().getDisplayMetrics().density);}
 private LinearLayout column() {LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);return layout;}
 private TextView text(String value,int size,int color) {TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setTextColor(color);return view;}
 private ImageButton iconButton(int icon,String description) {
  ImageButton button=new ImageButton(this);button.setImageResource(icon);button.setContentDescription(description);button.setBackgroundColor(Color.TRANSPARENT);button.setPadding(dp(12),dp(12),dp(12),dp(12));return button;
 }
 private Button button(int label,LinearLayout parent,Runnable action) {Button button=new Button(this);button.setText(label);parent.addView(button);button.setOnClickListener(v->action.run());return button;}
 private void createDrawer() {
  LinearLayout navigation=column();navigation.setBackgroundColor(Color.rgb(30,30,30));navigation.setPadding(dp(20),dp(56),dp(20),dp(20));
  button(R.string.library,navigation,this::openStreamingService);button(R.string.radio,navigation,()->openRadio(false));
  button(R.string.favorites,navigation,()->navigate(true));
  SwitchCompat start=new SwitchCompat(this);start.setText(R.string.start_favorites);start.setChecked(library.favoritesOnStart);start.setPadding(0,dp(12),0,dp(12));navigation.addView(start);
  boolean[] restoring={false};start.setOnCheckedChangeListener((button,checked)->{
   if(restoring[0]) return;
   if(!changeLibrary(()->library.favoritesOnStart=checked)) {restoring[0]=true;button.setChecked(library.favoritesOnStart);restoring[0]=false;}
  });
  playbackPreference(navigation,R.string.auto_play_last,false);playbackPreference(navigation,R.string.mini_player,true);
  button(R.string.sources,navigation,()->{drawer.closeDrawer(androidx.core.view.GravityCompat.START);showSources();});
  button(R.string.tv_guide,navigation,()->{drawer.closeDrawer(androidx.core.view.GravityCompat.START);showGuides();});
  button(R.string.update_all,navigation,()->{drawer.closeDrawer(androidx.core.view.GravityCompat.START);updateAll();});
  button(R.string.reset_app,navigation,()->{drawer.closeDrawer(androidx.core.view.GravityCompat.START);confirmReset();});
  ScrollView menu=new ScrollView(this);menu.addView(navigation);
  DrawerLayout.LayoutParams params=new DrawerLayout.LayoutParams(dp(280),-1);params.gravity=androidx.core.view.GravityCompat.START;drawer.addView(menu,params);
 }
 @androidx.annotation.ChecksSdkIntAtLeast(api=26)
 private boolean supportsMiniPlayer() {
  return android.os.Build.VERSION.SDK_INT>=26 && getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE);
 }
 private void playbackPreference(LinearLayout menu,int label,boolean mini) {
  SwitchCompat toggle=new SwitchCompat(this);toggle.setText(label);toggle.setChecked(mini?library.miniPlayer:library.autoPlayLast);toggle.setPadding(0,dp(12),0,dp(12));menu.addView(toggle);
  if(mini) toggle.setEnabled(supportsMiniPlayer());
  boolean[] restoring={false};toggle.setOnCheckedChangeListener((button,checked)->{
   if(restoring[0]) return;
   if(!changeLibrary(()->{if(mini) library.miniPlayer=checked;else library.autoPlayLast=checked;})) {
    restoring[0]=true;button.setChecked(mini?library.miniPlayer:library.autoPlayLast);restoring[0]=false;
   }
   updateMiniBack();
  });
 }
 private void restoreLastPlayback() {
  if(!RadioPlaybackService.nowPlaying().isEmpty() || !storageReadable || !library.autoPlayLast || library.lastPlayed==null || library.lastPlayed.url.isEmpty()) return;
  selected=library.lastPlayed;resumePosition=library.lastPlayedPosition;play(selected);
 }
 private void rememberPlayback() {
  if(selected==null) return;
  long position=player!=null?Math.max(0,player.getCurrentPosition()):resumePosition;
  CastSession session=connectedCast();
  if(session!=null && session.getRemoteMediaClient()!=null) position=Math.max(0,session.getRemoteMediaClient().getApproximateStreamPosition());
  long saved=selected.kind==Playlist.Kind.TV?0:position;Playlist.Channel channel=selected;
  changeLibrary(()->{library.lastPlayed=channel;library.lastPlayedPosition=saved;});
 }
 private boolean miniPlaybackReady() {return library.miniPlayer && supportsMiniPlayer() && selected!=null && player!=null && player.getPlayWhenReady() && connectedCast()==null;}
 @androidx.annotation.RequiresApi(26)
 private android.app.PictureInPictureParams miniParams(boolean autoEnter) {
  android.app.PictureInPictureParams.Builder params=new android.app.PictureInPictureParams.Builder().setAspectRatio(new android.util.Rational(16,9));
  android.graphics.Rect bounds=new android.graphics.Rect();if(video!=null && video.getGlobalVisibleRect(bounds)) params.setSourceRectHint(bounds);
  if(android.os.Build.VERSION.SDK_INT>=31) params.setAutoEnterEnabled(autoEnter);
  return params.build();
 }
 private void updateMiniBack() {
  boolean ready=miniPlaybackReady();if(miniBack!=null) miniBack.setEnabled(ready);
  if(android.os.Build.VERSION.SDK_INT>=31 && supportsMiniPlayer() && !isInPictureInPictureMode()) {
   try {setPictureInPictureParams(miniParams(ready));} catch(IllegalStateException|IllegalArgumentException|SecurityException ignored) { }
  }
 }
 private boolean enterMiniPlayer() {
  if(!library.miniPlayer || !supportsMiniPlayer() || selected==null || player==null || !player.getPlayWhenReady() || connectedCast()!=null || isFinishing()) return false;
  if(isInPictureInPictureMode()) return true;
  rememberPlayback();playbackPanel.hideControls();
  try {return enterPictureInPictureMode(miniParams(miniPlaybackReady()));}
  catch(IllegalStateException|IllegalArgumentException|SecurityException e) {return false;}
 }
 @Override protected void onUserLeaveHint() {super.onUserLeaveHint();if(android.os.Build.VERSION.SDK_INT>=31) updateMiniBack();else enterMiniPlayer();}
 @Override public void onPictureInPictureModeChanged(boolean inPip,android.content.res.Configuration config) {
  super.onPictureInPictureModeChanged(inPip,config);
  if(inPip && !pipLayout) {
   pipLayout=true;pipWasFullscreen=fullscreen;drawer.closeDrawers();
   if(!fullscreen) {content.removeView(playbackPanel);fullscreenHost.addView(playbackPanel,new FrameLayout.LayoutParams(-1,-1));}
   drawer.setVisibility(View.GONE);fullscreenHost.setVisibility(View.VISIBLE);playbackPanel.fullscreenLayout(true);
  } else if(!inPip && pipLayout) {
   pipLayout=false;
   if(!pipWasFullscreen) {fullscreenHost.removeView(playbackPanel);content.addView(playbackPanel,panelIndex,new LinearLayout.LayoutParams(-1,-2));fullscreenHost.setVisibility(View.GONE);drawer.setVisibility(View.VISIBLE);playbackPanel.fullscreenLayout(false);}
  }
 }
 private void requestNotificationPermission() {
  if(android.os.Build.VERSION.SDK_INT>=33 && androidx.core.content.ContextCompat.checkSelfPermission(this,android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED && !getPreferences(0).getBoolean("notifications_requested",false)) {
   getPreferences(0).edit().putBoolean("notifications_requested",true).apply();notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS);
  }
 }
 private android.app.PendingIntent videoAction(String action,int request) {
  return android.app.PendingIntent.getBroadcast(this,request,new android.content.Intent(action).setPackage(getPackageName()),android.app.PendingIntent.FLAG_UPDATE_CURRENT|android.app.PendingIntent.FLAG_IMMUTABLE);
 }
 // Android exempts genuine media-session notifications from POST_NOTIFICATIONS.
 @android.annotation.SuppressLint("MissingPermission")
 private void updateVideoNotification() {
  android.app.NotificationManager manager=(android.app.NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  if(!backgroundPlayback || videoSession==null || selected==null || player==null || connectedCast()!=null || player.getPlaybackState()==Player.STATE_IDLE) {manager.cancel(2002);return;}
  manager.notify(2002,PlaybackNotification.create(this,"stream_video",videoSession,videoAction(VIDEO_TOGGLE,10),videoAction(VIDEO_STOP,11)));
 }
 private void loadVideoArtwork(Playlist.Channel channel,androidx.media3.common.MediaItem item) {
  int generation=++videoArtworkGeneration;
  if(PlaybackArtwork.bundled(this,channel.name) || channel.logoUrl.isEmpty()) return;
  videoArtworkWorker.execute(()->{
   byte[] data=PlaybackArtwork.remote(this,channel.logoUrl);if(data==null) return;
   runOnUiThread(()->{
    if(isDestroyed() || generation!=videoArtworkGeneration || selected==null || !selected.url.equals(channel.url) || player==null) return;
    player.replaceMediaItem(0,item.buildUpon().setMediaMetadata(item.mediaMetadata.buildUpon().setArtworkData(data,androidx.media3.common.MediaMetadata.PICTURE_TYPE_FRONT_COVER).build()).build());updateVideoNotification();
   });
  });
 }
 private void showFavoriteKind(boolean radio) {
  list.cancelFavoriteGesture();favoritesPage=true;favoritesMode=true;radioMode=radio;
  choosingFavoriteTab=true;favoriteTabs.check(radio?favoriteRadio.getId():favoriteTv.getId());choosingFavoriteTab=false;
  radioPanel.setVisibility(radio?View.VISIBLE:View.GONE);if(radio) radioPanel.open(true);filter();
 }
 private void openRadio(boolean favorites) {
  exitFullscreen();list.cancelFavoriteGesture();drawer.closeDrawer(androidx.core.view.GravityCompat.START);
  if(favorites) {showFavoriteKind(true);return;}
  favoritesPage=false;favoritesMode=false;radioMode=true;radioPanel.setVisibility(View.VISIBLE);filter();radioPanel.open(false);
 }
 private void openStreamingService() {
  drawer.closeDrawer(androidx.core.view.GravityCompat.START);
  if(library.sources.size()>1) {
   List<Library.Source> sources=new ArrayList<>(library.sources);
   new AlertDialog.Builder(this).setTitle(R.string.choose_source)
    .setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,sources),(dialog,position)->{list.cancelFavoriteGesture();loadSource(sources.get(position));})
    .setNegativeButton(android.R.string.cancel,null).show();
  } else navigate(false);
 }
 private void navigate(boolean favorites) {
  if(favorites) {drawer.closeDrawer(androidx.core.view.GravityCompat.START);showFavoriteKind(library.favoritesRadio);return;}
  favoritesPage=false;list.cancelFavoriteGesture();radioMode=false;radioPanel.setVisibility(View.GONE);drawer.closeDrawer(androidx.core.view.GravityCompat.START);favoritesMode=favorites;search.setText("");chooseFirstKind();rebuildGroups();
  if(!favorites && catalog.items.isEmpty() && !loading) {Library.Source last=library.findSource(library.lastSourceId);if(last==null && library.sources.size()==1) last=library.sources.get(0);if(last!=null) loadSource(last);}
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
  favoriteTabs.setVisibility(favoritesPage?View.VISIBLE:View.GONE);
  list.setVisibility(radioMode?View.GONE:View.VISIBLE);
  if(radioMode) {pageTitle.setVisibility(View.GONE);sections.setVisibility(View.GONE);groups.setVisibility(View.GONE);search.setVisibility(View.GONE);status.setVisibility(View.GONE);return;}
  pageTitle.setVisibility(favoritesMode?View.GONE:View.VISIBLE);
  pageTitle.setText(favoritesMode?"":activeSource==null?getString(R.string.library):activeSource.name);
  String group=groups.getSelectedItemPosition()>0?String.valueOf(groups.getSelectedItem()):null;
  sections.setVisibility(favoritesMode?View.GONE:View.VISIBLE);
  groups.setVisibility(favoritesMode?View.GONE:View.VISIBLE);
  search.setVisibility(favoritesMode?View.GONE:View.VISIBLE);
  if(favoritesMode) visible=new ArrayList<>(library.favorites);
  else {
   visible=new ArrayList<>();
   if(activeSource!=null) for(Playlist.Channel channel:catalog.filter(currentKind,group,search.getText().toString())) visible.add(new Library.Entry(activeSource.id,channel));
  }
  adapter.notifyDataSetChanged();
  if(playbackPanel!=null && playbackPanel.getVisibility()==View.VISIBLE && selected!=null) {status.setVisibility(View.GONE);return;}
  if(!loading) {status.setText("");status.setVisibility(View.GONE);}
 }
 private boolean changeLibrary(Runnable change) {
  if(!storageReadable) {status.setText(R.string.storage_failed);return false;}
  String previous;
  try {previous=library.toJson();} catch(Exception e) {status.setText(R.string.storage_failed);return false;}
  try {change.run();store.save(library);RadioWidgetProvider.refresh(this);return true;}
  catch(Exception e) {
   try {library=Library.fromJson(previous);} catch(Exception ignored) {storageReadable=false;}
   status.setText(R.string.storage_failed);Toast.makeText(this,R.string.storage_failed,Toast.LENGTH_LONG).show();return false;
  }
 }
 private void showSources() {
  if(loading||updatingAll) {Toast.makeText(this,R.string.wait_loading,Toast.LENGTH_SHORT).show();return;}
  List<Library.Source> sources=new ArrayList<>(library.sources);
  LinearLayout actions=column();actions.setPadding(dp(16),dp(8),dp(16),dp(12));
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle(R.string.sources)
   .setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,sources),(d,position)->loadSource(sources.get(position))).setView(actions).create();
  button(R.string.add_m3u,actions,()->{dialog.dismiss();showAddSource(false);});
  button(R.string.xtream_login,actions,()->{dialog.dismiss();showAddSource(true);});
  button(android.R.string.cancel,actions,dialog::dismiss);
  dialog.setOnShowListener(d->dialog.getListView().setOnItemLongClickListener((parent,view,position,id)->{
   Library.Source source=sources.get(position);
   new AlertDialog.Builder(this).setTitle(source.name).setItems(new String[]{getString(R.string.edit_source),getString(R.string.remove_source)},(choice,which)->{
    if(which==0) {dialog.dismiss();showSourceEditor(source,source.xtream);}
    else new AlertDialog.Builder(this).setTitle(R.string.remove_source).setMessage(getString(R.string.remove_source_note,source.name))
     .setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.remove_source,(confirmation,selected)->{
      if(changeLibrary(()->library.removeSource(source.id))) {
       if(activeSource!=null && activeSource.id.equals(source.id)) {activeSource=null;catalog=new Catalog();}
       dialog.dismiss();rebuildGroups();showSources();
      }
     }).show();
   }).show();return true;
  }));dialog.show();
 }
 private EditText input(LinearLayout form,int hint,int type) {EditText field=new EditText(this);field.setHint(hint);field.setSingleLine();field.setInputType(type);form.addView(field);return field;}
 private void showAddSource(boolean xtream) {showSourceEditor(null,xtream);}
 private void showSourceEditor(Library.Source existing,boolean xtream) {
  LinearLayout form=column();form.setPadding(dp(20),dp(8),dp(20),dp(12));
  EditText name=input(form,R.string.source_name,android.text.InputType.TYPE_CLASS_TEXT);
  EditText address=input(form,xtream?R.string.xtream_server:R.string.playlist_hint,android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);
  EditText username=xtream?input(form,R.string.xtream_user,android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS):null;
  EditText password=xtream?input(form,R.string.xtream_password,android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD):null;
  if(existing!=null) {name.setText(existing.name);address.setText(existing.server);if(username!=null) username.setText(existing.username);if(password!=null) password.setText(existing.password);}
  if(password!=null) password.setSaveEnabled(false);
  form.addView(text(getString(R.string.select_epg),13,Color.LTGRAY));
  List<Library.Guide> choices=new ArrayList<>(library.guides);List<String> labels=new ArrayList<>();labels.add(getString(R.string.no_epg));for(Library.Guide guide:choices) labels.add(guide.name);
  Spinner epg=new Spinner(this);ArrayAdapter<String> epgChoices=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,labels);epgChoices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);epg.setAdapter(epgChoices);form.addView(epg);
  if(existing!=null) for(int i=0;i<choices.size();i++) if(choices.get(i).id.equals(existing.epgId)) epg.setSelection(i+1);
  ScrollView scroll=new ScrollView(this);scroll.addView(form);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle(existing!=null?R.string.edit_source:xtream?R.string.xtream_login:R.string.add_m3u).setView(scroll).setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.save_source,null).create();
  dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   try {
    Library.Source source=xtream?Library.Source.xtream(name.getText().toString(),address.getText().toString(),username.getText().toString(),password.getText().toString()):Library.Source.m3u(name.getText().toString(),address.getText().toString());
    String epgId=epg.getSelectedItemPosition()>0?choices.get(epg.getSelectedItemPosition()-1).id:"";
    source=source.withEpg(epgId);
    if(existing!=null) {
     Library.Source edited=new Library.Source(existing.id,source.name,source.server,source.username,source.password,source.xtream,epgId);
     if(!changeLibrary(()->library.addSource(edited))) return;
     source=edited;if(activeSource!=null && activeSource.id.equals(edited.id)) activeSource=edited;
    }
    dialog.dismiss();loadSource(source);
   } catch(IllegalArgumentException e) {address.setError(getString(xtream?R.string.xtream_invalid:R.string.invalid_url));}
  }));dialog.show();
 }
 private String programmeTitle(Library.Entry entry,long now) {
  Library.Source source=library.findSource(entry.sourceId);
  if(entry.channel.kind!=Playlist.Kind.TV || source==null || library.findGuide(source.epgId)==null) return "";
  EpgData guide=epgData.get(source.epgId);
  if(guide==null) return getString(epgPending.contains(source.epgId)?R.string.epg_loading:R.string.epg_no_programme);
  EpgData.Programme programme=guide.current(entry.channel,now);return programme==null?getString(R.string.epg_no_programme):programme.title;
 }
 private void loadAssignedGuides(boolean force) {
  if(epgRepository==null || updatingAll) return;
  Set<String> ids=new LinkedHashSet<>();for(Library.Source source:library.sources) if(!source.epgId.isEmpty()) ids.add(source.epgId);
  for(String id:ids) {Library.Guide guide=library.findGuide(id);if(guide!=null) refreshGuide(guide,force);}
 }
 private void refreshGuide(Library.Guide guide,boolean force) {
  long now=System.currentTimeMillis();Long attempted=epgAttempted.get(guide.id);
  if(epgPending.contains(guide.id)||(!force && attempted!=null && now-attempted<5*60*1000L)) return;
  epgPending.add(guide.id);epgAttempted.put(guide.id,now);
  epgWorker.execute(()->{
   EpgData cached=epgRepository.cached(guide);
   if(cached!=null) runOnUiThread(()->acceptGuide(guide,cached));
   try {if(force||cached==null||!epgRepository.fresh(guide,System.currentTimeMillis())) {EpgData fetched=epgRepository.fetch(guide,System.currentTimeMillis());runOnUiThread(()->acceptGuide(guide,fetched));}}
   catch(Exception ignored) {if(force) runOnUiThread(()->{if(!isDestroyed()) Toast.makeText(this,getString(R.string.epg_fetch_failed,guide.name),Toast.LENGTH_LONG).show();});}
   finally {runOnUiThread(()->{if(!isDestroyed()) {epgPending.remove(guide.id);adapter.notifyDataSetChanged();Library.Guide current=library.findGuide(guide.id);if(current!=null && !current.url.equals(guide.url)) {epgAttempted.remove(guide.id);refreshGuide(current,true);}}});}
  });
 }
 private void acceptGuide(Library.Guide guide,EpgData data) {
  if(isDestroyed()) return;Library.Guide current=library.findGuide(guide.id);
  if(current!=null && current.url.equals(guide.url)) {epgData.put(guide.id,data);adapter.notifyDataSetChanged();}
 }
 private void showGuides() {
  if(loading||updatingAll) {Toast.makeText(this,R.string.wait_loading,Toast.LENGTH_SHORT).show();return;}
  List<Library.Guide> guides=new ArrayList<>(library.guides);
  new AlertDialog.Builder(this).setTitle(R.string.tv_guide).setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,guides),(dialog,position)->showGuideEditor(guides.get(position)))
   .setPositiveButton(R.string.add_epg,(dialog,which)->showGuideEditor(null)).setNegativeButton(android.R.string.cancel,null).show();
 }
 private void showGuideEditor(Library.Guide existing) {
  LinearLayout form=column();form.setPadding(dp(20),dp(8),dp(20),dp(12));
  EditText name=input(form,R.string.epg_name,android.text.InputType.TYPE_CLASS_TEXT);
  EditText url=input(form,R.string.epg_url,android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);
  if(existing!=null) {name.setText(existing.name);url.setText(existing.url);}
  AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle(existing==null?R.string.add_epg:R.string.edit_epg).setView(form).setPositiveButton(R.string.save,null).setNegativeButton(android.R.string.cancel,null);
  if(existing!=null) builder.setNeutralButton(R.string.delete,(dialog,which)->{
   new AlertDialog.Builder(this).setTitle(R.string.delete_epg).setMessage(R.string.delete_epg_note).setNegativeButton(R.string.no,null).setPositiveButton(R.string.yes,(d,w)->{
    if(changeLibrary(()->library.removeGuide(existing.id))) {epgData.remove(existing.id);epgRepository.remove(existing);adapter.notifyDataSetChanged();}
   }).show();
  });
  AlertDialog dialog=builder.create();dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view->{
   try {
    Library.Guide created=Library.Guide.create(name.getText().toString(),url.getText().toString());
    Library.Guide guide=existing==null?created:new Library.Guide(existing.id,created.name,created.url);
    if(changeLibrary(()->library.putGuide(guide))) {epgData.remove(guide.id);epgAttempted.remove(guide.id);dialog.dismiss();refreshGuide(guide,true);}
   } catch(IllegalArgumentException e) {url.setError(getString(R.string.epg_invalid));}
  }));dialog.show();
 }
 private void confirmReset() {
  new AlertDialog.Builder(this).setTitle(R.string.reset_app).setMessage(R.string.reset_app_note).setNegativeButton(R.string.no,null).setPositiveButton(R.string.yes,(dialog,which)->{
   stopService(new android.content.Intent(this,RadioPlaybackService.class));
   if(player!=null) player.stop();
   if(cast!=null) cast.getSessionManager().endCurrentSession(true);
   android.app.ActivityManager manager=(android.app.ActivityManager)getSystemService(android.content.Context.ACTIVITY_SERVICE);
   if(manager==null || !manager.clearApplicationUserData()) Toast.makeText(this,R.string.reset_failed,Toast.LENGTH_LONG).show();
  }).show();
 }
 private void updateAll() {
  if(loading||updatingAll) {Toast.makeText(this,R.string.wait_loading,Toast.LENGTH_SHORT).show();return;}
  updatingAll=true;
  List<Library.Source> sources=new ArrayList<>(library.sources);List<Library.Guide> guides=new ArrayList<>(library.guides);
  TextView progress=text(getString(R.string.updating_all),14,Color.LTGRAY);progress.setPadding(dp(24),dp(16),dp(24),dp(16));
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle(R.string.update_all).setView(progress).setCancelable(false).create();dialog.show();
  worker.execute(()->{
   Map<String,Catalog> catalogs=new LinkedHashMap<>();Map<String,EpgData> schedules=new LinkedHashMap<>();List<String> failures=new ArrayList<>();int completed=0,total=sources.size()+guides.size();
   for(Library.Source source:sources) {
    final int step=++completed;runOnUiThread(()->{if(!isDestroyed()) progress.setText(getString(R.string.update_progress,step,total,source.name));});
    try {catalogs.put(source.id,source.xtream?new XtreamClient(source.server,source.username,source.password).load():new Catalog(new PlaylistLoader().load(source.server)));} catch(Exception e) {failures.add(source.name);}
   }
   for(Library.Guide guide:guides) {
    final int step=++completed;runOnUiThread(()->{if(!isDestroyed()) progress.setText(getString(R.string.update_progress,step,total,guide.name));});
    try {schedules.put(guide.id,epgRepository.fetch(guide,System.currentTimeMillis()));} catch(Exception e) {failures.add(guide.name);}
   }
   runOnUiThread(()->{
    updatingAll=false;if(isDestroyed()) return;dialog.dismiss();
    boolean saved=changeLibrary(()->{for(Map.Entry<String,Catalog> entry:catalogs.entrySet()) library.refreshFavorites(entry.getKey(),entry.getValue().items);});
    if(activeSource!=null && catalogs.containsKey(activeSource.id)) catalog=catalogs.get(activeSource.id);
    epgData.putAll(schedules);for(String id:schedules.keySet()) epgAttempted.put(id,System.currentTimeMillis());rebuildGroups();
    String result=getResources().getQuantityString(R.plurals.updated_sources,catalogs.size(),catalogs.size())+"\n"+getResources().getQuantityString(R.plurals.updated_guides,schedules.size(),schedules.size())+"\n"+getResources().getQuantityString(R.plurals.update_failures,failures.size(),failures.size());
    if(!failures.isEmpty()) result+="\n\n"+getString(R.string.update_failed_names,android.text.TextUtils.join(", ",failures));
    if(!saved) result+="\n\n"+getString(R.string.storage_failed);
    new AlertDialog.Builder(this).setTitle(R.string.update_all).setMessage(result).setPositiveButton(android.R.string.ok,null).show();
   });
  });
 }
 private void busy(boolean value) {loading=value;list.setEnabled(!value);}
 private void loadSource(Library.Source source) {
  if(loading) return;
  favoritesPage=false;radioMode=false;radioPanel.setVisibility(View.GONE);favoritesMode=false;busy(true);rebuildGroups();status.setVisibility(View.VISIBLE);status.setText(source.xtream?R.string.loading_catalog:R.string.loading);
  worker.execute(()->{
   try {
    Catalog result=source.xtream?new XtreamClient(source.server,source.username,source.password).load():new Catalog(new PlaylistLoader().load(source.server));
    runOnUiThread(()->{
     if(isDestroyed()) return;
     Library.Source[] saved={source};
     if(changeLibrary(()->{saved[0]=library.addSource(source);library.lastSourceId=saved[0].id;library.refreshFavorites(saved[0].id,result.items);})) {
      activeSource=saved[0];catalog=result;search.setText("");chooseFirstKind();busy(false);rebuildGroups();loadAssignedGuides(false);
     }
    });
   } catch(Exception e) {runOnUiThread(()->{if(!isDestroyed()) showError(e);});}
   finally {runOnUiThread(()->{if(!isDestroyed()) {busy(false);}});}
  });
 }
 private void showError(Exception e) {
  status.setVisibility(View.VISIBLE);
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
  busy(true);status.setVisibility(View.VISIBLE);status.setText(R.string.loading_episodes);
  worker.execute(()->{
   try {
    List<Playlist.Channel> episodes=new XtreamClient(source.server,source.username,source.password).episodes(series);
    runOnUiThread(()->{
     if(isDestroyed()) return;
     new AlertDialog.Builder(this).setTitle(series.name).setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,episodes),(dialog,position)->play(episodes.get(position))).setNegativeButton(android.R.string.cancel,null).show();
     status.setVisibility(View.GONE);
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
    row.star=iconButton(R.drawable.ic_favorite_outline,"");row.star.setFocusable(false);layout.addView(row.star,new LinearLayout.LayoutParams(dp(48),dp(48)));row.indicator=NowPlayingIndicator.addTo(layout);layout.setTag(row);recycled=layout;
   } else row=(Row)recycled.getTag();
   Library.Entry entry=getItem(position);Playlist.Channel channel=entry.channel;row.name.setText(channel.name);
   Library.Source source=library.findSource(entry.sourceId);
   if(favoritesMode) {
    String programme=programmeTitle(entry,System.currentTimeMillis());row.group.setText(programme);row.group.setMaxLines(2);row.group.setTextColor(Color.rgb(170,170,170));row.group.setVisibility(programme.isEmpty()?View.GONE:View.VISIBLE);
   } else {row.group.setMaxLines(1);row.group.setText(channel.group);row.group.setVisibility(View.VISIBLE);}
   row.logo.setContentDescription(getString(R.string.logo_for,channel.name));
   String bundled=localLogos.find(channel.name);
   Object logoModel=bundled==null?LogoRequests.model(channel.logoUrl,source==null?null:source.server):bundled;
   Glide.with(MainActivity.this).load(logoModel).signature(com.bumptech.glide.signature.ApplicationVersionSignature.obtain(MainActivity.this)).placeholder(R.drawable.ic_channel).error(R.drawable.ic_channel).override(dp(96),dp(96)).into(row.logo);
   recycled.setAlpha(list.isPicked(position)?0.6f:1f);row.star.setVisibility(favoritesMode?View.GONE:View.VISIBLE);
   NowPlayingIndicator.show(row.indicator,isChannelPlaying(channel));
   boolean favorite=library.isFavorite(entry);row.star.setImageResource(favorite?R.drawable.ic_favorite:R.drawable.ic_favorite_outline);row.star.setContentDescription(getString(favorite?R.string.remove_favorite:R.string.add_favorite,channel.name));
   row.star.setOnClickListener(v->{if(!favoritesMode&&changeLibrary(()->library.toggleFavorite(entry))) {if(favoritesMode) rebuildGroups();else notifyDataSetChanged();}});
   return recycled;
  }
 }
 private boolean isChannelPlaying(Playlist.Channel channel) {
  if(selected==null || !selected.url.equals(channel.url)) return false;
  CastSession session=connectedCast();return session!=null && session.getRemoteMediaClient()!=null?session.getRemoteMediaClient().isPlaying():player!=null && player.isPlaying();
 }
 private static final class Row {ImageView logo;TextView name,group;ImageButton star;View indicator;}
 private void play(Playlist.Channel channel) {
  requestNotificationPermission();
  stopService(new android.content.Intent(this,RadioPlaybackService.class));
  status.setVisibility(View.VISIBLE);
  boolean same=selected!=null && selected.url.equals(channel.url);
  if(!same) resumePosition=0;
  selected=channel;long savedPosition=channel.kind==Playlist.Kind.TV?0:resumePosition;changeLibrary(()->{library.lastPlayed=channel;library.lastPlayedPosition=savedPosition;});CastSession session=connectedCast();playbackPanel.setTitle(channel.name);playbackPanel.setVisibility(View.VISIBLE);playbackPanel.hideControls();status.setVisibility(View.GONE);hideFullscreenSystemBars();
  if(session!=null && session.isConnected() && session.getRemoteMediaClient()!=null) {
   exitFullscreen();video.setVisibility(View.VISIBLE);if(player!=null) player.pause();video.setPlayer(null);watchRemote();playbackPanel.refresh();
   MediaMetadata metadata=new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);metadata.putString(MediaMetadata.KEY_TITLE,channel.name);
   if(!channel.logoUrl.isEmpty()) metadata.addImage(new com.google.android.gms.common.images.WebImage(android.net.Uri.parse(channel.logoUrl)));
   String path=Playlist.httpUrl(channel.url).encodedPath().toLowerCase(Locale.ROOT);
   String type=path.endsWith(".m3u8")?"application/x-mpegURL":path.endsWith(".mpd")?"application/dash+xml":path.endsWith(".mp4")?"video/mp4":path.endsWith(".mkv")?"video/x-matroska":path.endsWith(".avi")?"video/x-msvideo":"video/mp2t";
   MediaInfo media=new MediaInfo.Builder(channel.url).setContentType(type).setStreamType(channel.kind==Playlist.Kind.TV?MediaInfo.STREAM_TYPE_LIVE:MediaInfo.STREAM_TYPE_BUFFERED).setMetadata(metadata).build();
   session.getRemoteMediaClient().load(new MediaLoadRequestData.Builder().setMediaInfo(media).setAutoplay(true).build()).setResultCallback(result->{if(!isDestroyed()) status.setText(result.getStatus().isSuccess()?getString(R.string.casting,channel.name):getString(R.string.cast_stream_failed));});
  } else {
   video.setVisibility(View.VISIBLE);
   if(player==null) {
    player=new ExoPlayer.Builder(this).setSeekBackIncrementMs(10000).setSeekForwardIncrementMs(10000).build();video.setPlayer(player);player.setAudioAttributes(new androidx.media3.common.AudioAttributes.Builder().setUsage(androidx.media3.common.C.USAGE_MEDIA).setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE).build(),true);
    videoSession=new androidx.media3.session.MediaSession.Builder(this,player).setId("stream-video").setSessionActivity(android.app.PendingIntent.getActivity(this,10,new android.content.Intent(this,MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP),android.app.PendingIntent.FLAG_UPDATE_CURRENT|android.app.PendingIntent.FLAG_IMMUTABLE)).build();
    player.addListener(new Player.Listener() {
     @Override public void onPlayerError(PlaybackException e) {Toast.makeText(MainActivity.this,R.string.playback_failed,Toast.LENGTH_SHORT).show();}
     @Override public void onEvents(Player source,Player.Events events) {video.setKeepScreenOn(source.isPlaying());playbackPanel.refresh();updateVideoNotification();updateMiniBack();if(adapter!=null) adapter.notifyDataSetChanged();}
    });
   }
   video.setPlayer(player);
   androidx.media3.common.MediaMetadata metadata=new androidx.media3.common.MediaMetadata.Builder().setTitle(channel.name).setArtist(getString(R.string.app_name)).setArtworkData(PlaybackArtwork.local(this,channel.name),androidx.media3.common.MediaMetadata.PICTURE_TYPE_FRONT_COVER).build();
   MediaItem item=new MediaItem.Builder().setUri(channel.url).setMediaMetadata(metadata).build();
   if(same && channel.kind!=Playlist.Kind.TV && resumePosition>0) player.setMediaItem(item,resumePosition);
   else player.setMediaItem(item);
   loadVideoArtwork(channel,item);
   player.prepare();player.play();updateMiniBack();playbackPanel.hideControls();playbackPanel.refresh();status.setVisibility(View.GONE);
  }
 }
 private CastSession connectedCast() {
  CastSession session=cast==null?null:cast.getSessionManager().getCurrentCastSession();return session!=null && session.isConnected()?session:null;
 }
 private void watchRemote() {
  CastSession session=connectedCast();com.google.android.gms.cast.framework.media.RemoteMediaClient remote=session==null?null:session.getRemoteMediaClient();
  if(watchedRemote==remote) return;
  if(watchedRemote!=null) watchedRemote.unregisterCallback(remoteUpdates);
  watchedRemote=remote;if(watchedRemote!=null) watchedRemote.registerCallback(remoteUpdates);
 }
 private void togglePlaybackState() {
  CastSession session=connectedCast();
  if(session!=null && session.getRemoteMediaClient()!=null) {
   var remote=session.getRemoteMediaClient();if(remote.isPlaying()) remote.pause();else remote.play();
  } else if(player!=null) {if(player.getPlayWhenReady()) player.pause();else {if(player.getPlaybackState()==Player.STATE_IDLE) player.prepare();player.play();}}
  else if(selected!=null) play(selected);
  playbackPanel.refresh();updateMiniBack();
 }
 private void stopPlaybackState() {
  rememberPlayback();videoArtworkGeneration++;resumeLocalPlayback=false;
  CastSession session=connectedCast();if(session!=null && session.getRemoteMediaClient()!=null) session.getRemoteMediaClient().stop();
  if(player!=null) player.stop();exitFullscreen();playbackPanel.setVisibility(View.GONE);video.setKeepScreenOn(false);resumePosition=0;selected=null;updateMiniBack();hideFullscreenSystemBars();
  status.setVisibility(View.VISIBLE);status.setText(R.string.playback_stopped);
 }
 private void enterFullscreen() {
  if(fullscreen || connectedCast()!=null || playbackPanel.getVisibility()!=View.VISIBLE) return;
  drawer.closeDrawer(androidx.core.view.GravityCompat.START);fullscreen=true;previousOrientation=getRequestedOrientation();
  if(android.os.Build.VERSION.SDK_INT>=28) previousCutoutMode=getWindow().getAttributes().layoutInDisplayCutoutMode;
  content.removeView(playbackPanel);fullscreenHost.addView(playbackPanel,new FrameLayout.LayoutParams(-1,-1));fullscreenHost.setVisibility(View.VISIBLE);drawer.setVisibility(View.GONE);
  playbackPanel.fullscreenLayout(true);fullscreenBack.setEnabled(true);androidx.core.view.ViewCompat.requestApplyInsets(fullscreenHost);
  androidx.core.view.WindowInsetsControllerCompat bars=androidx.core.view.WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView());
  bars.setSystemBarsBehavior(androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);bars.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars());
  hideFullscreenSystemBars();
  setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
 }
 private void exitFullscreen() {
  if(!fullscreen) return;
  fullscreen=false;fullscreenHost.removeView(playbackPanel);content.addView(playbackPanel,panelIndex,new LinearLayout.LayoutParams(-1,-2));fullscreenHost.setVisibility(View.GONE);drawer.setVisibility(View.VISIBLE);
  playbackPanel.fullscreenLayout(false);fullscreenBack.setEnabled(false);getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
  if(android.os.Build.VERSION.SDK_INT>=28) {android.view.WindowManager.LayoutParams params=getWindow().getAttributes();params.layoutInDisplayCutoutMode=previousCutoutMode;getWindow().setAttributes(params);}
  androidx.core.view.WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).show(androidx.core.view.WindowInsetsCompat.Type.systemBars());setRequestedOrientation(previousOrientation);hideFullscreenSystemBars();
 }
 private void hideFullscreenSystemBars() {
  if(!fullscreen) {
   boolean active=playbackPanel!=null && playbackPanel.getVisibility()==View.VISIBLE && selected!=null;
   if(active) {getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);androidx.core.view.WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).hide(androidx.core.view.WindowInsetsCompat.Type.statusBars());}
   else {getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);androidx.core.view.WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).show(androidx.core.view.WindowInsetsCompat.Type.statusBars());}
   return;
  }
  androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
  getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
  if(android.os.Build.VERSION.SDK_INT>=28) {
   android.view.WindowManager.LayoutParams params=getWindow().getAttributes();
   params.layoutInDisplayCutoutMode=android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;getWindow().setAttributes(params);
  }
  androidx.core.view.WindowInsetsControllerCompat bars=androidx.core.view.WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView());
  bars.setSystemBarsBehavior(androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);bars.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars());
 }
 @Override public void onWindowFocusChanged(boolean hasFocus) {super.onWindowFocusChanged(hasFocus);if(hasFocus) hideFullscreenSystemBars();}
 @Override public void onConfigurationChanged(android.content.res.Configuration configuration) {super.onConfigurationChanged(configuration);hideFullscreenSystemBars();}
 @Override protected void onResume() {super.onResume();backgroundPlayback=false;updateVideoNotification();if(resumeLocalPlayback && player==null && selected!=null && connectedCast()==null) {resumeLocalPlayback=false;play(selected);}if(playbackPanel!=null) playbackPanel.refresh();hideFullscreenSystemBars();if(list!=null) {list.removeCallbacks(epgTick);list.postDelayed(epgTick,60000);loadAssignedGuides(false);}}
 @Override public boolean onKeyUp(int keyCode,android.view.KeyEvent event) {
  boolean handled=super.onKeyUp(keyCode,event);
  if(keyCode==android.view.KeyEvent.KEYCODE_VOLUME_UP || keyCode==android.view.KeyEvent.KEYCODE_VOLUME_DOWN || keyCode==android.view.KeyEvent.KEYCODE_VOLUME_MUTE) getWindow().getDecorView().post(()->{if(playbackPanel!=null) playbackPanel.refresh();});
  return handled;
 }
 @Override protected void onPause() {rememberPlayback();backgroundPlayback=true;updateVideoNotification();super.onPause();}
 @Override protected void onStop() {rememberPlayback();if(list!=null) list.removeCallbacks(epgTick);super.onStop();if(player!=null){resumeLocalPlayback=player.getPlayWhenReady();resumePosition=player.getCurrentPosition();if(videoSession!=null){videoSession.release();videoSession=null;}player.release();player=null;video.setPlayer(null);video.setKeepScreenOn(false);}if(playbackPanel!=null) playbackPanel.refresh();updateMiniBack();if(adapter!=null) adapter.notifyDataSetChanged();updateVideoNotification();}
 @Override protected void onDestroy() {if(watchedRemote!=null) watchedRemote.unregisterCallback(remoteUpdates);if(cast!=null) cast.getSessionManager().removeSessionManagerListener(sessions,CastSession.class);videoArtworkGeneration++;videoArtworkWorker.shutdownNow();unregisterReceiver(videoTransport);if(videoSession!=null){videoSession.release();videoSession=null;}((android.app.NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(2002);worker.shutdownNow();epgWorker.shutdownNow();if(radioPanel!=null) radioPanel.close();super.onDestroy();}
}
