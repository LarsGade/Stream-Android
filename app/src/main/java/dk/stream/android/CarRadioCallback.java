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
 private final CarRadioCatalog catalog;
 private final ListeningExecutorService worker=MoreExecutors.listeningDecorator(java.util.concurrent.Executors.newSingleThreadExecutor());
 public CarRadioCallback(CarRadioCatalog catalog) {this.catalog=catalog;}
 public void close() {worker.shutdownNow();}
 @Override public MediaSession.ConnectionResult onConnect(MediaSession session,MediaSession.ControllerInfo controller) {
  return MediaSession.ConnectionResult.accept(MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS,MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS);
 }
 @Override public ListenableFuture<LibraryResult<MediaItem>> onGetLibraryRoot(MediaLibrarySession session,MediaSession.ControllerInfo controller,LibraryParams params) {
  Bundle extras=new Bundle();extras.putBoolean("android.media.browse.SEARCH_SUPPORTED",true);extras.putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED",true);extras.putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT",1);extras.putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT",1);
  return Futures.immediateFuture(LibraryResult.ofItem(CarRadioCatalog.folder(CarRadioCatalog.ROOT,"Stream"),new LibraryParams.Builder().setExtras(extras).build()));
 }
 @Override public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetChildren(MediaLibrarySession session,MediaSession.ControllerInfo controller,String parent,int page,int pageSize,LibraryParams params) {
  return worker.submit(()->{try {return LibraryResult.ofItemList(catalog.children(parent,page,pageSize),params);} catch(IllegalArgumentException e) {return LibraryResult.ofError(SessionError.ERROR_BAD_VALUE);} catch(Exception e) {return LibraryResult.ofError(SessionError.ERROR_IO);}});
 }
 @Override public ListenableFuture<LibraryResult<MediaItem>> onGetItem(MediaLibrarySession session,MediaSession.ControllerInfo controller,String id) {
  return worker.submit(()->{try {if(CarRadioCatalog.ROOT.equals(id)) return LibraryResult.ofItem(CarRadioCatalog.folder(id,"Stream"),null);if(CarRadioCatalog.FAVORITES.equals(id)) return LibraryResult.ofItem(CarRadioCatalog.folder(id,"Favoritter"),null);if(CarRadioCatalog.DANISH.equals(id)) return LibraryResult.ofItem(CarRadioCatalog.folder(id,"Danske radiostationer"),null);return LibraryResult.ofItem(catalog.item(catalog.resolve(id),false),null);} catch(Exception e) {return LibraryResult.ofError(SessionError.ERROR_BAD_VALUE);}});
 }
 @Override public ListenableFuture<LibraryResult<Void>> onSubscribe(MediaLibrarySession session,MediaSession.ControllerInfo controller,String parent,LibraryParams params) {
  if(!Arrays.asList(CarRadioCatalog.ROOT,CarRadioCatalog.FAVORITES,CarRadioCatalog.DANISH).contains(parent)) return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE));
  return Futures.immediateFuture(LibraryResult.ofVoid(params));
 }
 @Override public ListenableFuture<LibraryResult<Void>> onSearch(MediaLibrarySession session,MediaSession.ControllerInfo controller,String query,LibraryParams params) {
  return worker.submit(()->{try {int count=catalog.search(query).size();new android.os.Handler(android.os.Looper.getMainLooper()).post(()->session.notifySearchResultChanged(controller,query,count,params));return LibraryResult.ofVoid(params);} catch(Exception e) {return LibraryResult.ofError(SessionError.ERROR_IO);}});
 }
 @Override public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetSearchResult(MediaLibrarySession session,MediaSession.ControllerInfo controller,String query,int page,int pageSize,LibraryParams params) {
  return worker.submit(()->{try {List<MediaItem> items=catalog.search(query);long start=(long)page*pageSize;if(page<0||pageSize<1) return LibraryResult.ofError(SessionError.ERROR_BAD_VALUE);return LibraryResult.ofItemList(start>=items.size()?Collections.emptyList():items.subList((int)start,(int)Math.min(items.size(),start+pageSize)),params);} catch(Exception e) {return LibraryResult.ofError(SessionError.ERROR_IO);}});
 }
 @Override public ListenableFuture<List<MediaItem>> onAddMediaItems(MediaSession session,MediaSession.ControllerInfo controller,List<MediaItem> items) {
  return worker.submit(()->{List<MediaItem> resolved=new ArrayList<>();for(MediaItem item:items) {
   String id=item.mediaId;
   if(item.requestMetadata.searchQuery!=null) {String query=item.requestMetadata.searchQuery;String exact=null;for(RadioStation station:catalog.favorites()) if(LocalChannelLogos.key(station.displayName()).equals(LocalChannelLogos.key(query))) {exact=CarRadioCatalog.PREFIX+station.id;break;}
    List<MediaItem> results=exact==null?catalog.search(query):java.util.Collections.singletonList(new MediaItem.Builder().setMediaId(exact).build());if(results.isEmpty()) throw new IllegalArgumentException("No matching station");id=results.get(0).mediaId;}
   resolved.add(catalog.item(catalog.resolve(id),true));
  }return resolved;});
 }
}
