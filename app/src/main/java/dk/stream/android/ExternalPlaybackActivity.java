package dk.stream.android;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import androidx.media3.common.Player;
import java.util.*;

/** Holds the original playback source while Android's chooser hands it to another player. */
@androidx.media3.common.util.UnstableApi
public final class ExternalPlaybackActivity extends androidx.appcompat.app.AppCompatActivity {
 private final ExternalPlaybackReturn returning=new ExternalPlaybackReturn();private final Handler handler=new Handler(Looper.getMainLooper());
 private final Runnable returned=()->{if(returning.canResume(hasWindowFocus())&&!isFinishing())resumeStream();};
 static boolean available(Context context){return CastPlayback.hasMedia(context)&&!CastPlayback.image(context)||LocalAudioService.current()!=null&&!LocalAudioService.current().image()&&(LocalAudioService.player()==null||LocalAudioService.player().getPlaybackState()!=Player.STATE_ENDED)||RadioPlaybackService.selectedStation()!=null||RadioPlaybackService.selectedFile()!=null&&RadioPlaybackService.audioPlayer()!=null&&RadioPlaybackService.audioPlayer().getPlaybackState()!=Player.STATE_ENDED;}
 static void open(Context context){
  if(TvRecordingService.active()){android.widget.Toast.makeText(context,"Stop optagelsen, før du åbner en anden afspiller.",android.widget.Toast.LENGTH_SHORT).show();return;}
  Intent intent=new Intent(context,ExternalPlaybackActivity.class);
  try{
   if(CastPlayback.hasMedia(context)&&!CastPlayback.image(context)){var remote=CastPlayback.remote(context);var info=remote.getMediaInfo();intent.putExtra("source","cast").putExtra("live",info.getStreamType()==com.google.android.gms.cast.MediaInfo.STREAM_TYPE_LIVE).putExtra("uri",info.getContentId()).putExtra("mime",info.getContentType()).putExtra("name",CastPlayback.title(context)).putExtra("position",info.getStreamType()==com.google.android.gms.cast.MediaInfo.STREAM_TYPE_LIVE?0:remote.getApproximateStreamPosition());}
   else if(LocalAudioService.current()!=null&&!LocalAudioService.current().image()){LocalMedia file=LocalAudioService.current();intent.putExtra("source","local").putExtra("uri",file.uri).putExtra("name",file.name).putExtra("mime",CastPlayback.mime(file)).putExtra("position",LocalAudioService.player()==null?0:Math.max(0,LocalAudioService.player().getCurrentPosition()));}
   else if(RadioPlaybackService.selectedFile()!=null){LocalMedia file=RadioPlaybackService.selectedFile();intent.putExtra("source","carfile").putExtra("uri",file.uri).putExtra("name",file.name).putExtra("mime",CastPlayback.mime(file)).putExtra("position",RadioPlaybackService.audioPlayer().getCurrentPosition());}
   else{RadioStation station=RadioPlaybackService.selectedStation();if(station==null)return;intent.putExtra("source","radio").putExtra("radio",station.toJson().toString()).putExtra("uri",station.url).putExtra("name",station.displayName()).putExtra("mime",station.hls?"application/vnd.apple.mpegurl":"audio/*");}
   context.startActivity(intent);
  }catch(Exception error){android.widget.Toast.makeText(context,"Indholdet kunne ikke åbnes i en anden app.",android.widget.Toast.LENGTH_LONG).show();}
 }
 static void openVideo(MainActivity activity,Playlist.Channel channel,Player player){
  if(available(activity)&&CastPlayback.hasMedia(activity)){open(activity);return;}
  Intent intent=new Intent(activity,ExternalPlaybackActivity.class).putExtra("source","video").putExtra("uri",channel.url).putExtra("name",channel.name).putExtra("kind",channel.kind.name()).putExtra("logo",channel.logoUrl).putExtra("mime",ExternalPlaybackType.mime(channel.url,"video/*")).putExtra("position",channel.kind==Playlist.Kind.TV||player==null?0:Math.max(0,player.getCurrentPosition()));
  if(player!=null)player.pause();try{activity.startActivity(intent);}catch(RuntimeException failure){if(player!=null)player.play();android.widget.Toast.makeText(activity,"Indholdet kunne ikke åbnes i en anden app.",android.widget.Toast.LENGTH_LONG).show();}
 }
 @Override public void onCreate(Bundle saved){super.onCreate(saved);android.widget.TextView loading=new android.widget.TextView(this);loading.setText("Åbner afspiller …");loading.setGravity(android.view.Gravity.CENTER);setContentView(loading);if(saved!=null){returning.launched=saved.getBoolean("launched");returning.left=saved.getBoolean("left");}getOnBackPressedDispatcher().addCallback(this,new androidx.activity.OnBackPressedCallback(true){public void handleOnBackPressed(){resumeStream();}});if(!returning.launched)handler.post(this::launch);}
 private void launch(){if(isFinishing()||returning.finished)return;Intent target=getIntent();String value=target.getStringExtra("uri");if(value==null){finish();return;}try{
   String source=target.getStringExtra("source");
   if("local".equals(source)&&LocalAudioService.player()!=null)LocalAudioService.player().pause();
   if(("radio".equals(source)||"carfile".equals(source))&&RadioPlaybackService.audioPlayer()!=null)RadioPlaybackService.audioPlayer().pause();
   if("cast".equals(source)&&CastPlayback.remote(this)!=null)CastPlayback.remote(this).pause();
   Uri uri=PcloudDocument.is(value)?ExternalMediaProvider.share(this,new LocalMedia(value,target.getStringExtra("name"),target.getStringExtra("mime"),false)):Uri.parse(value);
   String mime=ExternalPlaybackType.mime(value,target.getStringExtra("mime"));Intent view=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).putExtra(Intent.EXTRA_TITLE,target.getStringExtra("name")).putExtra("title",target.getStringExtra("name")).putExtra("position",(int)Math.min(Integer.MAX_VALUE,Math.max(0,target.getLongExtra("position",0)))).putExtra("return_result",true);
   if("content".equals(uri.getScheme()))view.setClipData(ClipData.newRawUri("Afspilning",uri));
   if(view.resolveActivity(getPackageManager())==null)throw new ActivityNotFoundException();
   String session=UUID.randomUUID().toString();getSharedPreferences("external_player",0).edit().putString("session",session).putBoolean("selected",false).apply();PendingIntent chosen=PendingIntent.getBroadcast(this,7400,new Intent(this,ExternalPlayerChosenReceiver.class).putExtra("session",session),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE);returning.launched=true;startActivityForResult(Intent.createChooser(view,"Åbn i anden afspilningsapp",chosen.getIntentSender()),7400);
  }catch(Exception error){android.widget.Toast.makeText(this,"Ingen tilgængelig app kunne åbne afspilningen.",android.widget.Toast.LENGTH_LONG).show();resumeStream();}}
 @Override protected void onPause(){returning.foreground=false;if(returning.launched)returning.left=true;handler.removeCallbacks(returned);super.onPause();}
 @Override protected void onResume(){super.onResume();returning.foreground=true;scheduleReturn();}
 @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)scheduleReturn();else handler.removeCallbacks(returned);}
 private void scheduleReturn(){handler.removeCallbacks(returned);if(returning.launched&&returning.left)handler.postDelayed(returned,400);}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==7400){if(result==RESULT_OK&&data!=null){long position=data.getLongExtra("position",-1);if(position>=0)getIntent().putExtra("position",position);}scheduleReturn();}}
 private void resumeStream(){if(!returning.finish())return;getSharedPreferences("external_player",0).edit().remove("session").putBoolean("selected",false).apply();handler.removeCallbacks(returned);Intent target=getIntent();String source=target.getStringExtra("source");long position=Math.max(0,target.getLongExtra("position",0));try{
   if("local".equals(source)){LocalMedia file=new LocalMedia(target.getStringExtra("uri"),target.getStringExtra("name"),target.getStringExtra("mime"),false);List<LocalMedia> queue=LocalAudioService.queue();androidx.core.content.ContextCompat.startForegroundService(this,LocalAudioService.playIntent(this,file,queue.isEmpty()?Collections.singletonList(file):queue).putExtra("position",position));}
   else if("carfile".equals(source)){Player current=RadioPlaybackService.audioPlayer();if(current!=null){current.seekTo(position);current.play();}else{LocalMedia file=new LocalMedia(target.getStringExtra("uri"),target.getStringExtra("name"),target.getStringExtra("mime"),false);androidx.core.content.ContextCompat.startForegroundService(this,LocalAudioService.playIntent(this,file).putExtra("position",position));}}
   else if("radio".equals(source)){RadioStation station=RadioStation.fromJson(new org.json.JSONObject(target.getStringExtra("radio")));androidx.core.content.ContextCompat.startForegroundService(this,RadioPlaybackService.playIntent(this,station));}
   else if("cast".equals(source)){var remote=CastPlayback.remote(this);if(remote!=null){if(target.getBooleanExtra("live",false)&&remote.getMediaInfo()!=null)remote.load(new com.google.android.gms.cast.MediaLoadRequestData.Builder().setMediaInfo(remote.getMediaInfo()).setAutoplay(true).build());else remote.play();}}
   else if("video".equals(source)){Intent resume=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("resumeCastUrl",target.getStringExtra("uri")).putExtra("resumeCastName",target.getStringExtra("name")).putExtra("resumeCastKind",target.getStringExtra("kind")).putExtra("resumeCastLogo",target.getStringExtra("logo")).putExtra("resumeCastPosition",position);startActivity(resume);}
  }catch(Exception error){android.widget.Toast.makeText(this,"Afspilningen kunne ikke genoptages. Vælg den igen i Stream.",android.widget.Toast.LENGTH_LONG).show();}finish();}
 @Override protected void onSaveInstanceState(Bundle saved){saved.putBoolean("launched",returning.launched);saved.putBoolean("left",returning.left);super.onSaveInstanceState(saved);}
 @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
}
