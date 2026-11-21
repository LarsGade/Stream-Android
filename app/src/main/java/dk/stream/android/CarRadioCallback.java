package dk.stream.android;

import android.os.Bundle;
import androidx.media3.common.*;
import androidx.media3.session.*;
import androidx.media3.session.MediaLibraryService.*;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.*;
import java.util.*;

/** All disk and Radio Browser work happens outside the media session's main thread. */
@androidx.media3.common.util.UnstableApi
public final class CarRadioCallback implements MediaLibrarySession.Callback {
 private boolean closed;
 private final Set<MediaSession.ControllerInfo> carConnections=new HashSet<>();
 private final CarRadioCatalog catalog;private final Runnable liveAction,previousAction,nextAction;
 private final ListeningExecutorService worker=MoreExecutors.listeningDecorator(java.util.concurrent.Executors.newSingleThreadExecutor());
 public CarRadioCallback(CarRadioCatalog catalog) {this(catalog,null);}
 public CarRadioCallback(CarRadioCatalog catalog,Runnable liveAction){this(catalog,liveAction,null,null);}
 public CarRadioCallback(CarRadioCatalog catalog,Runnable liveAction,Runnable previousAction,Runnable nextAction){this.catalog=catalog;this.liveAction=liveAction;this.previousAction=previousAction;this.nextAction=nextAction;}
 public static java.util.List<CommandButton> liveButtons(){return java.util.Collections.emptyList();}
 public static java.util.List<CommandButton> transportButtons(){return java.util.Collections.emptyList();}
 public void close() {closed=true;carConnections.clear();worker.shutdownNow();}
 public void startPageChanged(MediaLibrarySession session){if(session==null||worker.isShutdown())return;worker.submit(()->{try{int count=catalog.children(AndroidAutoPages.START,0,Integer.MAX_VALUE).size();new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{try{session.notifyChildrenChanged(AndroidAutoPages.START,count,null);}catch(IllegalStateException ignored){}});}catch(Exception ignored){}});}

 @Override public MediaSession.ConnectionResult onConnect(MediaSession session,MediaSession.ControllerInfo controller) {
  if(liveAction==null)return MediaSession.ConnectionResult.accept(MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS,MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS);
  SessionCommands commands=MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon().build();
  Player.Commands playerCommands=MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS;
  return new MediaSession.ConnectionResult.AcceptedResultBuilder(session).setAvailableSessionCommands(commands).setAvailablePlayerCommands(playerCommands).setCustomLayout(previousAction==null?liveButtons():transportButtons()).setMediaButtonPreferences(previousAction==null?liveButtons():transportButtons()).build();
 }
 private boolean carController(MediaSession session,MediaSession.ControllerInfo controller){return session.isAutomotiveController(controller)||session.isAutoCompanionController(controller)||"com.google.android.projection.gearhead".equals(controller.getPackageName());}
 private void alignLastSelection(MediaSession session,MediaSession.ControllerInfo controller){if(!carController(session,controller))return;LastAudioSelection selected=LastAudio.read(catalog.context());MediaItem current=session.getPlayer().getCurrentMediaItem();if(selected==null||current==null)return;LocalMedia file=CarAudioCatalog.file(current);String key=file!=null?"file:"+file.uri:current.mediaId.startsWith(CarRadioCatalog.PREFIX)?current.mediaId:"radio:"+current.mediaId;if(!selected.key().equals(key)){session.getPlayer().pause();session.getPlayer().clearMediaItems();}}
 @Override public void onPostConnect(MediaSession session,MediaSession.ControllerInfo controller){alignLastSelection(session,controller);if(!carController(session,controller))return;boolean first=carConnections.isEmpty();carConnections.add(controller);if(!first)return;PlaybackTracker.flushAll();CastHistory.capture(true);MediaItem initial=session.getPlayer().getCurrentMediaItem();ListenableFuture<MediaSession.MediaItemsWithStartPosition> resume=worker.submit(()->{Library settings=new LibraryStore(catalog.context()).load();return settings.androidAutoAutoPlay?lastSelection():null;});Futures.addCallback(resume,new FutureCallback<MediaSession.MediaItemsWithStartPosition>(){public void onSuccess(MediaSession.MediaItemsWithStartPosition value){if(closed||value==null||session.getPlayer().getCurrentMediaItem()!=initial||carConnections.isEmpty()||ExternalOutputState.pending()||CastPlayback.connected(catalog.context())||session.getPlayer().getPlayWhenReady()||LocalAudioService.player()!=null&&LocalAudioService.player().getPlayWhenReady())return;session.getPlayer().setMediaItems(value.mediaItems,value.startIndex,value.startPositionMs);session.getPlayer().prepare();session.getPlayer().play();}public void onFailure(Throwable failure){}},androidx.core.content.ContextCompat.getMainExecutor(catalog.context()));}
 @Override public void onDisconnected(MediaSession session,MediaSession.ControllerInfo controller){carConnections.remove(controller);}
 @Override public int onPlayerCommandRequest(MediaSession session,MediaSession.ControllerInfo controller,int command){if(command==Player.COMMAND_PLAY_PAUSE&&ExternalOutputState.pending())return SessionError.ERROR_INVALID_STATE;if(command==Player.COMMAND_PLAY_PAUSE)alignLastSelection(session,controller);return SessionResult.RESULT_SUCCESS;}
 private MediaSession.MediaItemsWithStartPosition lastSelection()throws Exception{if(ExternalOutputState.pending())throw new IllegalStateException("Ekstern overdragelse i gang.");LastAudioSelection selected=LastAudio.read(catalog.context());if(selected==null)throw new IllegalStateException("Ingen tidligere lydafspilning.");MediaItem item;long position=0;if(selected.file!=null){CarAudioCatalog.register(catalog.context(),selected.file);item=CarAudioCatalog.item(catalog.context(),selected.file,true);position=PlaybackHistory.get(catalog.context(),selected.file.uri).resume();}else item=catalog.resumeRadio(selected.radio);return new MediaSession.MediaItemsWithStartPosition(Collections.singletonList(item),0,position);}
 @Override public ListenableFuture<MediaSession.MediaItemsWithStartPosition> onPlaybackResumption(MediaSession session,MediaSession.ControllerInfo controller){if(ExternalOutputState.pending())return Futures.immediateFailedFuture(new IllegalStateException("Ekstern overdragelse i gang."));PlaybackTracker.flushAll();CastHistory.capture(true);return worker.submit(this::lastSelection);}
 @Override public ListenableFuture<SessionResult> onCustomCommand(MediaSession session,MediaSession.ControllerInfo controller,SessionCommand command,Bundle args){
  if(liveAction!=null&&RadioPlaybackService.LIVE.equals(command.customAction)){liveAction.run();return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));}
  if(previousAction!=null&&RadioPlaybackService.PREVIOUS.equals(command.customAction)){previousAction.run();return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));}
  if(nextAction!=null&&RadioPlaybackService.NEXT.equals(command.customAction)){nextAction.run();return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));}
  return Futures.immediateFuture(new SessionResult(SessionError.ERROR_NOT_SUPPORTED));
 }
 @Override public ListenableFuture<LibraryResult<MediaItem>> onGetLibraryRoot(MediaLibrarySession session,MediaSession.ControllerInfo controller,LibraryParams params) {
  Bundle extras=new Bundle();extras.putBoolean("android.media.browse.SEARCH_SUPPORTED",true);extras.putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED",true);extras.putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT",1);extras.putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT",1);
  return Futures.immediateFuture(LibraryResult.ofItem(CarRadioCatalog.folder(AndroidAutoPages.START,"Stream"),new LibraryParams.Builder().setExtras(extras).build()));
 }
 @Override public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetChildren(MediaLibrarySession session,MediaSession.ControllerInfo controller,String parent,int page,int pageSize,LibraryParams params) {
  return worker.submit(()->{try {return LibraryResult.ofItemList(catalog.children(parent,page,pageSize),params);} catch(IllegalArgumentException e) {return LibraryResult.ofError(SessionError.ERROR_BAD_VALUE);} catch(Exception e) {return LibraryResult.ofError(SessionError.ERROR_IO);}});
 }
 @Override public ListenableFuture<LibraryResult<MediaItem>> onGetItem(MediaLibrarySession session,MediaSession.ControllerInfo controller,String id) {
  return worker.submit(()->{try {if(id.startsWith(CarAudioCatalog.FILE)||id.startsWith(CarAudioCatalog.FOLDER))return LibraryResult.ofItem(CarAudioCatalog.item(catalog.context(),CarAudioCatalog.resolve(catalog.context(),id),false),null);if(AndroidAutoPages.folder(id))return LibraryResult.ofItem(CarRadioCatalog.folder(id,CarRadioCatalog.ROOT.equals(id)?"Alle sider":AndroidAutoPages.label(id)),null);return LibraryResult.ofItem(catalog.item(catalog.resolve(id),false),null);} catch(Exception e) {return LibraryResult.ofError(SessionError.ERROR_BAD_VALUE);}});
 }
 @Override public ListenableFuture<LibraryResult<Void>> onSubscribe(MediaLibrarySession session,MediaSession.ControllerInfo controller,String parent,LibraryParams params) {
  if(!AndroidAutoPages.folder(parent)) return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE));
  return Futures.immediateFuture(LibraryResult.ofVoid(params));
 }
 @Override public ListenableFuture<LibraryResult<Void>> onSearch(MediaLibrarySession session,MediaSession.ControllerInfo controller,String query,LibraryParams params) {
  return worker.submit(()->{try {int count=catalog.search(query).size();new android.os.Handler(android.os.Looper.getMainLooper()).post(()->session.notifySearchResultChanged(controller,query,count,params));return LibraryResult.ofVoid(params);} catch(Exception e) {return LibraryResult.ofError(SessionError.ERROR_IO);}});
 }
 @Override public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetSearchResult(MediaLibrarySession session,MediaSession.ControllerInfo controller,String query,int page,int pageSize,LibraryParams params) {
  return worker.submit(()->{try {List<MediaItem> items=catalog.search(query);long start=(long)page*pageSize;if(page<0||pageSize<1) return LibraryResult.ofError(SessionError.ERROR_BAD_VALUE);return LibraryResult.ofItemList(start>=items.size()?Collections.emptyList():items.subList((int)start,(int)Math.min(items.size(),start+pageSize)),params);} catch(Exception e) {return LibraryResult.ofError(SessionError.ERROR_IO);}});
 }
 @Override public ListenableFuture<MediaSession.MediaItemsWithStartPosition> onSetMediaItems(MediaSession session,MediaSession.ControllerInfo controller,List<MediaItem> items,int index,long position){return Futures.transform(onAddMediaItems(session,controller,items),resolved->{int start=index<0?0:index;if(resolved.size()==1&&CarAudioCatalog.file(resolved.get(0))!=null){String selected=resolved.get(0).mediaId;resolved=catalog.audioQueue(selected);start=0;for(int i=0;i<resolved.size();i++)if(resolved.get(i).mediaId.equals(selected)){start=i;break;}}long resume=position;if((resume==C.TIME_UNSET||resume==0)&&start<resolved.size()){LocalMedia file=CarAudioCatalog.file(resolved.get(start));if(file!=null)resume=PlaybackHistory.get(catalog.context(),file.uri).resume();}return new MediaSession.MediaItemsWithStartPosition(resolved,start,resume);},MoreExecutors.directExecutor());}
 @Override public ListenableFuture<List<MediaItem>> onAddMediaItems(MediaSession session,MediaSession.ControllerInfo controller,List<MediaItem> items) {
  return worker.submit(()->{List<MediaItem> resolved=new ArrayList<>();for(MediaItem item:items) {
   String id=item.mediaId;if(id.startsWith(CarAudioCatalog.FILE)){resolved.add(CarAudioCatalog.item(catalog.context(),CarAudioCatalog.resolve(catalog.context(),id),true));continue;}
   if(item.requestMetadata.searchQuery!=null) {String query=item.requestMetadata.searchQuery;String exact=null;for(RadioStation station:catalog.favorites()) if(LocalChannelLogos.key(station.displayName()).equals(LocalChannelLogos.key(query))) {exact=CarRadioCatalog.PREFIX+station.id;break;}
    List<MediaItem> results=exact==null?catalog.search(query):java.util.Collections.singletonList(new MediaItem.Builder().setMediaId(exact).build());if(results.isEmpty()) throw new IllegalArgumentException("No matching station");id=results.get(0).mediaId;}
   resolved.add(id.startsWith(CarAudioCatalog.FILE)?CarAudioCatalog.item(catalog.context(),CarAudioCatalog.resolve(catalog.context(),id),true):catalog.item(catalog.resolve(id),true));
  }return resolved;});
 }
}
