package dk.stream.android;

import androidx.media3.common.*;

/** Every pause request is a real stop; play always opens a fresh live stream. */
@androidx.media3.common.util.UnstableApi
public final class LiveRadioPlayer extends ForwardingPlayer {
 private final Player delegate;
 private final Runnable beforePlay;
 public LiveRadioPlayer(Player player) {this(player,()->{});}
 public LiveRadioPlayer(Player player,Runnable beforePlay) {super(new RadioCommandsPlayer(player));delegate=player;this.beforePlay=beforePlay;}
 @Override public void pause() {stop();}
 @Override public void stop() {delegate.setPlayWhenReady(false);delegate.stop();}
 @Override public void setPlayWhenReady(boolean ready) {if(ready) play();else stop();}
 @Override public void play() {
  if(delegate.getPlayWhenReady() && delegate.getPlaybackState()!=STATE_IDLE) return;
  MediaItem item=delegate.getCurrentMediaItem();if(item==null) return;
  beforePlay.run();delegate.stop();delegate.setMediaItem(item,true);delegate.prepare();delegate.play();
 }
 // Direct transport calls keep their stop semantics even when pause is not advertised.
 @Override public boolean getPlayWhenReady() {return delegate.getPlayWhenReady();}
 @Override public int getPlaybackState() {return delegate.getPlaybackState();}
 @Override public MediaMetadata getMediaMetadata() {return delegate.getMediaMetadata();}
 @Override public long getCurrentPosition() {return delegate.getCurrentPosition();}
 private static final class RadioCommandsPlayer extends ForwardingSimpleBasePlayer {
  RadioCommandsPlayer(Player player) {super(new ForwardingPlayer(player) {
   // ExoPlayer can briefly retain isLoading after stop; SimpleBasePlayer requires a consistent state.
   @Override public boolean isLoading() {int state=getPlaybackState();return state!=STATE_IDLE && state!=STATE_ENDED && super.isLoading();}
  });}
  @Override protected State getState() {
   State state=super.getState();Commands.Builder commands=state.availableCommands.buildUpon().remove(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM).remove(COMMAND_SEEK_BACK).remove(COMMAND_SEEK_FORWARD).remove(COMMAND_SET_SPEED_AND_PITCH)
    .remove(COMMAND_SEEK_TO_DEFAULT_POSITION).remove(COMMAND_SEEK_TO_MEDIA_ITEM)
    .remove(COMMAND_SEEK_TO_PREVIOUS).remove(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
    .remove(COMMAND_SEEK_TO_NEXT).remove(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM);
   if(state.playWhenReady) commands.remove(COMMAND_PLAY_PAUSE);
   return state.buildUpon().setAvailableCommands(commands.build()).build();
  }
 }
}
