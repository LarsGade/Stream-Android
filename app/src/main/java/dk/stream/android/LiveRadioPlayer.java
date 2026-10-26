package dk.stream.android;

import androidx.media3.common.*;

/** Every pause request is a real stop; play always opens a fresh live stream. */
@androidx.media3.common.util.UnstableApi
public final class LiveRadioPlayer extends ForwardingPlayer {
 private final Player delegate;
 private final Runnable beforePlay;
 public LiveRadioPlayer(Player player) {this(player,()->{});}
 public LiveRadioPlayer(Player player,Runnable beforePlay) {super(player);delegate=player;this.beforePlay=beforePlay;}
 @Override public void pause() {stop();}
 @Override public void stop() {delegate.setPlayWhenReady(false);delegate.stop();}
 @Override public void setPlayWhenReady(boolean ready) {if(ready) play();else stop();}
 @Override public void play() {
  if(delegate.getPlayWhenReady() && delegate.getPlaybackState()!=STATE_IDLE) return;
  MediaItem item=delegate.getCurrentMediaItem();if(item==null) return;
  beforePlay.run();delegate.stop();delegate.setMediaItem(item,true);delegate.prepare();delegate.play();
 }
 @Override public Commands getAvailableCommands() {return super.getAvailableCommands().buildUpon().remove(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM).remove(COMMAND_SEEK_BACK).remove(COMMAND_SEEK_FORWARD).remove(COMMAND_SET_SPEED_AND_PITCH).build();}
 @Override public boolean isCommandAvailable(int command) {return getAvailableCommands().contains(command);}
}
