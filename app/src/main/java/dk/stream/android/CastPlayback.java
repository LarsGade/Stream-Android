package dk.stream.android;

import android.content.*;
import com.google.android.gms.cast.*;
import com.google.android.gms.cast.framework.*;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import java.util.*;

/** Common Cast transport and receiver metadata for radio, streams and local documents. */
@androidx.media3.common.util.UnstableApi
public final class CastPlayback {
 private static Context application;
 private static RadioStation artworkStation;private static String artworkFile;private static byte[] artworkData;
 private static CastContext cast;
 private static RemoteMediaClient watched;
 private static final RemoteMediaClient.Callback changes=new RemoteMediaClient.Callback(){@Override public void onStatusUpdated(){CastHistory.capture(true);if(watched!=null&&watched.getMediaStatus()!=null&&watched.getPlayerState()==MediaStatus.PLAYER_STATE_IDLE&&((!isImageInfo(watched.getMediaInfo())&&watched.getMediaStatus().getIdleReason()==MediaStatus.IDLE_REASON_FINISHED)||watched.getMediaStatus().getIdleReason()==MediaStatus.IDLE_REASON_ERROR))application.stopService(new Intent(application,LocalCastService.class));}};
 private static final Map<String,String> localUrls=new HashMap<>();
 public static void init(Context context) {
  if(application!=null)return;application=context.getApplicationContext();
  try {cast=CastContext.getSharedInstance(application);cast.getSessionManager().addSessionManagerListener(new SessionManagerListener<CastSession>() {
   public void onSessionStarted(CastSession session,String id){castCurrent();}
   public void onSessionResumed(CastSession session,boolean suspended){}
   public void onSessionEnded(CastSession session,int error){localUrls.clear();artworkStation=null;artworkFile=null;artworkData=null;application.stopService(new Intent(application,LocalCastService.class));}
   public void onSessionStarting(CastSession session){}public void onSessionStartFailed(CastSession session,int error){}public void onSessionEnding(CastSession session){}public void onSessionResuming(CastSession session,String id){}public void onSessionResumeFailed(CastSession session,int error){}public void onSessionSuspended(CastSession session,int reason){}
  },CastSession.class);}catch(RuntimeException unavailable){cast=null;}
 }
 public static CastSession session(Context context){init(context);CastSession session=cast==null?null:cast.getSessionManager().getCurrentCastSession();return session!=null&&session.isConnected()?session:null;}
 public static boolean connected(Context context){return session(context)!=null;}
 public static RemoteMediaClient remote(Context context){CastSession session=session(context);RemoteMediaClient remote=session==null?null:session.getRemoteMediaClient();if(watched!=remote){if(watched!=null)watched.unregisterCallback(changes);watched=remote;if(remote!=null)remote.registerCallback(changes);}if(remote!=null)CastHistory.start(context);return remote;}
 private static boolean isImageInfo(MediaInfo info){return info!=null&&(info.getContentType()!=null&&info.getContentType().startsWith("image/")||info.getMetadata()!=null&&info.getMetadata().getMediaType()==MediaMetadata.MEDIA_TYPE_PHOTO);}
 public static boolean image(Context context){RemoteMediaClient remote=remote(context);return remote!=null&&isImageInfo(remote.getMediaInfo())&&localUrls.containsValue(remote.getMediaInfo().getContentId());}
 public static String currentImageUri(Context context){if(!image(context))return null;String url=remote(context).getMediaInfo().getContentId();for(Map.Entry<String,String> entry:localUrls.entrySet())if(entry.getValue().equals(url))return entry.getKey();return null;}
 public static boolean hasMedia(Context context){RemoteMediaClient remote=remote(context);return remote!=null&&remote.getMediaInfo()!=null&&(remote.getPlayerState()!=MediaStatus.PLAYER_STATE_IDLE||image(context));}
 public static boolean audio(Context context){RemoteMediaClient remote=remote(context);MediaInfo info=remote==null?null:remote.getMediaInfo();return info!=null&&(info.getContentType()!=null&&info.getContentType().startsWith("audio/")||info.getMetadata()!=null&&info.getMetadata().getMediaType()==MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);}
 public static String title(Context context){RemoteMediaClient remote=remote(context);MediaInfo info=remote==null?null:remote.getMediaInfo();return info==null||info.getMetadata()==null?"Stream":Objects.toString(info.getMetadata().getString(MediaMetadata.KEY_TITLE),"Stream");}
 public static boolean currentLocal(Context context,String uri){RemoteMediaClient remote=remote(context);return remote!=null&&(remote.getPlayerState()!=MediaStatus.PLAYER_STATE_IDLE||image(context))&&remote.getMediaInfo()!=null&&Objects.equals(localUrls.get(uri),remote.getMediaInfo().getContentId());}
 public static boolean pcloud(Context context){for(String uri:localUrls.keySet())if(PcloudDocument.is(uri)&&currentLocal(context,uri))return true;return false;}
 public static boolean playingLocal(Context context,String uri){RemoteMediaClient remote=remote(context);return currentLocal(context,uri)&&remote!=null&&(remote.isPlaying()||remote.isBuffering());}
 /** Route physical volume keys to the receiver while a Cast session is connected. */
 public static boolean volumeKey(Context context,android.view.KeyEvent event){
  int key=event.getKeyCode();if(key!=android.view.KeyEvent.KEYCODE_VOLUME_UP&&key!=android.view.KeyEvent.KEYCODE_VOLUME_DOWN&&key!=android.view.KeyEvent.KEYCODE_VOLUME_MUTE)return false;
  CastSession session=session(context);if(session==null)return false;
  if(event.getAction()==android.view.KeyEvent.ACTION_DOWN)try{
   if(key==android.view.KeyEvent.KEYCODE_VOLUME_MUTE){if(event.getRepeatCount()==0)session.setMute(!session.isMute());}
   else {double volume=session.getVolume();if(Double.isNaN(volume))return true;session.setVolume(Math.max(0,Math.min(1,volume+(key==android.view.KeyEvent.KEYCODE_VOLUME_UP?0.05:-0.05))));if(key==android.view.KeyEvent.KEYCODE_VOLUME_UP&&session.isMute())session.setMute(false);}
  }catch(java.io.IOException|IllegalStateException error){android.util.Log.w("StreamCast","Could not change receiver volume",error);}
  return true;
 }
 public static void toggle(Context context){RemoteMediaClient remote=remote(context);if(remote!=null){if(remote.isPlaying()||remote.isBuffering())remote.pause();else if(remote.isLiveStream())LivePlayback.goLive(remote);else remote.play();}}
 public static void releaseLocalServer(Context context){localUrls.clear();context.stopService(new Intent(context,LocalCastService.class));}
 public static void closeWithApp(Context context){CastHistory.stop();if(cast!=null)cast.getSessionManager().endCurrentSession(true);artworkStation=null;artworkFile=null;artworkData=null;localUrls.clear();context.stopService(new Intent(context,LocalCastService.class));}
 public static void stop(Context context){CastHistory.beforeLoad();artworkStation=null;artworkFile=null;artworkData=null;RemoteMediaClient remote=remote(context);if(remote!=null)remote.stop();context.stopService(new Intent(context,LocalCastService.class));localUrls.clear();}
 public static boolean castCurrent(){if(ImageSession.viewing()){if(android.os.Build.VERSION.SDK_INT>=37&&application.checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK")!=android.content.pm.PackageManager.PERMISSION_GRANTED)return false;playLocal(application,ImageSession.current(),ImageSession.entries(),0);return true;}LocalMedia file=LocalAudioService.current();if(file!=null){if(android.os.Build.VERSION.SDK_INT>=37&&application.checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK")!=android.content.pm.PackageManager.PERMISSION_GRANTED)return false;playLocal(application,file,LocalAudioService.queue(),LocalAudioService.player().getCurrentPosition());return true;}RadioStation station=RadioPlaybackService.selectedStation();if(station!=null){playRadio(application,station);return true;}return false;}
 public static void playRadio(Context context,RadioStation station){CastHistory.beforeLoad();artworkStation=station;artworkFile=null;artworkData=null;if(remote(context)!=null)LocalCastService.castRadio(context,station);}
 static void loadRadio(Context context,RadioStation station,String logo){RemoteMediaClient remote=remote(context);if(remote==null)return;
  MediaMetadata metadata=new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);metadata.putString(MediaMetadata.KEY_TITLE,station.displayName());if(logo!=null)metadata.addImage(new com.google.android.gms.common.images.WebImage(android.net.Uri.parse(logo)));
  MediaInfo info=new MediaInfo.Builder(station.url).setContentType(station.hls?"application/x-mpegURL":"audio/mpeg").setStreamType(MediaInfo.STREAM_TYPE_LIVE).setMetadata(metadata).build();
  remote.load(new MediaLoadRequestData.Builder().setMediaInfo(info).setAutoplay(true).build()).setResultCallback(result->{if(result.getStatus().isSuccess()){localUrls.clear();context.stopService(new Intent(context,RadioPlaybackService.class));}else{failed(context);releaseLocalServer(context);}});
 }
 public static void playLocal(Context context,LocalMedia file,List<LocalMedia> entries,long position){if(!connected(context))return;artworkStation=null;artworkFile=file.uri;androidx.media3.exoplayer.ExoPlayer player=LocalAudioService.player();LocalMedia current=LocalAudioService.current();artworkData=current!=null&&current.uri.equals(file.uri)&&player!=null?player.getMediaMetadata().artworkData:null;LocalCastService.cast(context,new LocalQueue(entries,file),position);}
 static void loadImage(Context context,LocalMedia file,String url,String mime){CastHistory.beforeLoad();RemoteMediaClient remote=remote(context);if(remote==null)return;localUrls.clear();localUrls.put(file.uri,url);MediaMetadata metadata=new MediaMetadata(MediaMetadata.MEDIA_TYPE_PHOTO);metadata.putString(MediaMetadata.KEY_TITLE,file.displayName());if(metadata.getMediaType()==MediaMetadata.MEDIA_TYPE_MUSIC_TRACK){metadata.putString(MediaMetadata.KEY_ALBUM_TITLE,PlaybackText.shortTitle(file.displayName()));metadata.putString(MediaMetadata.KEY_ALBUM_ARTIST,PlaybackText.shortTitle(file.displayName()));}metadata.addImage(new com.google.android.gms.common.images.WebImage(android.net.Uri.parse(url)));
  MediaInfo info=new MediaInfo.Builder(url).setContentType(mime).setStreamType(MediaInfo.STREAM_TYPE_NONE).setMetadata(metadata).build();remote.load(new MediaLoadRequestData.Builder().setMediaInfo(info).setAutoplay(true).build()).setResultCallback(result->{if(result.getStatus().isSuccess()){context.stopService(new Intent(context,LocalAudioService.class));context.stopService(new Intent(context,RadioPlaybackService.class));}else{failed(context);releaseLocalServer(context);}});
 }
 static void loadLocal(Context context,LocalQueue queue,List<String> urls,long position){CastHistory.beforeLoad();RemoteMediaClient remote=remote(context);if(remote==null)return;localUrls.clear();MediaQueueItem[] items=new MediaQueueItem[queue.entries.size()];
  for(int i=0;i<items.length;i++){LocalMedia file=queue.entries.get(i);localUrls.put(file.uri,urls.get(i));MediaMetadata metadata=new MediaMetadata(file.audio()?MediaMetadata.MEDIA_TYPE_MUSIC_TRACK:MediaMetadata.MEDIA_TYPE_MOVIE);metadata.putString(MediaMetadata.KEY_TITLE,file.displayName());if(metadata.getMediaType()==MediaMetadata.MEDIA_TYPE_MUSIC_TRACK){metadata.putString(MediaMetadata.KEY_ALBUM_TITLE,PlaybackText.shortTitle(file.displayName()));metadata.putString(MediaMetadata.KEY_ALBUM_ARTIST,PlaybackText.shortTitle(file.displayName()));}
   MediaInfo info=new MediaInfo.Builder(urls.get(i)).setContentType(mime(file)).setStreamType(MediaInfo.STREAM_TYPE_BUFFERED).setMetadata(metadata).build();items[i]=new MediaQueueItem.Builder(info).setAutoplay(true).build();}
  remote.queueLoad(items,queue.start,MediaStatus.REPEAT_MODE_REPEAT_OFF,position,null).setResultCallback(result->{if(result.getStatus().isSuccess()){context.stopService(new Intent(context,LocalAudioService.class));context.stopService(new Intent(context,RadioPlaybackService.class));}else {failed(context);context.stopService(new Intent(context,LocalCastService.class));}});
 }
 public static void resetProgress(Context context){CastHistory.reset(context);}
 static String historyKey(Context context){RemoteMediaClient remote=remote(context);if(remote==null||remote.getMediaInfo()==null)return "";String id=remote.getMediaInfo().getContentId();for(Map.Entry<String,String> entry:localUrls.entrySet())if(java.util.Objects.equals(entry.getValue(),id))return entry.getKey();return id==null?"":id;}
 static String mime(LocalMedia file){int dot=file.name.lastIndexOf('.');String extension=dot<0?"":file.name.substring(dot+1);String inferred=android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.ROOT));return inferred!=null?inferred:!file.mime.isEmpty()&&!file.mime.equals("application/octet-stream")?file.mime:file.audio()?"audio/mpeg":"video/mp4";}
 static Object phoneArtwork(Context context){for(Map.Entry<String,String> entry:localUrls.entrySet())if(currentLocal(context,entry.getKey())){String cover=LocalFileArtwork.asset(new LocalMedia(entry.getKey(),title(context),"audio/mpeg",false));if(cover!=null)return cover;}if(artworkStation!=null&&title(context).equals(artworkStation.displayName()))return PlaybackArtwork.radioModel(context,artworkStation);return artworkFile!=null&&currentLocal(context,artworkFile)?artworkData:null;}
 static void failed(Context context){android.widget.Toast.makeText(context,"Cast-enheden kunne ikke afspille indholdet. Kontrollér netværk og filformat.",android.widget.Toast.LENGTH_LONG).show();}
}
