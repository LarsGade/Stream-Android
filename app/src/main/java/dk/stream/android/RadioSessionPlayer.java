package dk.stream.android;

import androidx.media3.common.*;

/** Standard media transport, with foreground promotion before audio focus is requested. */
@androidx.media3.common.util.UnstableApi
public final class RadioSessionPlayer extends ForwardingPlayer {
 private final Runnable beforePlay;
 public RadioSessionPlayer(Player player,Runnable beforePlay) {super(player);this.beforePlay=beforePlay;}
 @Override public void play() {
  beforePlay.run();
  if(getPlaybackState()==STATE_IDLE) super.prepare();
  super.play();
 }
 @Override public void setPlayWhenReady(boolean ready) {if(ready) play();else super.setPlayWhenReady(false);}
 @Override public void stop() {super.setPlayWhenReady(false);super.stop();}
}
