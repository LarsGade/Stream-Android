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
import androidx.appcompat.widget.AppCompatCheckBox;
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
 @Override public boolean dispatchKeyEvent(android.view.KeyEvent event){return CastPlayback.volumeKey(this,event)||super.dispatchKeyEvent(event);}
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final ExecutorService catalogReader=Executors.newSingleThreadExecutor();
 private java.util.concurrent.Future<?> catalogRestoreTask;private int catalogRestoreGeneration;
 private final ExecutorService epgWorker=Executors.newSingleThreadExecutor();
 private EpgRepository epgRepository;
 private final Map<String,EpgData> epgData=new HashMap<>();
 private final Set<String> epgPending=new HashSet<>();
 private final GuideRecovery guideRecovery=new GuideRecovery();
 private final Map<String,Long> epgAttempted=new HashMap<>();
 private boolean updatingAll,radioMode,favoritesPage,choosingFavoriteTab;
 private ImageButton headerMenu;private LinearLayout headerOutputs;private FrameLayout headerBrandSlot;private MediaRouteButton headerCast;
 private FrameLayout headerFrame;private LinearLayout destinationRow;private PlaybackDock playbackDock;private PageFooter pageFooter;private boolean pageShortcutLaunch;private String destination="";private LinearLayout pageRoot,destinationHeader;private MusicSearchField destinationSearch;private TextView destinationTitle;private FavoriteViewSwitch destinationFavorites;
 private FavoriteHub favoriteHub;private boolean favoriteHubMode;
 private Spinner startupViewChoice;private boolean refreshingStartup;private Spinner startupPageChoice;private java.util.List<String> startupPageNames=new java.util.ArrayList<>();
 private Spinner favoriteCategories;
 private final List<Integer> favoriteCategoryIds=new ArrayList<>();
 private int currentFavoriteTab;
 private LocalFilesPanel localPanel;
 private boolean localMode,pcloudMode;
 private PcloudPanel pcloudPanel;
 private WebDavClient pcloudLoginClient;
 private boolean localPlayerAttached,activityStarted,acceptLocalPlayback=true;
 private String replacingLocalFolder;
 private MediaRouteButton castRouteButton;
 private LocalMedia pendingCastFile;private long pendingCastPosition;private List<LocalMedia> pendingCastQueue;
 private final ExecutorService localWorker=Executors.newSingleThreadExecutor();
 private androidx.activity.OnBackPressedCallback localBack;
 private RadioPanel radioPanel;
 private final Runnable epgTick=new Runnable() {
  public void run() {if(isDestroyed()) return;if(adapter!=null) adapter.notifyDataSetChanged();if(favoriteHub!=null)favoriteHub.programmeChanged();loadAssignedGuides(false);list.postDelayed(this,60000);}
 };
 private Library library=new Library();
 private LibraryPersistence store;
 private boolean storageReadable=true,loading,favoritesMode;
 private boolean settingsTransferBusy,applyingSettings;
 private final ExecutorService settingsWorker=Executors.newSingleThreadExecutor();
 private Catalog catalog=new Catalog();
 private final Map<String,Catalog> sourceCatalogs=new LinkedHashMap<>();
 private final Map<String,Library.Source> cachedSourceConfigs=new HashMap<>();
 private CombinedCatalog combinedCatalog=new CombinedCatalog(java.util.Collections.emptyList(),java.util.Collections.emptyMap());
 private final ExecutorService streamingWorker=Executors.newFixedThreadPool(3);
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
 private int musicFavoriteVersion;
 private Button settingsToggle;private LinearLayout settingsContent;
 private boolean videoMinimized;
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
 private final ExecutorService castStreamWorker=Executors.newSingleThreadExecutor();private int castLoadGeneration;
 private final ExecutorService videoArtworkWorker=Executors.newSingleThreadExecutor();
 private static final String VIDEO_TOGGLE="dk.stream.android.VIDEO_TOGGLE",VIDEO_STOP="dk.stream.android.VIDEO_STOP";
 private final androidx.activity.result.ActivityResultLauncher<String> notificationPermission=registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),granted->{});
 private final android.content.BroadcastReceiver videoTransport=new android.content.BroadcastReceiver() {
  @Override public void onReceive(android.content.Context context,android.content.Intent intent) {if(DailyGuideJob.CHANGED.equals(intent.getAction())){epgAttempted.clear();loadAssignedGuides(false);}else if(LocalAudioService.CHANGED.equals(intent.getAction())) {if(activityStarted) syncLocalPlayback();}else if(VIDEO_STOP.equals(intent.getAction())) stopPlaybackState();else if(VIDEO_TOGGLE.equals(intent.getAction())) togglePlaybackState();}
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
 private Playlist.Channel selected;private Library.Entry tvAnchor;private int tvBackupIndex;private String tvActiveUrl;
 private LinearLayout groupControls;
 private final SessionManagerListener<CastSession> sessions=new SessionManagerListener<CastSession>() {
  public void onSessionStarted(CastSession s,String id) { if(!ImageSession.viewing()&&LocalAudioService.current()==null&&RadioPlaybackService.selectedFile()==null&&RadioPlaybackService.selectedStation()==null&&selected!=null&&player!=null&&player.getCurrentMediaItem()!=null&&!localPlayerAttached&&!selected.url.startsWith("content:"))play(selected); }
  public void onSessionResumed(CastSession s,boolean suspended) {watchRemote();if(playbackPanel!=null)playbackPanel.refresh();}
  public void onSessionEnded(CastSession s,int error) {watchRemote();if(playbackPanel!=null) playbackPanel.refresh();if(adapter!=null) adapter.notifyDataSetChanged(); }
  public void onSessionStarting(CastSession s) { }
  public void onSessionStartFailed(CastSession s,int error) { status.setText(R.string.cast_failed); }
  public void onSessionEnding(CastSession s) { }
  public void onSessionResuming(CastSession s,String id) { }
  public void onSessionResumeFailed(CastSession s,int error) { }
  public void onSessionSuspended(CastSession s,int reason) {if(adapter!=null) adapter.notifyDataSetChanged();}
 };

 @Override public void onCreate(Bundle state) {
  androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
  super.onCreate(state);PlaybackNotices.rename(this);CastPlayback.init(this);videoMinimized=state!=null?state.getBoolean("videoMinimized",false):LocalAudioService.current()!=null;if(state!=null) replacingLocalFolder=state.getString("replacingLocalFolder");androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
  android.content.IntentFilter transport=new android.content.IntentFilter(VIDEO_TOGGLE);transport.addAction(VIDEO_STOP);transport.addAction(LocalAudioService.CHANGED);transport.addAction(DailyGuideJob.CHANGED);androidx.core.content.ContextCompat.registerReceiver(this,videoTransport,transport,androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
  epgRepository=new EpgRepository(this);store=new LibraryStore(this);localLogos=LocalChannelLogos.load(this);setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
  try {
   library=store.load();
   String legacy=getPreferences(0).getString("playlist","");
   if(!legacy.isEmpty()) {
    if(library.sources.isEmpty()) { Library.Source migrated=library.addSource(Library.Source.m3u("",legacy));library.lastSourceId=migrated.id;store.save(library); }
    getPreferences(0).edit().remove("playlist").apply();
   }
  } catch(Exception e) { storageReadable=false; }
  if(storageReadable)DailyGuideJob.sync(this,library);
  favoritesPage=false;favoritesMode=false;
  drawer=new DrawerLayout(this);drawer.setBackgroundColor(Color.rgb(16,16,16));
  LinearLayout root=column();pageRoot=root;root.setBackgroundColor(Color.rgb(16,16,16));
  content=root;drawer.addView(root,new DrawerLayout.LayoutParams(-1,-1));
  FrameLayout screen=new FrameLayout(this);screen.addView(drawer,new FrameLayout.LayoutParams(-1,-1));
  fullscreenHost=new FrameLayout(this);fullscreenHost.setBackgroundColor(Color.BLACK);fullscreenHost.setVisibility(View.GONE);screen.addView(fullscreenHost,new FrameLayout.LayoutParams(-1,-1));setContentView(screen);
  getOnBackPressedDispatcher().addCallback(this,new androidx.activity.OnBackPressedCallback(true){public void handleOnBackPressed(){if(!atFavoritesHome())showFavoritesHome();else{setEnabled(false);try{getOnBackPressedDispatcher().onBackPressed();}finally{setEnabled(true);}}}});
  miniBack=new androidx.activity.OnBackPressedCallback(false) {@Override public void handleOnBackPressed() {if(!atFavoritesHome())showFavoritesHome();else if(!enterMiniPlayer())finish();}};
  getOnBackPressedDispatcher().addCallback(this,miniBack);
  fullscreenBack=new androidx.activity.OnBackPressedCallback(false) {@Override public void handleOnBackPressed() {exitFullscreen();}};
  getOnBackPressedDispatcher().addCallback(this,fullscreenBack);
  localBack=new androidx.activity.OnBackPressedCallback(false) {public void handleOnBackPressed() {if(fullscreen) exitFullscreen();else if(!destination.isEmpty())showFavoritesHome();else if(favoriteHubMode)showFavoritesHome();else if(radioMode&&radioPanel!=null)radioPanel.back();else if(pcloudMode&&pcloudPanel!=null)pcloudPanel.back();else if(localPanel!=null) localPanel.back();}};getOnBackPressedDispatcher().addCallback(this,localBack);
  androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{
   androidx.core.graphics.Insets bars=insets.getInsetsIgnoringVisibility(androidx.core.view.WindowInsetsCompat.Type.systemBars()|androidx.core.view.WindowInsetsCompat.Type.displayCutout());
   view.setPadding(bars.left+dp(12),bars.top+dp(8),bars.right+dp(12),bars.bottom+dp(8));return insets;
  });
  FrameLayout header=new FrameLayout(this);headerFrame=header;root.addView(header,new LinearLayout.LayoutParams(-1,dp(64)));
  FrameLayout brandSlot=new FrameLayout(this);headerBrandSlot=brandSlot;FrameLayout.LayoutParams brandPosition=new FrameLayout.LayoutParams(-1,dp(64),Gravity.CENTER);brandPosition.leftMargin=dp(80);brandPosition.rightMargin=dp(80);header.addView(brandSlot,brandPosition);
  ImageView brand=new ImageView(this);brand.setImageResource(R.drawable.brand_banner);brand.setScaleX(1.215f);brand.setScaleY(1.215f);brand.setScaleType(ImageView.ScaleType.FIT_CENTER);brand.setAdjustViewBounds(true);brand.setContentDescription(getString(R.string.app_name));brand.setPadding(dp(8),dp(4),dp(8),dp(4));brandSlot.addView(brand,new FrameLayout.LayoutParams(-1,dp(64),Gravity.CENTER));
  ImageButton menu=iconButton(R.drawable.ic_menu,getString(R.string.open_menu));headerMenu=menu;header.addView(menu,new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.START|Gravity.CENTER_VERTICAL));menu.setOnClickListener(v->drawer.openDrawer(androidx.core.view.GravityCompat.START));
  headerOutputs=new LinearLayout(this);headerOutputs.setGravity(Gravity.CENTER_VERTICAL);headerOutputs.addView(new BluetoothOutputButton(this),new LinearLayout.LayoutParams(dp(40),dp(48)));
  headerCast=new CastOutputButton(this){@Override public boolean performClick(){if(!CastPlayback.connected(MainActivity.this)&&(LocalAudioService.current()!=null||ImageSession.viewing())&&android.os.Build.VERSION.SDK_INT>=37&&checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK")!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{"android.permission.ACCESS_LOCAL_NETWORK"},7303);return true;}return super.performClick();}};headerOutputs.addView(headerCast,new LinearLayout.LayoutParams(dp(40),dp(48)));for(int output=0;output<headerOutputs.getChildCount()-1;output++)headerOutputs.getChildAt(output).setPadding(dp(8),dp(12),dp(8),dp(12));header.addView(headerOutputs,new FrameLayout.LayoutParams(-2,dp(48),Gravity.END|Gravity.CENTER_VERTICAL));

  createDrawer();
  androidx.activity.OnBackPressedCallback closeDrawer=new androidx.activity.OnBackPressedCallback(false) {
   @Override public void handleOnBackPressed() {drawer.closeDrawer(androidx.core.view.GravityCompat.START);}
  };
  getOnBackPressedDispatcher().addCallback(this,closeDrawer);
  drawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
   @Override public void onDrawerOpened(View view) {closeDrawer.setEnabled(true);}
   @Override public void onDrawerClosed(View view) {closeDrawer.setEnabled(false);collapseSettings();}
  });
  destinationHeader=column();destinationHeader.setPadding(dp(4),0,dp(4),dp(8));destinationHeader.setVisibility(View.GONE);destinationRow=new LinearLayout(this);destinationRow.setGravity(Gravity.CENTER_VERTICAL);ImageButton homeBack=iconButton(R.drawable.ic_back,"Tilbage til forsiden");homeBack.setOnClickListener(view->showFavoritesHome());destinationRow.addView(homeBack,new LinearLayout.LayoutParams(dp(48),dp(48)));destinationTitle=text("",28,Color.WHITE);destinationTitle.setTypeface(null,android.graphics.Typeface.BOLD);destinationRow.addView(destinationTitle,new LinearLayout.LayoutParams(0,-2,1));destinationRow.addView(new Space(this),new LinearLayout.LayoutParams(0,1,1));destinationHeader.addView(destinationRow);destinationSearch=new MusicSearchField(this,this::searchDestination);destinationHeader.addView(destinationSearch);destinationFavorites=new FavoriteViewSwitch(this,selected->{String page=destination;if(page.isEmpty())return;if(changeLibrary(()->library.pageFavorites.put(page,selected)))openDestination(page);});LinearLayout.LayoutParams favoriteSpace=new LinearLayout.LayoutParams(-1,dp(42));favoriteSpace.topMargin=dp(8);destinationHeader.addView(destinationFavorites,favoriteSpace);root.addView(destinationHeader);
  pageTitle=text("",20,Color.WHITE);pageTitle.setTypeface(null,android.graphics.Typeface.BOLD);pageTitle.setPadding(dp(4),dp(12),0,dp(8));root.addView(pageTitle);
  status=text("",13,Color.LTGRAY);status.setVisibility(View.GONE);status.setPadding(dp(4),0,0,dp(8));root.addView(status);
  playbackPanel=new PlaybackPanel(this,new PlaybackPanel.Actions() {
   public FavoriteLists.Item downloadItem(){FavoriteLists.Item item=MediaDownloads.current(MainActivity.this);return item!=null?item:selected!=null&&selected.kind!=Playlist.Kind.TV?FavoriteLists.of(new Library.Entry("",selected)):null;}
   public boolean recordingAvailable(){return selected!=null&&selected.kind==Playlist.Kind.TV&&!selected.url.startsWith("content:");}
   public boolean recording(){return TvRecordingService.active();}
   public void toggleRecording(){confirmTvRecording();}
   public boolean backupAvailable(){return tvAnchor!=null&&selected!=null&&selected.kind==Playlist.Kind.TV;}
   public void switchBackup(){switchTvBackup();}
   public boolean liveChannel(){return selected!=null&&selected.kind==Playlist.Kind.TV&&!selected.url.startsWith("content:");}
   public void goLive(){goToLive();}
   public void chooseCast(){castRouteButton.performClick();}
   public void togglePlayback() {togglePlaybackState();}
   public void stopPlayback() {stopPlaybackState();}
   public void toggleFullscreen() {if(fullscreen) exitFullscreen();else enterFullscreen();}
   public boolean pictureInPicture(){return android.os.Build.VERSION.SDK_INT>=26&&isInPictureInPictureMode();}
   public void volumeFailed() {UiText.toast(MainActivity.this,R.string.volume_failed,Toast.LENGTH_SHORT).show();}
   public boolean playing() {CastSession session=connectedCast();return session!=null && session.getRemoteMediaClient()!=null?(session.getRemoteMediaClient().isPlaying()||session.getRemoteMediaClient().isBuffering()):player!=null && player.getPlayWhenReady() && player.getPlaybackState()!=Player.STATE_IDLE && player.getPlaybackState()!=Player.STATE_ENDED && player.getPlayerError()==null;}
   public boolean fullscreen() {return fullscreen;}
   public CastSession castSession() {return connectedCast();}
  });
  castRouteButton=playbackPanel.castButton();
  video=playbackPanel.video;video.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->updateMiniBack());root.addView(playbackPanel,new LinearLayout.LayoutParams(-1,-2));panelIndex=root.indexOfChild(playbackPanel);
  sections=new RadioGroup(this);sections.setOrientation(RadioGroup.HORIZONTAL);root.addView(sections);
  int[] labels={R.string.tv,R.string.films,R.string.series};
  for(int i=0;i<labels.length;i++) {
   RadioButton button=new RadioButton(this);button.setId(View.generateViewId());button.setText(labels[i]);kindButtons[i]=button;sections.addView(button,new RadioGroup.LayoutParams(0,-2,1));
  }
  kindButtons[0].setChecked(true);
  favoriteCategories=dropdown();favoriteCategories.setContentDescription(UiText.text("Vælg favoritkategori"));root.addView(favoriteCategories,new LinearLayout.LayoutParams(-1,dp(48)));
  groups=new androidx.appcompat.widget.AppCompatSpinner(this,Spinner.MODE_DROPDOWN){@Override public boolean performClick(){if(getAdapter()==null)return false;androidx.appcompat.widget.ListPopupWindow popup=new androidx.appcompat.widget.ListPopupWindow(MainActivity.this);popup.setAnchorView(this);popup.setWidth(getWidth());popup.setModal(true);popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xFF505050));popup.setAdapter((ListAdapter)getAdapter());popup.setOnItemClickListener((parent,row,position,id)->{setSelection(position);popup.dismiss();});popup.show();popup.getListView().setOnItemLongClickListener((parent,row,position,id)->{if(position==0)return false;String name=String.valueOf(getAdapter().getItem(position));showGroupFavoriteMenu(row,name);return true;});return true;}};
  groups.setContentDescription(getString(R.string.select_group));groups.setOnLongClickListener(view->{if(groups.getSelectedItemPosition()<=0)return false;showGroupFavoriteMenu(groups,String.valueOf(groups.getSelectedItem()));return true;});groupControls=new LinearLayout(this);groupControls.setGravity(Gravity.CENTER_VERTICAL);root.addView(groupControls,new LinearLayout.LayoutParams(-1,dp(48)));groupControls.addView(groups,new LinearLayout.LayoutParams(-1,-1));
  search=new ClearableSearchEditText(this);search.setSingleLine();search.setHint(R.string.search_hint);root.addView(search);
  list=new FavoriteListView(this);MediaListStyle.apply(list);root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
  radioPanel=new RadioPanel(this,()->library,this::changeLibrary,()->{requestNotificationPermission();if(selected!=null) stopPlaybackState();});radioPanel.setVisibility(View.GONE);root.addView(radioPanel,new LinearLayout.LayoutParams(-1,0,1));
  radioPanel.setBackStateListener(inside->{if(radioMode)localBack.setEnabled(inside);});
  localPanel=new LocalFilesPanel(this,new LocalFilesPanel.Actions() {
   public void searchCleared(){destinationSearch.input.setText("");}
   public Library library() {return library;}
   public boolean save(Runnable change) {return changeLibrary(change);}
   public void pickFiles() {pickLocal(false);}
   public void pickFolder() {replacingLocalFolder=null;pickLocal(true);}
   public void changeFolder(LocalMedia entry) {replacingLocalFolder=entry.uri;pickLocal(true);}
   public void play(LocalMedia entry) {playLocal(entry);}
   public boolean playing(LocalMedia entry){return isFilePlaying(entry);}
   public void navigationChanged(boolean inside) {localBack.setEnabled(localMode&&inside);}
  });localPanel.setVisibility(View.GONE);root.addView(localPanel,new LinearLayout.LayoutParams(-1,0,1));
  pcloudPanel=new PcloudPanel(this,new PcloudPanel.Actions(){
   public void searchCleared(){destinationSearch.input.setText("");}
   public Library library(){return library;}public boolean save(Runnable mutation){return changeLibrary(mutation);}public void play(LocalMedia entry,List<LocalMedia> queue){playLocal(entry,0,queue);}public boolean playing(LocalMedia entry){return isFilePlaying(entry);}public void navigationChanged(boolean inside){if(pcloudMode)localBack.setEnabled(inside);}
  });pcloudPanel.setVisibility(View.GONE);root.addView(pcloudPanel,new LinearLayout.LayoutParams(-1,0,1));
  favoriteHub=new FavoriteHub(this,new FavoriteHub.Actions(){
   public void searchCleared(){destinationSearch.input.setText("");}
   public Library library(){return library;}public boolean save(Runnable change){return changeLibrary(change);}
   public void navigation(boolean back){if(favoriteHubMode){localBack.setEnabled(back);headerMenu.setVisibility(View.INVISIBLE);updateHeaderOutputs();}}
   public void openMenu(){drawer.openDrawer(androidx.core.view.GravityCompat.START);}
   public void openMusic(){MainActivity.this.openMusic();}
   public void home(){showFavoritesHome();}
   public void openPage(String name){openDestination(name);}
   public String programme(Library.Entry entry){return programmeTitle(entry,System.currentTimeMillis());}
   public void play(FavoriteLists.Item item,List<LocalMedia> queue){if(recordingBlocked())return;if(item.stream!=null){if(item.stream.channel.kind==Playlist.Kind.TV&&favoriteHub.tvListIndex()==0){tvAnchor=item.stream;tvBackupIndex=0;tvActiveUrl=item.stream.channel.url;MainActivity.this.play(item.stream.channel);}else open(item.stream);}else if(item.radio!=null)radioPanel.play(item.radio);else playLocal(item.file,0,queue);}
   public boolean playing(FavoriteLists.Item item){return item.stream!=null?isChannelPlaying(item.stream.channel):item.radio!=null?RadioPlaybackService.playing(item.radio.id)||CastPlayback.hasMedia(MainActivity.this)&&CastPlayback.title(MainActivity.this).equals(item.radio.displayName()):isFilePlaying(item.file);}
  });favoriteHub.setVisibility(View.GONE);root.addView(favoriteHub,new LinearLayout.LayoutParams(-1,0,1));
  RadioBar miniBar=new RadioBar(this,()->{requestNotificationPermission();if(selected!=null) stopPlaybackState();},new RadioBar.VideoActions(){
   public boolean stop(){if(recordingBlocked())return false;stopPlaybackState();return true;}
   public FavoriteLists.Item downloadItem(){return selected!=null&&selected.kind!=Playlist.Kind.TV?FavoriteLists.of(new Library.Entry("",selected)):null;}
   public Object artwork(){
    if(selected==null)return null;String bundled=localLogos.find(selected.name);if(bundled!=null)return bundled;
    if(!selected.logoUrl.isEmpty())return LogoRequests.model(selected.logoUrl,selected.url);
    if(player!=null){androidx.media3.common.MediaMetadata metadata=player.getMediaMetadata();return metadata.artworkData!=null&&metadata.artworkData.length>0?metadata.artworkData:metadata.artworkUri;}return null;
   }
   public long position(){return player==null?0:player.getCurrentPosition();}public long duration(){return player==null?0:player.getDuration();}
   public String title(){return selected==null?"":selected.name;}
   public boolean playing(){CastSession session=connectedCast();if(session!=null&&session.getRemoteMediaClient()!=null)return session.getRemoteMediaClient().isPlaying()||session.getRemoteMediaClient().isBuffering();return player!=null&&player.getPlayWhenReady()&&player.getPlaybackState()!=Player.STATE_IDLE&&player.getPlaybackState()!=Player.STATE_ENDED;}
   public boolean liveChannel(){return playbackPanel.liveChannel();}
   public boolean liveNow(){return playbackPanel.liveNow();}
   public boolean needsLive(){return playbackPanel.needsLive();}
   public void goLive(){goToLive();}
   public void chooseCast(){castRouteButton.performClick();}
   public void toggle(){togglePlaybackState();}
   public void open(){if(LocalAudioService.current()!=null)syncLocalPlayback();enterFullscreen();}
  });
  pageFooter=new PageFooter(this,this::showFavoritesHome,()->drawer.openDrawer(androidx.core.view.GravityCompat.START));playbackDock=new PlaybackDock(this,pageFooter,miniBar);root.addView(playbackDock,new LinearLayout.LayoutParams(-1,-2));
  adapter=new ChannelAdapter();list.setAdapter(adapter);
  list.setFavoriteListener(new FavoriteListView.Listener() {
   public boolean enabled() {return !loading&&!updatingAll&&list.isEnabled();}
   public boolean canReorder(){return favoritesMode&&(favoritesPage||search.getText().toString().trim().isEmpty());}
   public void showActions(int position,View anchor) {
    if(position<0||position>=visible.size()) return;
    Library.Entry entry=visible.get(position);PopupMenu menu=new StablePopupMenu(MainActivity.this,anchor);MediaDownloads.add(menu,MainActivity.this,FavoriteLists.of(entry));
    boolean favorite=library.isFavorite(entry);
    menu.getMenu().add(UiText.text(favorite?"Fjern fra favoritter":"Tilføj til favoritter")).setOnMenuItemClickListener(item->{if(favorite){if(changeLibrary(()->FavoriteLists.remove(library,FavoriteLists.of(entry))))filter();}else FavoritePicker.show(MainActivity.this,library,MainActivity.this::changeLibrary,FavoriteLists.of(entry),MainActivity.this::filter);return true;});if(favorite&&entry.channel.kind==Playlist.Kind.TV)menu.getMenu().add(UiText.text("Vælg TV-favoritlister")).setOnMenuItemClickListener(choice->{FavoritePicker.show(MainActivity.this,library,MainActivity.this::changeLibrary,FavoriteLists.of(entry),MainActivity.this::filter);return true;});list.addReorderActions(menu,position);menu.show();
   }
   public void move(int from,int to) {if(from>=0&&from<visible.size()&&to>=0&&to<visible.size()) {Library.Entry entry=visible.remove(from);visible.add(to,entry);adapter.notifyDataSetChanged();}}
   public void commit() {List<Library.Entry> ordered=new ArrayList<>(visible);changeLibrary(()->library.reorderVisibleFavorites(ordered));filter();}
   public void cancel() {filter();}
  });
  list.setOnItemClickListener((parent,view,position,id)->open(visible.get(position)));
  sections.setOnCheckedChangeListener((group,id)->{ if(!favoriteHubMode)minimizeVideo();for(int i=0;i<kindButtons.length;i++) if(kindButtons[i].getId()==id) currentKind=Playlist.Kind.values()[i];rebuildGroups(); });
  groups.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
   public void onItemSelected(AdapterView<?> parent,View view,int position,long id) { filter(); }
   public void onNothingSelected(AdapterView<?> parent) { }
  });
  search.addTextChangedListener(new TextWatcher() {
   public void beforeTextChanged(CharSequence s,int a,int count,int after) { }
   public void onTextChanged(CharSequence s,int start,int before,int count) { filter(); }
   public void afterTextChanged(Editable value) { }
  });
  favoriteCategories.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> parent){}public void onItemSelected(AdapterView<?> parent,View view,int position,long id){if(choosingFavoriteTab||!favoritesPage)return;int selected=favoriteCategories.getSelectedItemPosition();if(selected<0||selected>=favoriteCategoryIds.size())return;int tab=favoriteCategoryIds.get(selected);if(tab==currentFavoriteTab)return;if(changeLibrary(()->library.favoritesCurrentTab=tab))showFavoriteTab(tab);else updateFavoriteTabVisibility();}});
  chooseFirstKind();rebuildGroups();loadAssignedGuides(false);openStartupPage();
  try { cast=CastContext.getSharedInstance(this);CastButtonFactory.setUpMediaRouteButton(this,castRouteButton);cast.getSessionManager().addSessionManagerListener(sessions,CastSession.class); }
  catch(Exception e) { castRouteButton.setVisibility(View.INVISIBLE); }
  if(!storageReadable) {status.setVisibility(View.VISIBLE);status.setText(R.string.storage_failed);}
  getOnBackPressedDispatcher().addCallback(this,new androidx.activity.OnBackPressedCallback(true){public void handleOnBackPressed(){if(recordingBlocked())return;setEnabled(false);getOnBackPressedDispatcher().onBackPressed();setEnabled(true);}});
  restoreCatalogs();getWindow().getDecorView().post(this::firstRunSetup);
  if(getIntent().getBooleanExtra("resumeCastFile",false))resumeCastFileOnPhone(getIntent());else if(getIntent().hasExtra("resumeCastUrl"))resumeCastOnPhone(getIntent());else if(!pageShortcutLaunch&&!getIntent().getBooleanExtra("favoritesHome",false))restoreLastPlayback();if(getIntent().getBooleanExtra("openRadio",false)) openRadio(true);if(getIntent().getBooleanExtra("openFullPlayer",false))video.post(this::openNotificationPlayer);if(getIntent().getBooleanExtra("openDrawer",false)){getIntent().removeExtra("openDrawer");drawer.post(()->drawer.openDrawer(androidx.core.view.GravityCompat.START));}
 }
 @Override protected void onNewIntent(android.content.Intent intent) {super.onNewIntent(intent);setIntent(intent);if(TvRecordingService.active())return;if(intent.getBooleanExtra("resumeCastFile",false))resumeCastFileOnPhone(intent);if(intent.hasExtra("resumeCastUrl"))resumeCastOnPhone(intent);if(intent.getBooleanExtra("favoritesHome",false)){intent.removeExtra("favoritesHome");showFavoritesHome();}if(intent.getBooleanExtra("openRadio",false)) openRadio(true);if(intent.getBooleanExtra("openFullPlayer",false))video.post(this::openNotificationPlayer);if(intent.getBooleanExtra("openDrawer",false)){intent.removeExtra("openDrawer");showFavoritesHome();drawer.openDrawer(androidx.core.view.GravityCompat.START);}String page=intent.getStringExtra(PageShortcut.EXTRA);intent.removeExtra(PageShortcut.EXTRA);if(PageShortcut.valid(page))openDestination(page);}
 private void resumeCastFileOnPhone(android.content.Intent intent){intent.removeExtra("resumeCastFile");LocalQueue queue=CastPlayback.takePhoneQueue();if(queue!=null)playLocal(queue.entries.get(queue.start),Math.max(0,intent.getLongExtra("resumeCastPosition",0)),queue.entries);}
 private void resumeCastOnPhone(android.content.Intent intent){String url=intent.getStringExtra("resumeCastUrl");if(url==null)return;intent.removeExtra("resumeCastUrl");Playlist.Kind kind;try{kind=Playlist.Kind.valueOf(intent.getStringExtra("resumeCastKind"));}catch(Exception invalid){kind=Playlist.Kind.FILM;}long position=Math.max(0,intent.getLongExtra("resumeCastPosition",0));Playlist.Channel channel=new Playlist.Channel(intent.getStringExtra("resumeCastName"),url,"",kind,null,intent.getStringExtra("resumeCastLogo"));selected=null;resumePosition=position;play(channel);if(kind!=Playlist.Kind.TV&&player!=null)player.seekTo(position);}
 private void openNotificationPlayer(){if(isDestroyed()||isFinishing())return;if(CastPlayback.hasMedia(this)){if(CastPlayback.image(this)){startActivity(ImageViewerActivity.reopenIntent(this));return;}startActivity(new android.content.Intent(this,AudioPlayerActivity.class).putExtra("cast",true));return;}if(RadioPlaybackService.audioPlayer()!=null&&LocalAudioService.current()==null){startActivity(new android.content.Intent(this,AudioPlayerActivity.class).putExtra("local",false));return;}LocalMedia file=LocalAudioService.current();if(file!=null){if(file.audio()){startActivity(new android.content.Intent(this,AudioPlayerActivity.class).putExtra("local",true));return;}syncLocalPlayback();}enterFullscreen();}
 private void collapseSettings(){if(settingsContent==null||settingsToggle==null)return;settingsContent.setVisibility(View.GONE);settingsToggle.setSelected(false);settingsToggle.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,R.drawable.ic_expand_more,0);settingsToggle.setContentDescription(UiText.text("Indstillinger, fold ud"));}
 private void minimizeVideo(){moveDestinationFilters(false);destination="";if(destinationHeader!=null)destinationHeader.setVisibility(View.GONE);if(localPanel!=null)localPanel.setHeadingManaged(false);if(pcloudPanel!=null)pcloudPanel.setHeadingManaged(false);if(radioPanel!=null)radioPanel.setHeadingManaged(false);if(favoriteHub!=null)favoriteHub.setHeadingManaged(false);if(headerMenu!=null)headerMenu.setVisibility(View.VISIBLE);favoriteHubMode=false;updateHeaderOutputs();if(favoriteHub!=null)favoriteHub.setVisibility(View.GONE);videoMinimized=true;exitFullscreen();if(playbackPanel!=null){playbackPanel.hideControls();playbackPanel.setVisibility(View.GONE);}if(video!=null)video.setKeepScreenOn(false);hideFullscreenSystemBars();}
 private int dp(int value) {return Math.round(value*getResources().getDisplayMetrics().density);}
 private LinearLayout column() {LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);return layout;}
 private ArrayAdapter<String> boldDropdown(String[] labels){return new UiArrayAdapter<String>(this,android.R.layout.simple_spinner_item,labels){
  private View bold(View view){((TextView)view).setTypeface(null,android.graphics.Typeface.BOLD);return view;}
  @Override public View getView(int position,View recycled,ViewGroup parent){return bold(super.getView(position,recycled,parent));}
  @Override public View getDropDownView(int position,View recycled,ViewGroup parent){return bold(super.getDropDownView(position,recycled,parent));}
 };}
 private Spinner dropdown(){Spinner spinner=new Spinner(this,Spinner.MODE_DROPDOWN);spinner.setPopupBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xFF505050));return spinner;}
 private TextView text(String value,int size,int color) {TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setTextColor(color);return view;}
 private ImageButton iconButton(int icon,String description) {
  ImageButton button=new ImageButton(this);button.setImageResource(icon);button.setContentDescription(UiText.text(description));button.setBackgroundColor(Color.TRANSPARENT);button.setPadding(dp(12),dp(12),dp(12),dp(12));return button;
 }
 private Button button(int label,LinearLayout parent,Runnable action) {Button button=new Button(this);button.setText(label);parent.addView(button);button.setOnClickListener(v->action.run());return button;}
 private void createDrawer() {
  LinearLayout navigation=column();navigation.setBackgroundColor(Color.rgb(30,30,30));navigation.setPadding(dp(20),dp(56),dp(20),dp(20));
  Button favoritesButton=button(R.string.home,navigation,()->navigate(true));favoritesButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF3686FF));favoritesButton.setTextColor(Color.WHITE);favoritesButton.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,favoritesButton.getTextSize()*1.25f);favoritesButton.setTypeface(favoritesButton.getTypeface(),android.graphics.Typeface.BOLD);
  Button tvButton=button(R.string.tv,navigation,()->openDestination("TV")),filmButton=button(R.string.films,navigation,()->openDestination("Film")),seriesButton=button(R.string.series,navigation,()->openDestination("Serier")),radioButton=button(R.string.radio,navigation,()->openDestination("Radio")),musicButton=button(R.string.music_library,navigation,this::openMusic),podcastButton=button(R.string.podcast,navigation,()->openDestination("Podcast")),localButton=button(R.string.local_files,navigation,()->openDestination("Lokale filer")),pcloudButton=button(R.string.pcloud,navigation,()->openDestination("Cloud"));
  for(Button primary:new Button[]{tvButton,filmButton,seriesButton,radioButton,musicButton,podcastButton,localButton,pcloudButton}){primary.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this,R.color.drawer_dark_blue)));primary.setTextColor(Color.WHITE);}
  settingsToggle=button(R.string.settings,navigation,()->{});
  settingsContent=column();settingsContent.setPadding(dp(8),0,dp(8),0);settingsContent.setVisibility(View.GONE);navigation.addView(settingsContent,new LinearLayout.LayoutParams(-1,-2));
  settingsToggle.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,R.drawable.ic_expand_more,0);settingsToggle.setContentDescription(UiText.text("Indstillinger, fold ud"));
  settingsToggle.setOnClickListener(view->{boolean expanded=settingsContent.getVisibility()!=View.VISIBLE;settingsContent.setVisibility(expanded?View.VISIBLE:View.GONE);settingsToggle.setSelected(expanded);settingsToggle.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,expanded?R.drawable.ic_expand_less:R.drawable.ic_expand_more,0);settingsToggle.setContentDescription(UiText.text(expanded?"Indstillinger, fold sammen":"Indstillinger, fold ud"));});
  TextView startLabel=text(UiText.text("Opstartsside"),14,Color.WHITE);settingsContent.addView(startLabel);
  startupPageChoice=dropdown();startupPageChoice.setContentDescription(UiText.text("Opstartsside"));settingsContent.addView(startupPageChoice);refreshStartupPageChoice();
  startupPageChoice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> parent){}public void onItemSelected(AdapterView<?> parent,View view,int position,long id){if(position<0||position>=startupPageNames.size()||position!=startupPageChoice.getSelectedItemPosition())return;String name=startupPageNames.get(position);if(name.equals(StartupPages.selected(library)))return;if(!changeLibrary(()->StartupPages.select(library,name)))refreshStartupPageChoice();}});
  startupViewChoice=dropdown();startupViewChoice.setContentDescription(UiText.text("Visning ved opstart"));startupViewChoice.setAdapter(boldDropdown(new String[]{"Alle","Favoritter"}));settingsContent.addView(startupViewChoice);refreshStartupPageChoice();
  startupViewChoice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> parent){}public void onItemSelected(AdapterView<?> parent,View view,int position,long id){if(refreshingStartup||startupViewChoice.getVisibility()!=View.VISIBLE||position!=startupViewChoice.getSelectedItemPosition()||(position==1)==StartupPages.favorites(library))return;if(!changeLibrary(()->library.startupFavorites=position==1))refreshStartupPageChoice();}});
  AppCompatCheckBox keepScreen=new AppCompatCheckBox(this);keepScreen.setText(R.string.keep_screen_on_fullscreen);keepScreen.setChecked(library.keepScreenOnFullscreen);keepScreen.setPadding(0,dp(12),0,dp(12));settingsContent.addView(keepScreen);boolean[] revertingScreen={false};keepScreen.setOnCheckedChangeListener((button,checked)->{if(revertingScreen[0])return;if(!changeLibrary(()->library.keepScreenOnFullscreen=checked)){revertingScreen[0]=true;keepScreen.setChecked(library.keepScreenOnFullscreen);revertingScreen[0]=false;}FullscreenScreenAwake.apply(getWindow(),fullscreen&&!pipLayout&&library.keepScreenOnFullscreen);});
  playbackPreference(settingsContent,R.string.mini_player,true);
  AppCompatCheckBox dailyGuide=new AppCompatCheckBox(this);dailyGuide.setText(R.string.daily_guide_update);dailyGuide.setChecked(library.dailyGuideUpdate);dailyGuide.setPadding(0,dp(12),0,dp(12));settingsContent.addView(dailyGuide);boolean[] revertingGuide={false};dailyGuide.setOnCheckedChangeListener((button,checked)->{if(revertingGuide[0])return;if(!changeLibrary(()->library.dailyGuideUpdate=checked)){revertingGuide[0]=true;dailyGuide.setChecked(library.dailyGuideUpdate);revertingGuide[0]=false;}});
  Button sourcesToggle=new Button(this);sourcesToggle.setText(UiText.text("Kilder"));settingsContent.addView(sourcesToggle);LinearLayout sourceContent=column();sourceContent.setPadding(dp(8),0,0,0);sourceContent.setVisibility(View.GONE);settingsContent.addView(sourceContent);sourcesToggle.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,R.drawable.ic_expand_more,0);sourcesToggle.setOnClickListener(view->{boolean expanded=sourceContent.getVisibility()!=View.VISIBLE;sourceContent.setVisibility(expanded?View.VISIBLE:View.GONE);sourcesToggle.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,expanded?R.drawable.ic_expand_less:R.drawable.ic_expand_more,0);});
  button(R.string.sources,sourceContent,()->{drawer.closeDrawer(androidx.core.view.GravityCompat.START);showSources();});
  Button musicSource=new Button(this);musicSource.setText(UiText.text("Kilde til musik"));sourceContent.addView(musicSource);musicSource.setOnClickListener(view->{drawer.closeDrawer(androidx.core.view.GravityCompat.START);MusicSourceSettings.show(this,library,(address,proxy)->{boolean saved=changeLibrary(()->{library.openSpotApiUrl=address;library.openSpotProxyUrl=proxy;});if(saved)SaavnMusic.favoriteVersion++;return saved;});});
  button(R.string.tv_guide_sources,sourceContent,()->{drawer.closeDrawer(androidx.core.view.GravityCompat.START);showGuides();});
  button(R.string.local_source_folder,sourceContent,()->{drawer.closeDrawer(androidx.core.view.GravityCompat.START);showLocalSourceOptions();});
  button(R.string.refresh_radio,sourceContent,()->{drawer.closeDrawer(androidx.core.view.GravityCompat.START);radioPanel.refreshStations();});
  button(R.string.pcloud,sourceContent,()->{drawer.closeDrawer(androidx.core.view.GravityCompat.START);showPcloudAccount();});
  for(int sourceIndex=0;sourceIndex<sourceContent.getChildCount();sourceIndex++){View sourceButton=sourceContent.getChildAt(sourceIndex);if(sourceButton instanceof Button){sourceButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF383838));((Button)sourceButton).setTextColor(Color.WHITE);}}
  button(R.string.home_buttons,settingsContent,()->{drawer.closeDrawer(androidx.core.view.GravityCompat.START);showHomeButtons();});
  button(R.string.tv_backup_lists,settingsContent,()->{drawer.closeDrawer(androidx.core.view.GravityCompat.START);showTvBackupLists();});
  Button languageButton=new Button(this);languageButton.setText("Sprog/Language");settingsContent.addView(languageButton);languageButton.setOnClickListener(view->{int[] selected={AppLanguage.saved(this).equals("en")?1:0};new AlertDialog.Builder(this).setTitle("Sprog/Language").setSingleChoiceItems(UiText.labels(new String[]{"Dansk","English"}),selected[0],(dialog,index)->selected[0]=index).setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.save,(dialog,which)->{String code=selected[0]==1?"en":"da";if(changeLibrary(()->library.appLanguage=code)){AppLanguage.choose(this,code);PlaybackNotices.rename(this);RadioWidgetProvider.refresh(this);PageWidgetProvider.refresh(this);RadioPlaybackService.refreshCarStartPage();recreate();}}).show();});
  button(R.string.dot_meanings,settingsContent,()->{drawer.closeDrawer(androidx.core.view.GravityCompat.START);showDotMeanings();});
  button(R.string.reset_history,settingsContent,()->{drawer.closeDrawer(androidx.core.view.GravityCompat.START);new AlertDialog.Builder(this).setTitle(R.string.reset_history).setMessage(R.string.confirm_reset_history).setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.reset_history,(dialog,which)->{PlaybackHistory.clear(this);PlaybackTracker.resetPlayers();CastPlayback.resetProgress(this);resumePosition=0;changeLibrary(()->library.lastPlayedPosition=0);localPanel.playbackChanged();pcloudPanel.playbackChanged();favoriteHub.refresh();adapter.notifyDataSetChanged();UiText.toast(this,"Lyttede/sete er nulstillet.",Toast.LENGTH_SHORT).show();}).show();});
  button(R.string.import_export,settingsContent,()->{drawer.closeDrawer(androidx.core.view.GravityCompat.START);showSettingsTransfer();});
  Button updateButton=button(R.string.update_all,settingsContent,()->{drawer.closeDrawer(androidx.core.view.GravityCompat.START);updateAll();});updateButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2E7D32));updateButton.setTextColor(Color.WHITE);
  Button resetButton=button(R.string.reset_app,settingsContent,()->{drawer.closeDrawer(androidx.core.view.GravityCompat.START);confirmReset();});resetButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFD32F2F));resetButton.setTextColor(Color.WHITE);
  View autoDivider=new View(this);autoDivider.setBackgroundColor(0xFF666666);LinearLayout.LayoutParams dividerSpace=new LinearLayout.LayoutParams(-1,dp(1));dividerSpace.topMargin=dp(16);dividerSpace.bottomMargin=dp(12);settingsContent.addView(autoDivider,dividerSpace);
  Button autoButton=button(R.string.android_auto,settingsContent,()->{drawer.closeDrawer(androidx.core.view.GravityCompat.START);showAndroidAutoSettings();});autoButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));autoButton.setTextColor(Color.BLACK);autoButton.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_car,0,0,0);autoButton.setCompoundDrawablePadding(dp(12));autoButton.setPadding(dp(16),autoButton.getPaddingTop(),dp(16),autoButton.getPaddingBottom());
  navigation.addView(new Space(this),new LinearLayout.LayoutParams(-1,0,1));
  TextView credit=text(getString(R.string.developer_credit),11,Color.rgb(136,136,136));credit.setGravity(Gravity.CENTER);credit.setPadding(0,dp(16),0,0);navigation.addView(credit,new LinearLayout.LayoutParams(-1,-2));
  ScrollView menu=new ScrollView(this);menu.setFillViewport(true);menu.addView(navigation);
  DrawerLayout.LayoutParams params=new DrawerLayout.LayoutParams(dp(280),-1);params.gravity=androidx.core.view.GravityCompat.START;drawer.addView(menu,params);
 }
 @androidx.annotation.ChecksSdkIntAtLeast(api=26)
 private boolean supportsMiniPlayer() {
  return android.os.Build.VERSION.SDK_INT>=26 && getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE);
 }
 private void playbackPreference(LinearLayout menu,int label,boolean mini) {
  AppCompatCheckBox toggle=new AppCompatCheckBox(this);toggle.setText(label);toggle.setChecked(mini?library.miniPlayer:library.autoPlayLast);toggle.setPadding(0,dp(12),0,dp(12));menu.addView(toggle);
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
  if(CastPlayback.hasMedia(this)||LocalAudioService.current()!=null || !RadioPlaybackService.nowPlaying().isEmpty() || !storageReadable || !library.autoPlayLast || library.lastPlayed==null || library.lastPlayed.url.isEmpty()) return;
  selected=library.lastPlayed;tvAnchor=TvFavorites.primary(library,selected);if(tvAnchor!=null)selected=tvAnchor.channel;tvBackupIndex=0;tvActiveUrl=selected.url;PlaybackMark mark=PlaybackHistory.get(this,selected.url);resumePosition=mark.duration>0?mark.resume():library.lastPlayedPosition;if(selected.url.startsWith("content:")) playLocal(new LocalMedia(selected.url,selected.name,"video/*",false),resumePosition);else play(selected);
 }
 private void rememberPlayback() {
  if(selected==null) return;
  long position=player!=null?Math.max(0,player.getCurrentPosition()):resumePosition;
  CastSession session=connectedCast();
  if(session!=null && session.getRemoteMediaClient()!=null) position=Math.max(0,session.getRemoteMediaClient().getApproximateStreamPosition());
  long saved=selected.kind==Playlist.Kind.TV?0:position;Playlist.Channel channel=tvAnchor!=null?tvAnchor.channel:selected;
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
  super.onPictureInPictureModeChanged(inPip,config);FullscreenScreenAwake.apply(getWindow(),!inPip&&fullscreen&&library.keepScreenOnFullscreen);
  if(inPip && !pipLayout) {
   pipLayout=true;pipWasFullscreen=fullscreen;collapseSettings();drawer.closeDrawers();
   if(!fullscreen) {content.removeView(playbackPanel);fullscreenHost.addView(playbackPanel,new FrameLayout.LayoutParams(-1,-1));}
   drawer.setVisibility(View.GONE);fullscreenHost.setVisibility(View.VISIBLE);playbackPanel.setVisibility(View.VISIBLE);playbackPanel.fullscreenLayout(true);
  } else if(!inPip && pipLayout) {
   pipLayout=false;
   if(!pipWasFullscreen) {fullscreenHost.removeView(playbackPanel);content.addView(playbackPanel,panelIndex,new LinearLayout.LayoutParams(-1,-2));fullscreenHost.setVisibility(View.GONE);drawer.setVisibility(View.VISIBLE);playbackPanel.fullscreenLayout(false);playbackPanel.setVisibility(videoMinimized?View.GONE:View.VISIBLE);}
  }
  playbackPanel.refresh();
 }
 private void firstRunSetup(){if(isFinishing()||isDestroyed())return;if(getSharedPreferences("app_language",0).contains("language")){StartupPermissions.request(this);return;}new AlertDialog.Builder(this).setTitle("Choose language").setItems(new String[]{"Dansk (Danish)","English"},(dialog,index)->{String code=index==1?"en":"da";if(changeLibrary(()->library.appLanguage=code)){AppLanguage.choose(this,code);if(code.equals("da"))getWindow().getDecorView().post(()->StartupPermissions.request(this));else recreate();}}).setCancelable(false).show();}
 private void requestNotificationPermission() {
  if(android.os.Build.VERSION.SDK_INT>=33 && androidx.core.content.ContextCompat.checkSelfPermission(this,android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED && !getPreferences(0).getBoolean("notifications_requested",false)) {
   getPreferences(0).edit().putBoolean("notifications_requested",true).apply();notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS);
  }
 }
 private android.app.PendingIntent videoAction(String action,int request) {
  return android.app.PendingIntent.getBroadcast(this,request,new android.content.Intent(action).setPackage(getPackageName()),android.app.PendingIntent.FLAG_UPDATE_CURRENT|android.app.PendingIntent.FLAG_IMMUTABLE);
 }
 // Android exempts genuine media-session notifications from POST_NOTIFICATIONS.
 @android.annotation.SuppressLint({"MissingPermission","NotificationPermission"})
 private void updateVideoNotification() {
  android.app.NotificationManager manager=(android.app.NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  if(!backgroundPlayback || videoSession==null || selected==null || player==null || connectedCast()!=null || player.getPlaybackState()==Player.STATE_IDLE) {manager.cancel(2002);return;}
  manager.notify(2002,PlaybackNotification.create(this,"stream_video",videoSession,videoAction(VIDEO_TOGGLE,10),videoAction(VIDEO_STOP,11),selected.kind==Playlist.Kind.TV&&!selected.url.startsWith("content:")));
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
 private void showFavoriteKind(boolean radio) {showFavoriteTab(radio?1:0);}
 private int selectedFavoriteTab(){return currentFavoriteTab;}
 private void updateFavoriteTabVisibility(){
  List<Integer> available=new ArrayList<>();List<String> labels=new ArrayList<>();int[] resources={R.string.tv,R.string.radio,R.string.local_files,R.string.pcloud,R.string.films,R.string.series};
  for(int tab:Library.favoriteTabOrder())if(library.hasFavoriteTab(tab)){available.add(tab);labels.add(getString(resources[tab]));}
  choosingFavoriteTab=true;
  if(!available.equals(favoriteCategoryIds)){favoriteCategoryIds.clear();favoriteCategoryIds.addAll(available);ArrayAdapter<String> choices=new UiArrayAdapter<String>(this,android.R.layout.simple_spinner_item,labels){
   private View bold(View view){if(view instanceof TextView)((TextView)view).setTypeface(null,android.graphics.Typeface.BOLD);return view;}
   @Override public View getView(int position,View recycled,ViewGroup parent){return bold(super.getView(position,recycled,parent));}
   @Override public View getDropDownView(int position,View recycled,ViewGroup parent){return bold(super.getDropDownView(position,recycled,parent));}
  };choices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);favoriteCategories.setAdapter(choices);}
  int position=favoriteCategoryIds.indexOf(currentFavoriteTab);if(position>=0&&favoriteCategories.getSelectedItemPosition()!=position)favoriteCategories.setSelection(position);
  favoriteCategories.setVisibility(favoritesPage&&!available.isEmpty()?View.VISIBLE:View.GONE);choosingFavoriteTab=false;
 }
 private void updateHeaderOutputs(){
  boolean home=atFavoritesHome();if(pageFooter!=null)pageFooter.setOnHome(home);if(playbackDock!=null)playbackDock.setOnHome(home);if(headerOutputs==null||headerFrame==null)return;
  boolean inHeading=!home&&destinationHeader!=null&&destinationHeader.getVisibility()==View.VISIBLE;
  ViewGroup target=inHeading?destinationRow:headerFrame;if(headerOutputs.getParent()!=target){((ViewGroup)headerOutputs.getParent()).removeView(headerOutputs);if(inHeading)destinationRow.addView(headerOutputs,new LinearLayout.LayoutParams(-2,dp(48)));else headerFrame.addView(headerOutputs,new FrameLayout.LayoutParams(-2,dp(48),Gravity.END|Gravity.CENTER_VERTICAL));}
  headerFrame.setVisibility(inHeading?View.GONE:View.VISIBLE);ViewGroup.LayoutParams headerSize=headerFrame.getLayoutParams();headerSize.height=dp(home?64:48);headerFrame.setLayoutParams(headerSize);headerBrandSlot.setVisibility(home?View.VISIBLE:View.GONE);headerOutputs.setVisibility(home?View.GONE:View.VISIBLE);if(headerMenu!=null&&!home)headerMenu.setVisibility(View.INVISIBLE);
  FrameLayout.LayoutParams position=(FrameLayout.LayoutParams)headerBrandSlot.getLayoutParams();position.leftMargin=dp(48);position.rightMargin=dp(48);headerBrandSlot.setLayoutParams(position);
 }

 private boolean atFavoritesHome(){return favoriteHubMode&&favoriteHub!=null&&favoriteHub.isHome();}
 private void showFavoritesHome(){if(recordingBlocked())return;
  minimizeVideo();drawer.closeDrawer(androidx.core.view.GravityCompat.START);favoritesPage=true;favoritesMode=true;radioMode=false;localMode=false;pcloudMode=false;favoriteHubMode=true;
  radioPanel.setVisibility(View.GONE);localPanel.setVisibility(View.GONE);pcloudPanel.setVisibility(View.GONE);list.setVisibility(View.GONE);pageTitle.setVisibility(View.GONE);sections.setVisibility(View.GONE);groupControls.setVisibility(View.GONE);search.setVisibility(View.GONE);status.setVisibility(View.GONE);favoriteCategories.setVisibility(View.GONE);favoriteHub.setVisibility(View.VISIBLE);favoriteHub.showHome();
 }
 private void refreshStartupPageChoice(){if(startupPageChoice==null)return;java.util.List<String> names=StartupPages.choices(library);if(!names.equals(startupPageNames)){startupPageNames=names;startupPageChoice.setAdapter(boldDropdown(names.toArray(new String[0])));}int index=names.indexOf(StartupPages.selected(library));if(startupPageChoice.getSelectedItemPosition()!=index)startupPageChoice.setSelection(index);if(startupViewChoice!=null){refreshingStartup=true;startupViewChoice.setVisibility((StartupPages.HOME.equals(StartupPages.selected(library))||"Podcast".equals(StartupPages.selected(library)))?View.GONE:View.VISIBLE);startupViewChoice.setSelection(StartupPages.favorites(library)?1:0);refreshingStartup=false;}}
 private void openStartupPage(){if(getIntent().getBooleanExtra("openDrawer",false)){pageShortcutLaunch=true;showFavoritesHome();return;}String shortcut=getIntent().getStringExtra(PageShortcut.EXTRA);getIntent().removeExtra(PageShortcut.EXTRA);if(PageShortcut.valid(shortcut)){pageShortcutLaunch=true;openDestination(shortcut);return;}String name=getIntent().getBooleanExtra("favoritesHome",false)?StartupPages.HOME:StartupPages.selected(library);if(StartupPages.HOME.equals(name))showFavoritesHome();else openDestination(name,StartupPages.favorites(library));}
 private void moveDestinationFilters(boolean inHeader){if(groupControls==null||pageRoot==null||destinationHeader==null)return;LinearLayout target=inHeader?destinationHeader:pageRoot;if(groupControls.getParent()==target)return;if(groupControls.getParent()!=null)((ViewGroup)groupControls.getParent()).removeView(groupControls);int index=inHeader?3:pageRoot.indexOfChild(search);LinearLayout.LayoutParams spacing=new LinearLayout.LayoutParams(-1,dp(48));if(inHeader){spacing.topMargin=dp(8);spacing.bottomMargin=dp(4);}target.addView(groupControls,index<0?target.getChildCount():index,spacing);}
 private void searchDestination(){if(destination.isEmpty())return;String value=destinationSearch.input.getText().toString();if(favoriteHubMode)favoriteHub.setSearchQuery(value);else if(radioMode)radioPanel.setSearchQuery(value);else if(localMode)localPanel.setSearchQuery(value);else if(pcloudMode)pcloudPanel.setSearchQuery(value);else search.setText(value);}
 private void openDestination(String name){openDestination(name,null);}
 private void openDestination(String name,Boolean favoriteOverride){
  if(recordingBlocked())return;if(name.equals("Musik")){showFavoritesHome();if(favoriteOverride==null)openMusic();else{android.content.Intent intent=new android.content.Intent(this,MusicActivity.class);intent.putExtra("startupFavorites",favoriteOverride.booleanValue());startActivity(intent);}return;}
  boolean saved=favoriteOverride!=null?favoriteOverride:Boolean.TRUE.equals(library.pageFavorites.get(name));int kind=java.util.Arrays.asList("TV","Film","Serier").indexOf(name);
  if(kind>=0){if(saved){showFavoritesHome();favoriteHub.selectStreamingKind(kind);favoriteHub.showList("Streaming");}else{navigate(false);currentKind=Playlist.Kind.values()[kind];kindButtons[kind].setChecked(true);rebuildGroups();}}
  else if(name.equals("Podcast")){showFavoritesHome();favoriteHub.showList("Podcast");saved=true;}
  else if(name.equals("Radio")){openRadio(false);radioPanel.open(saved);}
  else if(name.equals("Lokale filer")){openLocal(false);if(saved)localPanel.open(true,false);}
  else if(name.equals("Cloud")){if(saved){openLocal(false);localPanel.open(true,true);}else openPcloud();}else return;
  destination=name;destinationSearch.input.setText("");if(kind>=0&&!saved)moveDestinationFilters(true);destinationTitle.setText(UiText.text(saved&&name.equals("TV")?"TV-Favoritliste 1":name));FavoriteHeading.show(destinationTitle,saved&&!name.equals("Podcast"));destinationFavorites.setFavorites(saved);destinationFavorites.setVisibility(name.equals("Podcast")?View.GONE:View.VISIBLE);((LinearLayout.LayoutParams)destinationFavorites.getLayoutParams()).topMargin=dp(8);destinationHeader.setVisibility(View.VISIBLE);localPanel.setHeadingManaged(true);pcloudPanel.setHeadingManaged(true);radioPanel.setHeadingManaged(true);favoriteHub.setHeadingManaged(true);localBack.setEnabled(true);headerMenu.setVisibility(View.INVISIBLE);pageTitle.setVisibility(View.GONE);if(kind>=0)sections.setVisibility(View.GONE);updateHeaderOutputs();filter();
 }
 private void openMusic(){if(recordingBlocked())return;drawer.closeDrawer(androidx.core.view.GravityCompat.START);startActivity(new android.content.Intent(this,MusicActivity.class));}
 private void showFavoriteTab(int tab){if(tab==6){showFavoritesHome();return;}openDestination(tab==7?"Cloud":tab==1?"Radio":tab==2?"Lokale filer":tab==3?"Musik":tab==4?"Film":tab==5?"Serier":new String[]{"TV","Film","Serier"}[library.favoritesStartKind]);}

 private void openLocal(boolean favorites) {if(recordingBlocked())return;
  minimizeVideo();
  pcloudMode=false;pcloudPanel.setVisibility(View.GONE);
  exitFullscreen();drawer.closeDrawer(androidx.core.view.GravityCompat.START);list.cancelFavoriteGesture();
  if(favorites) {showFavoriteTab(2);return;}
  favoritesPage=false;favoritesMode=false;radioMode=false;localMode=true;radioPanel.setVisibility(View.GONE);localPanel.setVisibility(View.VISIBLE);localPanel.open(false);filter();
 }
 private void showPcloudAccount(){
  if(library.pcloud==null){showPcloudLogin();return;}
  drawer.closeDrawer(androidx.core.view.GravityCompat.START);
  new AlertDialog.Builder(this).setTitle(R.string.pcloud_account).setItems(UiText.labels(new String[]{"Redigér konto","Opdatér","Log ud"}),(dialog,which)->{
   switch(which){
    case 0:showPcloudLogin();break;
    case 1:if(pcloudMode)pcloudPanel.refresh();else openPcloud();break;
    case 2:logoutPcloud();break;
   }
  }).setNegativeButton(android.R.string.cancel,null).show();
 }
 private void openPcloud(){if(recordingBlocked())return;
  minimizeVideo();
  exitFullscreen();drawer.closeDrawer(androidx.core.view.GravityCompat.START);list.cancelFavoriteGesture();pcloudMode=true;localMode=false;radioMode=false;favoritesPage=false;favoritesMode=false;localPanel.setVisibility(View.GONE);radioPanel.setVisibility(View.GONE);pcloudPanel.setVisibility(View.VISIBLE);pcloudPanel.open();filter();
 }
 private void logoutPcloud(){
  if(!changeLibrary(()->library.pcloud=null))return;
  LocalMedia current=LocalAudioService.current();if(current!=null&&PcloudDocument.is(current.uri))stopService(new android.content.Intent(this,LocalAudioService.class));if(CastPlayback.pcloud(this))CastPlayback.stop(this);pcloudPanel.open();
 }
 private void showPcloudLogin(){
  LinearLayout form=column();form.setPadding(dp(20),dp(8),dp(20),dp(8));Spinner region=dropdown();ArrayAdapter<String> regions=new UiArrayAdapter<>(this,android.R.layout.simple_spinner_item,new String[]{"EU","USA"});regions.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);region.setAdapter(regions);form.addView(region);
  EditText email=new EditText(this);email.setHint("E-mail");email.setSingleLine();email.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);email.setSaveEnabled(false);form.addView(email);
  EditText password=new EditText(this);password.setHint(UiText.text("Adgangskode"));password.setSingleLine();password.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);password.setSaveEnabled(false);form.addView(password);
  if(library.pcloud!=null){region.setSelection(PcloudAccount.US.equals(library.pcloud.server)?1:0);email.setText(library.pcloud.email);}
  TextView error=text("",13,Color.LTGRAY);error.setVisibility(View.GONE);form.addView(error);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Cloud").setView(form).setNegativeButton(android.R.string.cancel,null).setPositiveButton(UiText.text("Log på"),null).create();
  dialog.setOnDismissListener(ignored->{if(pcloudLoginClient!=null){pcloudLoginClient.cancel();pcloudLoginClient=null;}password.setText("");});
  dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view->{
   PcloudAccount account;try{account=new PcloudAccount(region.getSelectedItemPosition()==1?PcloudAccount.US:PcloudAccount.EU,email.getText().toString(),password.getText().toString());}catch(IllegalArgumentException invalid){error.setText(invalid.getMessage());error.setVisibility(View.VISIBLE);return;}
   error.setText(UiText.text("Bekræft login på din mail"));error.setVisibility(View.VISIBLE);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);WebDavClient client=new WebDavClient(account);pcloudLoginClient=client;
   localWorker.execute(()->{try{client.list(okhttp3.HttpUrl.get(account.server));runOnUiThread(()->{if(isDestroyed()||!dialog.isShowing())return;if(changeLibrary(()->library.pcloud=account)){dialog.dismiss();openDestination("Cloud");}else dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);});}catch(Exception failure){runOnUiThread(()->{if(isDestroyed()||!dialog.isShowing())return;error.setText(UiText.text("Bekræft login på din mail\n\n"+(failure instanceof java.io.IOException?failure.getMessage():"Cloud-login mislykkedes.")));error.setVisibility(View.VISIBLE);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);});}});
  }));dialog.show();
 }
 private void showDotMeanings(){
  LinearLayout legend=column();legend.setPadding(dp(24),dp(8),dp(24),dp(8));int[] colors={PlaybackStatus.color(true,new PlaybackMark(0,0,false)),PlaybackStatus.color(false,new PlaybackMark(1,100,false)),PlaybackStatus.color(false,new PlaybackMark(100,100,true))};int[] labels={R.string.dot_white_meaning,R.string.dot_yellow_meaning,R.string.dot_green_meaning};
  for(int index=0;index<labels.length;index++){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(dp(56));row.setPadding(0,dp(10),0,dp(10));View dot=new View(this);android.graphics.drawable.GradientDrawable circle=new android.graphics.drawable.GradientDrawable();circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);circle.setColor(colors[index]);dot.setBackground(circle);dot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);LinearLayout.LayoutParams dotSpace=new LinearLayout.LayoutParams(dp(16),dp(16));dotSpace.setMarginEnd(dp(16));row.addView(dot,dotSpace);TextView label=text(getString(labels[index]),16,Color.WHITE);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));legend.addView(row,new LinearLayout.LayoutParams(-1,-2));}
  new AlertDialog.Builder(this).setTitle(R.string.dot_meanings).setView(legend).setPositiveButton(android.R.string.ok,null).show();
 }
 private void showTvBackupLists(){new AlertDialog.Builder(this).setTitle(UiText.text("TV backup-lister")).setItems(UiText.labels(new String[]{"TV-Favoritliste 2","TV-Favoritliste 3","TV-Favoritliste 4"}),(dialog,index)->{showFavoritesHome();favoriteHub.showTvList(index+1);}).setNegativeButton(UiText.text("Luk"),null).show();}
 private void switchTvBackup(){if(recordingBlocked())return;if(tvAnchor==null)return;TvFavorites.Match match=TvFavorites.next(library,tvAnchor,tvBackupIndex,selected==null?tvActiveUrl:selected.url);if(match==null){UiText.toast(this,"Ingen anden TV-liste har en kanal med samme navn.",Toast.LENGTH_SHORT).show();return;}tvBackupIndex=match.index;tvActiveUrl=match.entry.channel.url;play(match.entry.channel);playbackPanel.showControls();if(adapter!=null)adapter.notifyDataSetChanged();favoriteHub.programmeChanged();}
 private void showGroupFavoriteMenu(View anchor,String group){PopupMenu menu=new StablePopupMenu(this,anchor);menu.getMenu().add(UiText.text("Tilføj gruppe til favoritter")).setOnMenuItemClickListener(item->{addGroupToFavorites(group);return true;});if(currentKind!=Playlist.Kind.TV){List<Library.Entry> entries=new ArrayList<>();if(!sourceCatalogs.isEmpty())entries.addAll(combinedCatalog.filter(currentKind,group,""));else if(activeSource!=null)for(Playlist.Channel channel:catalog.filter(currentKind,group,""))entries.add(new Library.Entry(activeSource.id,channel));MediaDownloads.addGroup(menu,this,entries,group);}menu.show();}
 private void addGroupToFavorites(String group){List<Library.Entry> entries=new ArrayList<>();if(!sourceCatalogs.isEmpty())entries.addAll(combinedCatalog.filter(currentKind,group,""));else if(activeSource!=null)for(Playlist.Channel channel:catalog.filter(currentKind,group,""))entries.add(new Library.Entry(activeSource.id,channel));if(entries.isEmpty())return;String[] choices=currentKind==Playlist.Kind.TV?new String[]{"TV-Favoritliste 1","TV-Favoritliste 2","TV-Favoritliste 3","TV-Favoritliste 4"}:new String[]{currentKind==Playlist.Kind.FILM?"Film":"Serier"};new AlertDialog.Builder(this).setTitle(UiText.text("Tilføj gruppen til favoritliste")).setItems(UiText.labels(choices),(dialog,index)->{if(changeLibrary(()->{if(entries.get(0).channel.kind==Playlist.Kind.TV)TvFavorites.addGroup(library,entries,index);else{for(Library.Entry entry:entries)FavoriteLists.assignStreaming(library,FavoriteLists.of(entry),entry.channel.kind.ordinal());List<String> order=new ArrayList<>();for(FavoriteLists.Item item:FavoriteLists.items(library,"Streaming"))order.add(item.key());for(Library.Entry entry:entries)order.remove(FavoriteLists.of(entry).key());for(Library.Entry entry:entries)order.add(FavoriteLists.of(entry).key());library.favoriteOrder.removeAll(order);library.favoriteOrder.addAll(order);}})){UiText.toast(this,entries.size()+" kanaler tilføjet til "+choices[index],Toast.LENGTH_SHORT).show();filter();}}).setNegativeButton(UiText.text("Annuller"),null).show();}
 private void showAndroidAutoSettings(){
  LinearLayout content=column();content.setPadding(dp(20),dp(8),dp(20),0);TextView explanation=text(UiText.text("Vælg den side, der åbnes i Android Auto. Du kan altid åbne de andre sider via Alle sider."),15,Color.LTGRAY);content.addView(explanation);TextView heading=text(UiText.text("Standardside"),16,Color.WHITE);heading.setTypeface(null,android.graphics.Typeface.BOLD);heading.setPadding(0,dp(16),0,dp(4));content.addView(heading);
  RadioGroup choices=new RadioGroup(this);choices.setOrientation(RadioGroup.VERTICAL);content.addView(choices);String[] selected={AndroidAutoPages.normalize(library.androidAutoStartPage)};
  for(int i=0;i<AndroidAutoPages.IDS.length;i++){final String page=AndroidAutoPages.IDS[i];RadioButton choice=new RadioButton(this);choice.setId(View.generateViewId());choice.setText(UiText.text(AndroidAutoPages.LABELS[i]));choice.setMinHeight(dp(52));choices.addView(choice,new RadioGroup.LayoutParams(-1,-2));choice.setChecked(page.equals(selected[0]));choice.setOnCheckedChangeListener((button,checked)->{if(checked)selected[0]=page;});}
  TextView viewLabel=text(UiText.text("Visning"),16,Color.WHITE);content.addView(viewLabel);Spinner view=dropdown();view.setAdapter(boldDropdown(new String[]{"Alle","Favoritter"}));view.setSelection(library.androidAutoFavorites?1:0);content.addView(view);
  AppCompatCheckBox autoplay=new AppCompatCheckBox(this);autoplay.setText(UiText.text("Afspil automatisk seneste"));autoplay.setChecked(library.androidAutoAutoPlay);autoplay.setPadding(0,dp(12),0,dp(12));content.addView(autoplay);ScrollView scroll=new ScrollView(this);scroll.addView(content);
  new AlertDialog.Builder(this).setTitle(R.string.android_auto).setView(scroll).setNegativeButton(UiText.text("Annuller"),null).setPositiveButton(UiText.text("Gem"),(dialog,which)->{if(changeLibrary(()->{library.androidAutoStartPage=selected[0];library.androidAutoFavorites=view.getSelectedItemPosition()==1;library.androidAutoAutoPlay=autoplay.isChecked();}))RadioPlaybackService.refreshCarStartPage();}).show();
 }
 private void showHomeButtons(){
  String[] names=FavoriteHomeOrder.NAMES.clone();boolean[] checked=new boolean[names.length];for(int i=0;i<names.length;i++)checked[i]=!library.hiddenHomeButtons.contains(names[i]);
  new AlertDialog.Builder(this).setTitle(R.string.home_buttons).setMultiChoiceItems(UiText.labels(names),checked,(dialog,which,selected)->checked[which]=selected).setNegativeButton(UiText.text("Annuller"),null).setPositiveButton(UiText.text("Gem"),(dialog,which)->{if(changeLibrary(()->{library.hiddenHomeButtons.clear();for(int i=0;i<names.length;i++)if(!checked[i])library.hiddenHomeButtons.add(names[i]);}))favoriteHub.rebuildHome();}).show();
 }
 private void showSettingsTransfer(){
  if(settingsTransferBusy){UiText.toast(this,"Vent, til overførslen er færdig.",Toast.LENGTH_SHORT).show();return;}
  new AlertDialog.Builder(this).setTitle(R.string.import_export).setItems(UiText.labels(new String[]{"Importér opsætning","Eksportér opsætning"}),(dialog,which)->{
   android.content.Intent intent=new android.content.Intent(which==0?android.content.Intent.ACTION_OPEN_DOCUMENT:android.content.Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
   if(which==0){intent.setType("*/*");intent.putExtra(android.content.Intent.EXTRA_MIME_TYPES,new String[]{"application/json","text/plain","application/octet-stream"});}else {intent.setType("application/json");intent.putExtra(android.content.Intent.EXTRA_TITLE,UiText.text("Stream-opsætning.json"));}
   try{startActivityForResult(intent,which==0?7210:7211);}catch(android.content.ActivityNotFoundException unavailable){UiText.toast(this,"Telefonen har ingen filvælger.",Toast.LENGTH_LONG).show();}
  }).setNegativeButton(android.R.string.cancel,null).show();
 }
 private void exportSettings(android.net.Uri uri){
  if(settingsTransferBusy)return;if(!storageReadable){UiText.toast(this,R.string.storage_failed,Toast.LENGTH_LONG).show();return;}
  final String settings;final Map<String,Map<String,Object>> preferences;
  try{PlaybackTracker.flushAll();CastHistory.capture(true);PlaybackHistory.flush(this);settings=library.toJson();preferences=AppSettingsState.capture(this);}catch(Exception invalid){UiText.toast(this,"Kunne ikke eksportere opsætningen.",Toast.LENGTH_LONG).show();return;}settingsTransferBusy=true;
  settingsWorker.execute(()->{boolean success=false;try{byte[] bytes=SettingsBackup.encode(Library.fromJson(settings),preferences);try(java.io.OutputStream output=getContentResolver().openOutputStream(uri,"wt")){if(output==null)throw new java.io.IOException();output.write(bytes);output.flush();}success=true;}catch(Exception failure){}boolean saved=success;runOnUiThread(()->{if(isDestroyed())return;settingsTransferBusy=false;UiText.toast(this,saved?"Hele opsætningen er eksporteret, inklusive logins og afspilningshistorik.":"Kunne ikke eksportere opsætningen.",Toast.LENGTH_LONG).show();});});
 }
 private void importSettings(android.net.Uri uri){
  if(settingsTransferBusy)return;settingsTransferBusy=true;
  settingsWorker.execute(()->{try{SettingsBackup.Snapshot imported;try(java.io.InputStream input=getContentResolver().openInputStream(uri)){imported=SettingsBackup.decodeSnapshot(input);}runOnUiThread(()->applyImportedSettings(imported));}catch(Exception invalid){runOnUiThread(()->{if(isDestroyed())return;settingsTransferBusy=false;UiText.toast(this,"Filen kunne ikke importeres. Den nuværende opsætning er bevaret.",Toast.LENGTH_LONG).show();});}});
 }
 private void applyImportedSettings(SettingsBackup.Snapshot imported){
  if(isDestroyed()||isFinishing())return;
  final Library previous;final Map<String,Map<String,Object>> previousPreferences;
  try{PlaybackTracker.flushAll();CastHistory.capture(true);PlaybackHistory.flush(this);previous=Library.fromJson(library.toJson());previousPreferences=AppSettingsState.capture(this);}catch(Exception failure){settingsTransferBusy=false;UiText.toast(this,"Opsætningen kunne ikke importeres.",Toast.LENGTH_LONG).show();return;}
  applyingSettings=true;PlaybackHistory.suspend();PlaybackTracker.detachAll();PlaybackShutdown.stop(this);stopPlaybackState();
  settingsWorker.execute(()->{try{store.save(imported.library);AppSettingsState.restore(this,imported.preferences);runOnUiThread(()->{PlaybackHistory.reload();finishImportedSettings(imported.library);});}catch(Exception failure){boolean recovered=true;try{store.save(previous);AppSettingsState.restore(this,previousPreferences);}catch(Exception rollbackFailure){recovered=false;}boolean restored=recovered;runOnUiThread(()->{PlaybackHistory.reload();if(isDestroyed())return;applyingSettings=false;settingsTransferBusy=false;UiText.toast(this,restored?"Import mislykkedes. Den tidligere opsætning er gendannet.":"Import mislykkedes. Opsætningen kunne ikke gendannes fuldt. Prøv at importere igen.",Toast.LENGTH_LONG).show();});}});
 }
 private void finishImportedSettings(Library imported){
  if(isDestroyed()||isFinishing())return;getPreferences(0).edit().remove("playlist").apply();
  library=imported;AppLanguage.choose(this,library.appLanguage);PageWidgetProvider.refresh(this);storageReadable=true;DailyGuideJob.sync(this,library);RadioWidgetProvider.refresh(this);ImageSession.favoritesChanged();
  boolean folderAccess=hasLocalFolderAccess(imported);
  UiText.toast(this,folderAccess?"Hele opsætningen er importeret.":"Opsætningen er importeret. Vælg kildemappen igen under Kildemappe til lokale filer for at give adgang.",Toast.LENGTH_LONG).show();recreate();
 }
 private boolean hasLocalFolderAccess(Library settings){
  for(LocalMedia entry:settings.localFiles)if(entry.folder&&!PcloudDocument.is(entry.uri)){
   boolean allowed=false;android.net.Uri root=android.net.Uri.parse(entry.uri);
   for(android.content.UriPermission grant:getContentResolver().getPersistedUriPermissions())try{if(grant.isReadPermission()&&java.util.Objects.equals(root.getAuthority(),grant.getUri().getAuthority())&&android.provider.DocumentsContract.getTreeDocumentId(root).equals(android.provider.DocumentsContract.getTreeDocumentId(grant.getUri()))){allowed=true;break;}}catch(IllegalArgumentException invalid){}
   if(!allowed)return false;
  }return true;
 }
 private void showLocalSourceOptions() {
  new android.app.AlertDialog.Builder(this).setTitle(R.string.local_source_folder)
   .setItems(UiText.labels(new String[]{"Vælg kildemappe","Ingen lokale filer"}),(dialog,which)->{
    replacingLocalFolder=null;
    if(which==0) {pickLocal(true);return;}
    changeLibrary(()->library.localFiles.clear());
    if(localMode&&!favoritesPage) localPanel.sourceChanged();
   }).setNegativeButton(UiText.text("Annuller"),null).show();
 }
 private void pickLocal(boolean folder) {
  android.content.Intent intent=new android.content.Intent(folder?android.content.Intent.ACTION_OPEN_DOCUMENT_TREE:android.content.Intent.ACTION_OPEN_DOCUMENT);
  intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION|android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
  if(folder && replacingLocalFolder!=null && android.os.Build.VERSION.SDK_INT>=26) intent.putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI,android.net.Uri.parse(replacingLocalFolder));
  if(!folder) {intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);intent.setType("*/*");intent.putExtra(android.content.Intent.EXTRA_MIME_TYPES,new String[]{"audio/*","video/*","image/*"});intent.putExtra(android.content.Intent.EXTRA_ALLOW_MULTIPLE,true);}
  try {startActivityForResult(intent,folder?7202:7201);}catch(android.content.ActivityNotFoundException error) {UiText.toast(this,"Ingen filvælger er tilgængelig på enheden.",Toast.LENGTH_LONG).show();}
 }
 @Override protected void onSaveInstanceState(Bundle state) {state.putString("replacingLocalFolder",replacingLocalFolder);state.putBoolean("videoMinimized",videoMinimized);super.onSaveInstanceState(state);}
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==7320){if(results.length>0&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)confirmTvRecording();else UiText.toast(this,"Tillad adgang for at optage og vise optagelsesnotifikationen.",Toast.LENGTH_LONG).show();}if(request==7303&&results.length>0&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)headerCast.performClick();if(request==7301&&results.length>0&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED){castRouteButton.performClick();}if(request==7300&&pendingCastFile!=null){LocalMedia file=pendingCastFile;pendingCastFile=null;if(results.length>0&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)playLocal(file,pendingCastPosition,pendingCastQueue==null?Collections.singletonList(file):pendingCastQueue);else UiText.toast(this,"Adgang til lokalt netværk kræves for at caste lokale filer.",Toast.LENGTH_LONG).show();}}
 @Override protected void onActivityResult(int request,int result,android.content.Intent data) {
  super.onActivityResult(request,result,data);
  if(request==7210||request==7211){if(result==RESULT_OK&&data!=null&&data.getData()!=null){if(request==7210)importSettings(data.getData());else exportSettings(data.getData());}return;}
  if((request!=7201&&request!=7202)||result!=RESULT_OK||data==null) return;
  boolean folder=request==7202;String replace=folder?replacingLocalFolder:null;replacingLocalFolder=null;List<android.net.Uri> uris=new ArrayList<>();
  if(data.getClipData()!=null) for(int i=0;i<data.getClipData().getItemCount();i++) uris.add(data.getClipData().getItemAt(i).getUri());else if(data.getData()!=null) uris.add(data.getData());
  int grants=data.getFlags()&android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
  localWorker.execute(()->{
   List<LocalMedia> linked=new ArrayList<>();boolean failed=false;
   for(android.net.Uri uri:uris) try {if(grants==0) throw new SecurityException("Read grant missing");getContentResolver().takePersistableUriPermission(uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);LocalMedia entry=LocalMedia.describe(this,uri,folder);if(!entry.supported()) throw new IllegalArgumentException("Unsupported media");linked.add(entry);}catch(Exception error) {failed=true;}
   boolean incomplete=failed;runOnUiThread(()->{if(isDestroyed()) return;if(!linked.isEmpty()) changeLibrary(()->{for(LocalMedia entry:linked) {if(folder) library.setLocalSource(entry);else library.addLocal(entry);}});if(localMode) {if(folder&&!linked.isEmpty()&&!favoritesPage) localPanel.sourceChanged();else localPanel.reload();}if(incomplete) UiText.toast(this,"En eller flere filer kunne ikke tilføjes. Vælg lyd/video fra en placering med varig læseadgang.",Toast.LENGTH_LONG).show();});
  });
 }
 private void playLocal(LocalMedia entry) {playLocal(entry,PlaybackHistory.get(this,entry.uri).resume());}
 private void playLocal(LocalMedia entry,long position) {playLocal(entry,position,localPanel.playbackEntries());}
 private void playLocal(LocalMedia entry,long position,List<LocalMedia> queue) {
  if(recordingBlocked())return;PlaybackKeyboard.hide(this);
  if(position<=0)position=PlaybackHistory.get(this,entry.uri).resume();
  if(entry.image()){if(player!=null&&(LocalAudioService.current()==null||!LocalAudioService.current().audio()))player.pause();startActivity(ImageViewerActivity.intent(this,entry,queue));return;}
  if(CastPlayback.connected(this)){if(android.os.Build.VERSION.SDK_INT>=37&&checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK")!=android.content.pm.PackageManager.PERMISSION_GRANTED){pendingCastFile=entry;pendingCastPosition=position;pendingCastQueue=new ArrayList<>(queue);requestPermissions(new String[]{"android.permission.ACCESS_LOCAL_NETWORK"},7300);return;}CastPlayback.playLocal(this,entry,queue,position);return;}
  LocalMedia current=LocalAudioService.current();
  if(current!=null&&current.uri.equals(entry.uri)) {return;}
  videoMinimized=false;requestNotificationPermission();
  if(player!=null&&!localPlayerAttached) {stopPlaybackState();if(videoSession!=null) {videoSession.release();videoSession=null;}PlaybackTracker.detach(player);BluetoothOutputButton.unregister(player);player.release();player=null;video.setPlayer(null);}
  acceptLocalPlayback=true;resumeLocalPlayback=false;
  stopService(new android.content.Intent(this,RadioPlaybackService.class));
  try {androidx.core.content.ContextCompat.startForegroundService(this,LocalAudioService.playIntent(this,entry,queue).putExtra("position",position));}
  catch(RuntimeException error) {android.util.Log.e("StreamLocalPlayback","Could not start media service",error);UiText.toast(this,"Afspilningen kunne ikke startes. Prøv igen, mens appen er åben.",Toast.LENGTH_LONG).show();}
 }
 private void detachLocalPlayback() {
  if(!localPlayerAttached) return;
  video.setPlayer(null);player=null;selected=null;localPlayerAttached=false;video.setKeepScreenOn(false);
 }
 private void syncLocalPlayback() {
  if(localPanel!=null) localPanel.playbackChanged();if(pcloudPanel!=null)pcloudPanel.playbackChanged();
  LocalMedia entry=LocalAudioService.current();ExoPlayer shared=LocalAudioService.player();
  if(entry==null||shared==null) {if(localPlayerAttached) {detachLocalPlayback();exitFullscreen();playbackPanel.setVisibility(View.GONE);updateMiniBack();}return;}
  if(!acceptLocalPlayback) return;
  if(player!=null&&!localPlayerAttached) {if(videoSession!=null) {videoSession.release();videoSession=null;}PlaybackTracker.detach(player);BluetoothOutputButton.unregister(player);player.release();}
  player=shared;localPlayerAttached=true;resumeLocalPlayback=false;
  if(entry.audio()) {selected=null;video.setPlayer(null);video.setKeepScreenOn(false);exitFullscreen();playbackPanel.setVisibility(View.GONE);}
  else {selected=new Playlist.Channel(entry.name,entry.uri,"",Playlist.Kind.FILM,null);video.setPlayer(player);video.setVisibility(View.VISIBLE);playbackPanel.setTitle(entry.name);playbackPanel.setVisibility(fullscreen||pipLayout||!videoMinimized?View.VISIBLE:View.GONE);video.setKeepScreenOn(FullscreenScreenAwake.keep(fullscreen&&!pipLayout,library.keepScreenOnFullscreen,(pipLayout||!videoMinimized)&&player.isPlaying()));}
  playbackPanel.refresh();updateMiniBack();hideFullscreenSystemBars();
 }
 private void openRadio(boolean favorites) {if(recordingBlocked())return;
  minimizeVideo();
  pcloudMode=false;if(pcloudPanel!=null)pcloudPanel.setVisibility(View.GONE);localMode=false;localPanel.setVisibility(View.GONE);localBack.setEnabled(false);
  exitFullscreen();list.cancelFavoriteGesture();drawer.closeDrawer(androidx.core.view.GravityCompat.START);
  if(favorites) {showFavoriteKind(true);return;}
  favoritesPage=false;favoritesMode=false;radioMode=true;radioPanel.setVisibility(View.VISIBLE);filter();radioPanel.open(false);
 }
 private void navigate(boolean favorites) {if(recordingBlocked())return;
  minimizeVideo();
  if(favorites) {drawer.closeDrawer(androidx.core.view.GravityCompat.START);showFavoritesHome();return;}
  pcloudMode=false;if(pcloudPanel!=null)pcloudPanel.setVisibility(View.GONE);localMode=false;localPanel.setVisibility(View.GONE);localBack.setEnabled(false);favoritesPage=false;list.cancelFavoriteGesture();radioMode=false;radioPanel.setVisibility(View.GONE);drawer.closeDrawer(androidx.core.view.GravityCompat.START);favoritesMode=favorites;search.setText("");chooseFirstKind();rebuildGroups();

 }
 private void chooseFirstKind() {
  int named=java.util.Arrays.asList("TV","Film","Serier").indexOf(destination);if(named>=0){currentKind=Playlist.Kind.values()[named];kindButtons[named].setChecked(true);return;}
  if(favoritesMode && !library.favorites.isEmpty()) currentKind=library.favorites.get(0).channel.kind;
  else if(!favoritesMode && !catalog.items.isEmpty()) currentKind=catalog.items.get(0).kind;
  kindButtons[currentKind.ordinal()].setChecked(true);
 }
 private void rebuildGroups() {
  LinkedHashSet<String> names=new LinkedHashSet<>();
  if(favoritesMode) {for(Library.Entry entry:library.favorites) if(entry.channel.kind==currentKind && !entry.channel.group.isEmpty()) names.add(entry.channel.group);}
  else names.addAll(catalog.groups(currentKind));
  List<String> labels=new ArrayList<>();labels.add(getString(R.string.all_groups));labels.addAll(names);
  ArrayAdapter<String> choices=new UiArrayAdapter<String>(this,android.R.layout.simple_spinner_item,labels){@Override public View getView(int position,View recycled,ViewGroup parent){return parent instanceof ListView?super.getDropDownView(position,recycled,parent):super.getView(position,recycled,parent);}};choices.setDropDownViewResource(R.layout.group_dropdown_item);groups.setAdapter(choices);filter();
 }
 private void filter() {
  updateHeaderOutputs();
  if(adapter==null) return;if(favoriteHubMode){favoriteHub.refresh();return;}
  int previousFavoriteTab=selectedFavoriteTab();updateFavoriteTabVisibility();
  if(favoritesPage){int available=Math.max(0,library.availableFavoriteTab(previousFavoriteTab));if(available!=previousFavoriteTab){showFavoriteTab(available);return;}}

  list.setVisibility(radioMode||localMode||pcloudMode?View.GONE:View.VISIBLE);
  if(radioMode||localMode||pcloudMode) {pageTitle.setVisibility(View.GONE);sections.setVisibility(View.GONE);groupControls.setVisibility(View.GONE);search.setVisibility(View.GONE);status.setVisibility(View.GONE);return;}
  pageTitle.setVisibility(favoritesMode||!destination.isEmpty()?View.GONE:View.VISIBLE);
  pageTitle.setText(favoritesMode?"":getString(R.string.streaming_title));
  String group=groups.getSelectedItemPosition()>0?String.valueOf(groups.getSelectedItem()):null;
  sections.setVisibility(favoritesMode||!destination.isEmpty()?View.GONE:View.VISIBLE);
  groupControls.setVisibility(favoritesMode?View.GONE:View.VISIBLE);
  search.setVisibility(favoritesPage||!destination.isEmpty()?View.GONE:View.VISIBLE);
  if(favoritesMode) {visible=new ArrayList<>();String query="";for(Library.Entry entry:library.favorites) if(entry.channel.kind==Library.favoriteKind(selectedFavoriteTab())&&entry.channel.name.toLowerCase(Locale.ROOT).contains(query)) visible.add(entry);}
  else {
   visible=new ArrayList<>();
   if(!sourceCatalogs.isEmpty())visible.addAll(combinedCatalog.filter(currentKind,group,search.getText().toString()));else if(activeSource!=null)for(Playlist.Channel channel:catalog.filter(currentKind,group,search.getText().toString()))visible.add(new Library.Entry(activeSource.id,channel));
  }
  adapter.notifyDataSetChanged();
  if(playbackPanel!=null && playbackPanel.getVisibility()==View.VISIBLE && selected!=null) {status.setVisibility(View.GONE);return;}
  if(!loading) {status.setText("");status.setVisibility(View.GONE);}
 }
 private boolean changeLibrary(Runnable change) {
  if(applyingSettings)return false;
  if(!storageReadable) {status.setText(R.string.storage_failed);return false;}
  String previous;
  try {previous=library.toJson();} catch(Exception e) {status.setText(R.string.storage_failed);return false;}
  try {change.run();store.save(library);if(startupPageChoice!=null)startupPageChoice.post(this::refreshStartupPageChoice);DailyGuideJob.sync(this,library);RadioWidgetProvider.refresh(this);if(favoriteCategories!=null&&favoritesPage)favoriteCategories.post(()->{if(!isDestroyed()&&favoritesPage)filter();});return true;}
  catch(Exception e) {
   try {library=Library.fromJson(previous);} catch(Exception ignored) {storageReadable=false;}
   status.setText(R.string.storage_failed);UiText.toast(this,R.string.storage_failed,Toast.LENGTH_LONG).show();return false;
  }
 }
 private void showSources() {
  if(loading||updatingAll) {UiText.toast(this,R.string.wait_loading,Toast.LENGTH_SHORT).show();return;}
  List<Library.Source> sources=new ArrayList<>(library.sources);
  LinearLayout actions=column();actions.setPadding(dp(16),dp(8),dp(16),dp(12));
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle(R.string.sources)
   .setAdapter(new UiArrayAdapter<>(this,android.R.layout.simple_list_item_1,sources),(d,position)->loadSource(sources.get(position))).setView(actions).create();
  button(R.string.add_m3u,actions,()->{dialog.dismiss();showAddSource(false);});
  button(R.string.xtream_login,actions,()->{dialog.dismiss();showAddSource(true);});
  button(android.R.string.cancel,actions,dialog::dismiss);
  dialog.setOnShowListener(d->dialog.getListView().setOnItemLongClickListener((parent,view,position,id)->{
   Library.Source source=sources.get(position);
   new AlertDialog.Builder(this).setTitle(source.name).setItems(UiText.labels(new String[]{getString(R.string.edit_source),getString(R.string.remove_source)}),(choice,which)->{
    if(which==0) {dialog.dismiss();showSourceEditor(source,source.xtream);}
    else new AlertDialog.Builder(this).setTitle(R.string.remove_source).setMessage(getString(R.string.remove_source_note,source.name))
     .setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.remove_source,(confirmation,selected)->{
      if(changeLibrary(()->library.removeSource(source.id))) {
       sourceCatalogs.remove(source.id);cachedSourceConfigs.remove(source.id);rebuildStreamingCatalog();
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
  Spinner epg=dropdown();ArrayAdapter<String> epgChoices=new UiArrayAdapter<>(this,android.R.layout.simple_spinner_item,labels);epgChoices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);epg.setAdapter(epgChoices);form.addView(epg);
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
  EpgData guide=epgData.get(source.epgId);EpgData.Programme programme=guide==null?null:guide.current(entry.channel,now);
  if(programme==null||programme.title.trim().isEmpty()){
   if(!updatingAll&&favoriteHubMode&&favoriteHub!=null&&favoriteHub.tvListIndex()>=0&&library.isFavorite(entry)&&guideRecovery.request(source.epgId,android.os.SystemClock.elapsedRealtime(),epgPending.contains(source.epgId))){String id=source.epgId;list.post(()->{if(isDestroyed()||isFinishing()||updatingAll)return;Library.Guide assigned=library.findGuide(id);if(assigned!=null)refreshGuide(assigned,true,true);});}
   return getString(epgPending.contains(source.epgId)?R.string.epg_loading:R.string.epg_no_programme);
  }
  return programme.title;
 }
 private void loadAssignedGuides(boolean force) {
  if(epgRepository==null || updatingAll) return;
  Set<String> ids=new LinkedHashSet<>();for(Library.Source source:library.sources) if(!source.epgId.isEmpty()) ids.add(source.epgId);
  for(String id:ids) {Library.Guide guide=library.findGuide(id);if(guide!=null) refreshGuide(guide,force);}
 }
 private void refreshGuide(Library.Guide guide,boolean force){refreshGuide(guide,force,false);}
 private void refreshGuide(Library.Guide guide,boolean force,boolean quiet) {
  long now=System.currentTimeMillis();Long attempted=epgAttempted.get(guide.id);
  if(epgPending.contains(guide.id)||(!force && attempted!=null && now-attempted<5*60*1000L)) return;
  epgPending.add(guide.id);epgAttempted.put(guide.id,now);
  epgWorker.execute(()->{
   EpgData cached=epgRepository.cached(guide);
   if(cached!=null) runOnUiThread(()->acceptGuide(guide,cached));
   try {if(force) {EpgData fetched=epgRepository.fetch(guide,System.currentTimeMillis());runOnUiThread(()->acceptGuide(guide,fetched));}}
   catch(Exception ignored) {if(force&&!quiet) runOnUiThread(()->{if(!isDestroyed()) UiText.toast(this,getString(R.string.epg_fetch_failed,guide.name),Toast.LENGTH_LONG).show();});}
   finally {runOnUiThread(()->{if(!isDestroyed()) {epgPending.remove(guide.id);adapter.notifyDataSetChanged();if(favoriteHub!=null)favoriteHub.programmeChanged();Library.Guide current=library.findGuide(guide.id);if(current!=null && !current.url.equals(guide.url)) {epgAttempted.remove(guide.id);refreshGuide(current,true);}}});}
  });
 }
 private void acceptGuide(Library.Guide guide,EpgData data) {
  if(isDestroyed()) return;Library.Guide current=library.findGuide(guide.id);
  if(current!=null && current.url.equals(guide.url)) {epgData.put(guide.id,data);adapter.notifyDataSetChanged();if(favoriteHub!=null)favoriteHub.programmeChanged();}
 }
 private void showGuides() {
  if(loading||updatingAll) {UiText.toast(this,R.string.wait_loading,Toast.LENGTH_SHORT).show();return;}
  List<Library.Guide> guides=new ArrayList<>(library.guides);
  new AlertDialog.Builder(this).setTitle(R.string.tv_guide_sources).setAdapter(new UiArrayAdapter<>(this,android.R.layout.simple_list_item_1,guides),(dialog,position)->showGuideEditor(guides.get(position)))
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
    if(changeLibrary(()->library.removeGuide(existing.id))) {epgData.remove(existing.id);epgRepository.remove(existing);adapter.notifyDataSetChanged();if(favoriteHub!=null)favoriteHub.programmeChanged();}
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
   if(manager==null || !manager.clearApplicationUserData()) UiText.toast(this,R.string.reset_failed,Toast.LENGTH_LONG).show();
  }).show();
 }
 private void updateAll() {
  if(loading||updatingAll) {UiText.toast(this,R.string.wait_loading,Toast.LENGTH_SHORT).show();return;}
  cancelCatalogRestore();updatingAll=true;radioPanel.refreshStations();
  List<Library.Source> sources=new ArrayList<>(library.sources);List<Library.Guide> guides=new ArrayList<>(library.guides);
  TextView progress=text(getString(R.string.updating_all),14,Color.LTGRAY);progress.setPadding(dp(24),dp(16),dp(24),dp(16));
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle(R.string.update_all).setView(progress).setCancelable(false).create();dialog.show();
  worker.execute(()->{
   Map<String,Catalog> catalogs=new LinkedHashMap<>();Map<String,EpgData> schedules=new LinkedHashMap<>();List<String> failures=new ArrayList<>();int completed=0,total=sources.size()+guides.size();
   for(Library.Source source:sources) {
    final int step=++completed;runOnUiThread(()->{if(!isDestroyed()) progress.setText(getString(R.string.update_progress,step,total,source.name));});
    try {Catalog fetched=source.xtream?new XtreamClient(source.server,source.username,source.password).load():new Catalog(new PlaylistLoader().load(source.server));new CatalogStore(this).save(source,fetched);catalogs.put(source.id,fetched);} catch(Exception e) {failures.add(source.name);}
   }
   for(Library.Guide guide:guides) {
    final int step=++completed;runOnUiThread(()->{if(!isDestroyed()) progress.setText(getString(R.string.update_progress,step,total,guide.name));});
    try {schedules.put(guide.id,epgRepository.fetch(guide,System.currentTimeMillis()));} catch(Exception e) {failures.add(guide.name);}
   }
   runOnUiThread(()->{
    updatingAll=false;if(isDestroyed()) return;dialog.dismiss();
    boolean saved=changeLibrary(()->{for(Map.Entry<String,Catalog> entry:catalogs.entrySet()) library.refreshFavorites(entry.getKey(),entry.getValue().items);});
    for(Library.Source source:sources)if(catalogs.containsKey(source.id)){sourceCatalogs.put(source.id,catalogs.get(source.id));cachedSourceConfigs.put(source.id,source);}rebuildStreamingCatalog();
    epgData.putAll(schedules);if(favoriteHub!=null)favoriteHub.programmeChanged();for(String id:schedules.keySet()) epgAttempted.put(id,System.currentTimeMillis());rebuildGroups();
    String result=getResources().getQuantityString(R.plurals.updated_sources,catalogs.size(),catalogs.size())+"\n"+getResources().getQuantityString(R.plurals.updated_guides,schedules.size(),schedules.size())+"\n"+getResources().getQuantityString(R.plurals.update_failures,failures.size(),failures.size());
    if(!failures.isEmpty()) result+="\n\n"+getString(R.string.update_failed_names,android.text.TextUtils.join(", ",failures));
    if(!saved) result+="\n\n"+getString(R.string.storage_failed);
    new AlertDialog.Builder(this).setTitle(R.string.update_all).setMessage(result).setPositiveButton(android.R.string.ok,null).show();
   });
  });
 }
 private void busy(boolean value) {loading=value;list.setEnabled(!value);}
 private void cancelCatalogRestore(){catalogRestoreGeneration++;if(catalogRestoreTask!=null)catalogRestoreTask.cancel(true);}
 private void restoreCatalogs(){
  List<Library.Source> sources=new ArrayList<>(library.sources);
  int restoreGeneration=++catalogRestoreGeneration;catalogRestoreTask=catalogReader.submit(()->{Map<Library.Source,Catalog> saved=new LinkedHashMap<>();CatalogStore cache=new CatalogStore(this);for(Library.Source source:sources){if(Thread.currentThread().isInterrupted())return;Catalog data=cache.load(source);if(data!=null)saved.put(source,data);}runOnUiThread(()->{if(isDestroyed()||applyingSettings||restoreGeneration!=catalogRestoreGeneration)return;for(Map.Entry<Library.Source,Catalog> entry:saved.entrySet()){Library.Source current=library.findSource(entry.getKey().id);if(current!=null&&CombinedCatalog.sameConnection(current,entry.getKey())&&!sourceCatalogs.containsKey(current.id)){sourceCatalogs.put(current.id,entry.getValue());cachedSourceConfigs.put(current.id,current);}}rebuildStreamingCatalog();if(!favoriteHubMode)chooseFirstKind();rebuildGroups();});});
 }
 private void rebuildStreamingCatalog(){
  Set<String> keep=new HashSet<>();for(Library.Source source:library.sources){keep.add(source.id);if(!CombinedCatalog.sameConnection(source,cachedSourceConfigs.get(source.id)))sourceCatalogs.remove(source.id);}
  sourceCatalogs.keySet().retainAll(keep);cachedSourceConfigs.keySet().retainAll(keep);combinedCatalog=new CombinedCatalog(library.sources,sourceCatalogs);catalog=combinedCatalog.catalog;activeSource=null;
 }
 private void loadSource(Library.Source source) {
  if(loading||updatingAll)return;cancelCatalogRestore();minimizeVideo();pcloudMode=false;pcloudPanel.setVisibility(View.GONE);localMode=false;localPanel.setVisibility(View.GONE);localBack.setEnabled(false);favoritesPage=false;radioMode=false;radioPanel.setVisibility(View.GONE);favoritesMode=false;
  List<Library.Source> requested=new ArrayList<>(library.sources);int replace=-1;for(int i=0;i<requested.size();i++)if(requested.get(i).id.equals(source.id)||CombinedCatalog.sameConnection(requested.get(i),source)){replace=i;break;}if(replace<0)requested.add(source);else requested.set(replace,source);
  rebuildStreamingCatalog();busy(true);rebuildGroups();status.setVisibility(View.VISIBLE);status.setText(R.string.loading_catalog);
  worker.execute(()->{
   Map<Library.Source,Catalog> loaded=new LinkedHashMap<>();List<String> failures=new ArrayList<>();Exception firstError=null;List<java.util.concurrent.Future<Catalog>> tasks=new ArrayList<>();
   for(Library.Source item:requested)tasks.add(streamingWorker.submit(()->{Catalog fetched=item.xtream?new XtreamClient(item.server,item.username,item.password).load():new Catalog(new PlaylistLoader().load(item.server));new CatalogStore(this).save(item,fetched);return fetched;}));
   for(int i=0;i<requested.size();i++)try{loaded.put(requested.get(i),tasks.get(i).get());}catch(Exception failure){failures.add(requested.get(i).name);Throwable cause=failure instanceof java.util.concurrent.ExecutionException?failure.getCause():failure;if(firstError==null)firstError=cause instanceof Exception?(Exception)cause:new java.io.IOException("Source failed");if(failure instanceof InterruptedException){Thread.currentThread().interrupt();break;}}
   Exception error=firstError;runOnUiThread(()->{
    if(isDestroyed()||applyingSettings)return;
    if(!loaded.isEmpty())changeLibrary(()->{for(Map.Entry<Library.Source,Catalog> entry:loaded.entrySet()){Library.Source saved=library.addSource(entry.getKey());sourceCatalogs.put(saved.id,entry.getValue());cachedSourceConfigs.put(saved.id,saved);if(!saved.id.equals(entry.getKey().id)){Catalog snapshot=entry.getValue();worker.execute(()->new CatalogStore(this).save(saved,snapshot));}library.refreshFavorites(saved.id,entry.getValue().items);}});
    rebuildStreamingCatalog();busy(false);if(!favoritesMode&&catalog.filter(currentKind,null,"").isEmpty()&&!catalog.items.isEmpty())chooseFirstKind();rebuildGroups();loadAssignedGuides(false);
    if(error!=null){if(catalog.items.isEmpty()&&!radioMode&&!localMode&&!pcloudMode&&!favoritesPage)showError(error);else UiText.toast(this,getString(R.string.update_failed_names,android.text.TextUtils.join(", ",failures)),Toast.LENGTH_LONG).show();}
   });
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
  status.setText(UiText.text(message));
 }
 private void open(Library.Entry entry) {
  if(recordingBlocked())return;
  tvAnchor=null;tvBackupIndex=0;tvActiveUrl=null;
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
     LinearLayout choices=column();EditText query=new ClearableSearchEditText(this);query.setSingleLine();query.setHint(R.string.search_hint);choices.addView(query);
     ListView episodeList=new ListView(this);MediaListStyle.apply(episodeList);ArrayAdapter<Playlist.Channel> episodeAdapter=new UiArrayAdapter<Playlist.Channel>(this,android.R.layout.simple_list_item_1,new ArrayList<>(episodes)){@Override public View getView(int position,View recycled,ViewGroup parent){View row=super.getView(position,null,parent);MediaListStyle.row(row);Playlist.Channel episode=getItem(position);LinearLayout content=new LinearLayout(MainActivity.this);content.setGravity(Gravity.CENTER_VERTICAL);ImageView episodeIcon=new ImageView(MainActivity.this);episodeIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);content.addView(episodeIcon,new LinearLayout.LayoutParams(dp(48),dp(48)));Glide.with(MainActivity.this).load(episode==null?null:episode.logoUrl).placeholder(R.drawable.ic_video).error(R.drawable.ic_video).override(dp(96),dp(96)).fitCenter().into(episodeIcon);content.addView(row,new LinearLayout.LayoutParams(0,-1,1));boolean playing=episode!=null&&isChannelPlaying(episode);((TextView)row).setTypeface(null,playing?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);NowPlayingIndicator.show(NowPlayingIndicator.addTo(content),playing);MediaListStyle.row(content);return ProgressRows.wrap(content,episode==null?"":episode.url,true);}};episodeList.setAdapter(episodeAdapter);choices.addView(episodeList,new LinearLayout.LayoutParams(-1,dp(360)));
     AlertDialog dialog=new AlertDialog.Builder(this).setTitle(series.name).setView(choices).setNegativeButton(android.R.string.cancel,null).create();
     query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence value,int start,int count,int after){}public void onTextChanged(CharSequence value,int start,int before,int count){episodeAdapter.getFilter().filter(value);}public void afterTextChanged(Editable value){}});
     episodeList.setOnItemLongClickListener((parent,row,position,id)->{Playlist.Channel episode=episodeAdapter.getItem(position);if(episode==null)return false;MediaDownloads.show(row,MainActivity.this,FavoriteLists.of(new Library.Entry(source.id,episode)));return true;});
     episodeList.setOnItemClickListener((parent,view,position,id)->{Playlist.Channel episode=episodeAdapter.getItem(position);if(episode!=null){dialog.dismiss();play(episode);}});dialog.show();
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
    LinearLayout labels=column();labels.setPadding(MediaListStyle.iconGap(labels),0,dp(4),0);row.name=text("",16,Color.WHITE);row.name.setMaxLines(2);labels.addView(row.name);row.group=text("",12,Color.LTGRAY);row.group.setMaxLines(1);labels.addView(row.group);layout.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
    row.indicator=NowPlayingIndicator.addTo(layout);layout.setTag(row);recycled=ProgressRows.wrap(layout,"",false);
   } else row=(Row)recycled.getTag();
   Library.Entry entry=getItem(position);Playlist.Channel channel=entry.channel;row.name.setText(channel.name);
   Library.Source source=library.findSource(entry.sourceId);
   if(favoritesMode) {
    String programme=programmeTitle(entry,System.currentTimeMillis());row.group.setText(programme);row.group.setMaxLines(Integer.MAX_VALUE);row.group.setEllipsize(null);row.group.setTextColor(Color.rgb(170,170,170));row.group.setVisibility(programme.isEmpty()?View.GONE:View.VISIBLE);
   } else {row.group.setMaxLines(1);row.group.setText(channel.group);row.group.setVisibility(View.VISIBLE);}
   row.logo.setContentDescription(getString(R.string.logo_for,channel.name));
   String bundled=localLogos.find(channel.name);
   Object logoModel=bundled==null?LogoRequests.model(channel.logoUrl,source==null?null:source.server):bundled;
   Glide.with(MainActivity.this).load(logoModel).signature(com.bumptech.glide.signature.ApplicationVersionSignature.obtain(MainActivity.this)).placeholder(R.drawable.ic_video).error(R.drawable.ic_video).override(dp(96),dp(96)).fitCenter().into(row.logo);
   recycled.setAlpha(list.isPicked(position)?0.6f:1f);
   boolean playing=isChannelPlaying(channel);NowPlayingIndicator.show(row.indicator,playing);row.name.setTypeface(null,playing?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);row.group.setTypeface(null,playing?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);

   ProgressRows.bind(recycled,channel.url,channel.kind!=Playlist.Kind.TV&&channel.seriesId==null);return recycled;
  }
 }
 private boolean isFilePlaying(LocalMedia file){return LocalAudioService.playing(file.uri)||RadioPlaybackService.playingFile(file.uri)||CastPlayback.playingLocal(this,file.uri)||localPlayerAttached&&selected!=null&&selected.url.equals(file.uri)&&player!=null&&player.isPlaying();}
 private boolean isChannelPlaying(Playlist.Channel channel) {
  if(selected==null || !(tvAnchor!=null?tvAnchor.channel.url:selected.url).equals(channel.url)) return false;
  CastSession session=connectedCast();return session!=null && session.getRemoteMediaClient()!=null?(session.getRemoteMediaClient().isPlaying()||session.getRemoteMediaClient().isBuffering()):player!=null && player.isPlaying();
 }
 private static final class Row {ImageView logo;TextView name,group;View indicator;}
 boolean externalPlaybackAvailable(){return selected!=null&&(player==null||player.getPlaybackState()!=Player.STATE_ENDED)||ExternalPlaybackActivity.available(this);}
 void refreshExternalOutputSpacing(){updateHeaderOutputs();}
 void openExternalPlayback(){if(recordingBlocked())return;if(selected!=null&&!localPlayerAttached)ExternalPlaybackActivity.openVideo(this,selected,player);else ExternalPlaybackActivity.open(this);}
 private boolean recordingTouchBlocked;
 @Override public boolean dispatchTouchEvent(android.view.MotionEvent event){
  if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN){recordingTouchBlocked=false;if(TvRecordingService.active()&&playbackPanel!=null){int[] location=new int[2];playbackPanel.getLocationOnScreen(location);android.graphics.Rect bounds=new android.graphics.Rect(location[0],location[1],location[0]+playbackPanel.getWidth(),location[1]+playbackPanel.getHeight());if(!bounds.contains((int)event.getRawX(),(int)event.getRawY())){recordingBlocked();recordingTouchBlocked=true;}}}
  if(recordingTouchBlocked){if(event.getActionMasked()==android.view.MotionEvent.ACTION_UP||event.getActionMasked()==android.view.MotionEvent.ACTION_CANCEL)recordingTouchBlocked=false;return true;}return super.dispatchTouchEvent(event);
 }
 private boolean recordingBlocked(){if(!TvRecordingService.active())return false;UiText.toast(this,"Stop optagelsen, før du skifter afspilning eller forlader siden.",Toast.LENGTH_SHORT).show();return true;}
 private void confirmTvRecording(){
  if(TvRecordingService.active()){TvRecordingService.stop(this);playbackPanel.refresh();return;}
  if(selected==null||selected.kind!=Playlist.Kind.TV)return;
  Playlist.Channel channel=selected;
  new AlertDialog.Builder(this).setTitle(UiText.text("Start TV-optagelse?")).setMessage(UiText.text("Vil du optage "+channel.name+"?\n\nOptagelsen gemmes i Download. Du kan ikke skifte kanal eller lukke appen med tilbage-knappen, før optagelsen er stoppet.")).setNegativeButton(UiText.text("Annuller"),null).setPositiveButton(UiText.text("Start optagelse"),(dialog,which)->{
   if(selected==null||!selected.url.equals(channel.url)||TvRecordingService.active())return;
   if(android.os.Build.VERSION.SDK_INT<=28&&checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},7320);return;}
   if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},7320);return;}
   try{TvRecordingService.start(this,channel);playbackPanel.refresh();}catch(RuntimeException error){UiText.toast(this,"Optagelsen kunne ikke startes.",Toast.LENGTH_LONG).show();}
  }).show();
 }
 private void play(Playlist.Channel channel) {
  if(recordingBlocked())return;PlaybackKeyboard.hide(this);
  if(tvAnchor!=null&&(channel.kind!=Playlist.Kind.TV||!channel.url.equals(tvActiveUrl))){tvAnchor=null;tvBackupIndex=0;tvActiveUrl=null;}
  final int castRequest=++castLoadGeneration;
  videoMinimized=false;
  acceptLocalPlayback=false;detachLocalPlayback();
  requestNotificationPermission();
  RadioPlaybackService.stopForSourceSwitch(this);LocalAudioService.stopForSourceSwitch(this);
  status.setVisibility(View.VISIBLE);
  boolean same=selected!=null && selected.url.equals(channel.url);
  if(!same) resumePosition=0;else rememberPlayback();
  if(channel.url.startsWith("content:")){playLocal(new LocalMedia(channel.url,channel.name,"video/*",false));return;}
  selected=channel;PlaybackMark previous=PlaybackHistory.get(this,channel.url);long legacyPosition=library.lastPlayed!=null&&library.lastPlayed.url.equals(channel.url)?library.lastPlayedPosition:resumePosition;long savedPosition=channel.kind==Playlist.Kind.TV?0:previous.duration>0?previous.resume():legacyPosition;changeLibrary(()->{library.lastPlayed=tvAnchor!=null?tvAnchor.channel:channel;library.lastPlayedPosition=savedPosition;});CastSession session=connectedCast();playbackPanel.setTitle(channel.name);playbackPanel.setVisibility(View.VISIBLE);playbackPanel.hideControls();status.setVisibility(View.GONE);hideFullscreenSystemBars();
  if(session!=null && session.isConnected() && session.getRemoteMediaClient()!=null) {
   exitFullscreen();video.setVisibility(View.VISIBLE);if(player!=null){player.stop();player.clearMediaItems();}video.setPlayer(null);watchRemote();playbackPanel.refresh();
   CastHistory.beforeLoad();final int selectedRequest=CastPlayback.beginSelection();String sessionId=session.getSessionId();
   Runnable resolveCast=()->{CastStream stream=CastStream.resolve(channel.url,channel.kind==Playlist.Kind.TV);runOnUiThread(()->{
    CastSession current=connectedCast();if(!CastPlayback.selectionCurrent(selectedRequest)||isDestroyed()||castRequest!=castLoadGeneration||current==null||!java.util.Objects.equals(sessionId,current.getSessionId())||current.getRemoteMediaClient()==null)return;
    MediaMetadata metadata=new MediaMetadata(stream.type.startsWith("audio/")?MediaMetadata.MEDIA_TYPE_MUSIC_TRACK:MediaMetadata.MEDIA_TYPE_MOVIE);metadata.putString(MediaMetadata.KEY_TITLE,channel.name);if(stream.type.startsWith("audio/")){metadata.putString(MediaMetadata.KEY_ARTIST,UiText.text(PlaybackText.SOURCE));metadata.putString(MediaMetadata.KEY_SUBTITLE,UiText.text(PlaybackText.SOURCE));}
    if(okhttp3.HttpUrl.parse(channel.logoUrl)!=null)metadata.addImage(new com.google.android.gms.common.images.WebImage(android.net.Uri.parse(channel.logoUrl)));
    CastPlayback.streamLoaded(channel,stream.url);MediaInfo media=new MediaInfo.Builder(stream.url).setContentType(stream.type).setStreamType(channel.kind==Playlist.Kind.TV?MediaInfo.STREAM_TYPE_LIVE:MediaInfo.STREAM_TYPE_BUFFERED).setMetadata(metadata).build();
    MediaLoadRequestData.Builder request=new MediaLoadRequestData.Builder().setMediaInfo(media).setAutoplay(true);if(channel.kind!=Playlist.Kind.TV)request.setCurrentTime(savedPosition);
    current.getRemoteMediaClient().load(request.build()).setResultCallback(result->{if(!CastPlayback.selectionCurrent(selectedRequest)||castRequest!=castLoadGeneration||isDestroyed())return;if(result.getStatus().isSuccess())CastPlayback.releaseLocalServer(this);else{status.setVisibility(View.VISIBLE);status.setText(R.string.cast_stream_failed);}});
   });};
   if(session.getRemoteMediaClient().getMediaInfo()!=null)session.getRemoteMediaClient().stop().setResultCallback(result->{if(!isDestroyed()&&castRequest==castLoadGeneration)castStreamWorker.execute(resolveCast);});else castStreamWorker.execute(resolveCast);
  } else {
   video.setVisibility(View.VISIBLE);
   if(player==null) {
    player=new ExoPlayer.Builder(this).setSeekBackIncrementMs(10000).setSeekForwardIncrementMs(10000).build();PlaybackTracker.attach(this,player);video.setPlayer(player);BluetoothOutputButton.register(this,player);player.setAudioAttributes(new androidx.media3.common.AudioAttributes.Builder().setUsage(androidx.media3.common.C.USAGE_MEDIA).setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE).build(),true);
    videoSession=new androidx.media3.session.MediaSession.Builder(this,player).setId("stream-video").setSessionActivity(android.app.PendingIntent.getActivity(this,10,new android.content.Intent(this,MainActivity.class).putExtra("openFullPlayer",true).addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP),android.app.PendingIntent.FLAG_UPDATE_CURRENT|android.app.PendingIntent.FLAG_IMMUTABLE)).build();
    player.addListener(new Player.Listener() {
     @Override public void onPlayerError(PlaybackException e) {UiText.toast(MainActivity.this,R.string.playback_failed,Toast.LENGTH_SHORT).show();}
     @Override public void onEvents(Player source,Player.Events events) {video.setKeepScreenOn(FullscreenScreenAwake.keep(fullscreen&&!pipLayout,library.keepScreenOnFullscreen,!videoMinimized&&source.isPlaying()));playbackPanel.refresh();updateVideoNotification();updateMiniBack();if(adapter!=null) adapter.notifyDataSetChanged();}
    });
   }
   video.setPlayer(player);
   androidx.media3.common.MediaMetadata metadata=PlaybackText.metadata(channel.name).setArtworkData(PlaybackArtwork.local(this,channel.name),androidx.media3.common.MediaMetadata.PICTURE_TYPE_FRONT_COVER).build();
   android.os.Bundle historyExtras=new android.os.Bundle();historyExtras.putBoolean("history_live",channel.kind==Playlist.Kind.TV);metadata=metadata.buildUpon().setExtras(historyExtras).build();
   MediaItem item=new MediaItem.Builder().setMediaId(channel.url).setUri(channel.url).setMediaMetadata(metadata).build();
   if(channel.kind!=Playlist.Kind.TV && savedPosition>0) player.setMediaItem(item,savedPosition);
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
 private void goToLive(){
  playbackPanel.resetLive();
  CastSession session=connectedCast();if(session!=null){LivePlayback.goLive(session.getRemoteMediaClient());return;}
  if(selected!=null&&selected.kind==Playlist.Kind.TV&&!selected.url.startsWith("content:")){resumePosition=0;play(selected);playbackPanel.resetLive();}
  else if(player!=null&&player.isCurrentMediaItemLive()){player.seekToDefaultPosition();player.play();}
 }
 private void togglePlaybackState() {
  if(recordingBlocked())return;
  CastSession output=connectedCast();boolean liveTv=selected!=null&&selected.kind==Playlist.Kind.TV&&!selected.url.startsWith("content:");if(output!=null&&output.getRemoteMediaClient()!=null&&output.getRemoteMediaClient().getMediaInfo()!=null)liveTv=liveTv&&output.getRemoteMediaClient().isLiveStream();
  if(liveTv){
   CastSession castSession=connectedCast();if(castSession!=null&&castSession.getRemoteMediaClient()!=null){var remote=castSession.getRemoteMediaClient();if(remote.isPlaying()||remote.isBuffering())stopPlaybackState();else play(selected);}
   else if(player!=null&&player.getPlayWhenReady()&&player.getPlaybackState()!=Player.STATE_IDLE&&player.getPlaybackState()!=Player.STATE_ENDED){stopPlaybackState();}
   else play(selected);
   playbackPanel.refresh();updateMiniBack();if(adapter!=null)adapter.notifyDataSetChanged();if(favoriteHub!=null)favoriteHub.programmeChanged();return;
  }
  if(localPlayerAttached) {startService(LocalAudioService.command(this,LocalAudioService.TOGGLE));return;}
  CastSession session=connectedCast();
  if(session!=null && session.getRemoteMediaClient()!=null) {
   var remote=session.getRemoteMediaClient();if(remote.isPlaying()) remote.pause();else if(remote.isLiveStream())LivePlayback.goLive(remote);else remote.play();
  } else if(player!=null) {if(player.getPlayWhenReady()) player.pause();else if(playbackPanel.liveChannel())goToLive();else {if(player.getPlaybackState()==Player.STATE_IDLE) player.prepare();player.play();}}
  else if(selected!=null) play(selected);
  playbackPanel.refresh();updateMiniBack();
 }
 private void stopPlaybackState() {
  if(recordingBlocked())return;
  castLoadGeneration++;
  rememberPlayback();tvAnchor=null;tvActiveUrl=null;tvBackupIndex=0;videoArtworkGeneration++;resumeLocalPlayback=false;
  CastSession session=connectedCast();if(session!=null && session.getRemoteMediaClient()!=null)CastPlayback.stop(this);
  if(localPlayerAttached) {acceptLocalPlayback=false;startService(LocalAudioService.command(this,LocalAudioService.STOP));detachLocalPlayback();}else if(player!=null){player.pause();player.stop();player.clearMediaItems();video.setPlayer(null);}exitFullscreen();playbackPanel.setVisibility(View.GONE);video.setKeepScreenOn(false);resumePosition=0;selected=null;updateMiniBack();hideFullscreenSystemBars();
  status.setText("");status.setVisibility(View.GONE);updateVideoNotification();if(adapter!=null)adapter.notifyDataSetChanged();if(favoriteHub!=null)favoriteHub.programmeChanged();
 }
 private void enterFullscreen() {
  if(fullscreen || connectedCast()!=null || selected==null || player==null) return;
  playbackPanel.setVisibility(View.VISIBLE);video.setKeepScreenOn(library.keepScreenOnFullscreen);FullscreenScreenAwake.apply(getWindow(),library.keepScreenOnFullscreen);
  collapseSettings();drawer.closeDrawer(androidx.core.view.GravityCompat.START);fullscreen=true;previousOrientation=getRequestedOrientation();
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
  fullscreen=false;FullscreenScreenAwake.apply(getWindow(),false);fullscreenHost.removeView(playbackPanel);content.addView(playbackPanel,panelIndex,new LinearLayout.LayoutParams(-1,-2));fullscreenHost.setVisibility(View.GONE);drawer.setVisibility(View.VISIBLE);
  playbackPanel.fullscreenLayout(false);playbackPanel.setVisibility(videoMinimized?View.GONE:View.VISIBLE);video.setKeepScreenOn(!videoMinimized&&player!=null&&player.isPlaying());fullscreenBack.setEnabled(false);getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
  if(android.os.Build.VERSION.SDK_INT>=28) {android.view.WindowManager.LayoutParams params=getWindow().getAttributes();params.layoutInDisplayCutoutMode=previousCutoutMode;getWindow().setAttributes(params);}
  androidx.core.view.WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).show(androidx.core.view.WindowInsetsCompat.Type.systemBars());setRequestedOrientation(previousOrientation);hideFullscreenSystemBars();
 }
 private void hideFullscreenSystemBars() {
  if(!fullscreen) {
   getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
   androidx.core.view.WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).show(androidx.core.view.WindowInsetsCompat.Type.systemBars());
   if(content!=null)androidx.core.view.ViewCompat.requestApplyInsets(content);
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
 private long imageFavoriteVersion=ImageSession.favoriteVersion;
 @Override protected void onStart() {super.onStart();if(musicFavoriteVersion!=SaavnMusic.favoriteVersion){try{library=store.load();musicFavoriteVersion=SaavnMusic.favoriteVersion;if(favoriteHub!=null)favoriteHub.refresh();if(localPanel!=null)localPanel.playbackChanged();if(pcloudPanel!=null)pcloudPanel.playbackChanged();if(adapter!=null)adapter.notifyDataSetChanged();}catch(Exception ignored){}}if(getIntent().getBooleanExtra("favoritesHome",false)){getIntent().removeExtra("favoritesHome");showFavoritesHome();}if(imageFavoriteVersion!=ImageSession.favoriteVersion){try{Library fresh=store.load();library.imageFavorites.clear();library.imageFavorites.addAll(fresh.imageFavorites);library.favoriteAssignments.clear();library.favoriteAssignments.putAll(fresh.favoriteAssignments);library.favoriteKinds.clear();library.favoriteKinds.putAll(fresh.favoriteKinds);library.favoriteOrder.clear();library.favoriteOrder.addAll(fresh.favoriteOrder);imageFavoriteVersion=ImageSession.favoriteVersion;if(favoritesPage&&localMode)localPanel.reload();else if(localPanel!=null)localPanel.playbackChanged();if(pcloudPanel!=null)pcloudPanel.playbackChanged();if(favoritesPage)filter();}catch(Exception ignored){}}activityStarted=true;FullscreenScreenAwake.apply(getWindow(),fullscreen&&!pipLayout&&library.keepScreenOnFullscreen);syncLocalPlayback();}
 @Override protected void onResume() {super.onResume();if(LocalAudioService.current()!=null) syncLocalPlayback();backgroundPlayback=false;updateVideoNotification();if(resumeLocalPlayback && player==null && selected!=null && connectedCast()==null) {resumeLocalPlayback=false;play(selected);}if(playbackPanel!=null) playbackPanel.refresh();hideFullscreenSystemBars();if(list!=null) {list.removeCallbacks(epgTick);list.postDelayed(epgTick,60000);loadAssignedGuides(false);}}
 @Override public boolean onKeyUp(int keyCode,android.view.KeyEvent event) {
  boolean handled=super.onKeyUp(keyCode,event);
  if(keyCode==android.view.KeyEvent.KEYCODE_VOLUME_UP || keyCode==android.view.KeyEvent.KEYCODE_VOLUME_DOWN || keyCode==android.view.KeyEvent.KEYCODE_VOLUME_MUTE) getWindow().getDecorView().post(()->{if(playbackPanel!=null) playbackPanel.refresh();});
  return handled;
 }
 @Override protected void onPause() {rememberPlayback();backgroundPlayback=true;updateVideoNotification();super.onPause();}
 @Override protected void onStop() {FullscreenScreenAwake.apply(getWindow(),false);rememberPlayback();if(list!=null) list.removeCallbacks(epgTick);super.onStop();activityStarted=false;if(localPlayerAttached) {detachLocalPlayback();resumeLocalPlayback=false;}else if(player!=null&&!TvRecordingService.active()){resumeLocalPlayback=player.getPlayWhenReady();resumePosition=player.getCurrentPosition();if(videoSession!=null){videoSession.release();videoSession=null;}PlaybackTracker.detach(player);BluetoothOutputButton.unregister(player);player.release();player=null;video.setPlayer(null);video.setKeepScreenOn(false);}if(playbackPanel!=null) playbackPanel.refresh();updateMiniBack();if(adapter!=null) adapter.notifyDataSetChanged();updateVideoNotification();}
 @Override protected void onDestroy() {if(isFinishing()&&!isChangingConfigurations())PlaybackShutdown.closeUi(this);if(watchedRemote!=null) watchedRemote.unregisterCallback(remoteUpdates);if(cast!=null) cast.getSessionManager().removeSessionManagerListener(sessions,CastSession.class);castLoadGeneration++;castStreamWorker.shutdownNow();videoArtworkGeneration++;videoArtworkWorker.shutdownNow();settingsWorker.shutdownNow();unregisterReceiver(videoTransport);if(videoSession!=null){videoSession.release();videoSession=null;}((android.app.NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(2002);worker.shutdownNow();catalogReader.shutdownNow();streamingWorker.shutdownNow();localWorker.shutdownNow();if(favoriteHub!=null)favoriteHub.close();if(localPanel!=null) localPanel.close();if(pcloudPanel!=null)pcloudPanel.close();if(pcloudLoginClient!=null)pcloudLoginClient.cancel();epgWorker.shutdownNow();if(radioPanel!=null) radioPanel.close();super.onDestroy();}
}
