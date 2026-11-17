package dk.stream.android;

import androidx.media3.common.*;

/** Standard media transport, with foreground promotion before audio focus is requested. */
@androidx.media3.common.util.UnstableApi
public final class RadioSessionPlayer extends ForwardingPlayer {
 public interface PlayGate {boolean allowed();}
 private final Runnable beforePlay;private final PlayGate allowPlay;
 public RadioSessionPlayer(Player player,Runnable beforePlay) {this(player,beforePlay,()->true);}
 public RadioSessionPlayer(Player player,Runnable beforePlay,PlayGate allowPlay){super(player);this.beforePlay=beforePlay;this.allowPlay=allowPlay;}
 @Override public void play() {
  if(!allowPlay.allowed())return;beforePlay.run();
  if(getPlaybackState()==STATE_IDLE) super.prepare();
  super.play();
 }
 @Override public void setPlayWhenReady(boolean ready) {if(ready) play();else super.setPlayWhenReady(false);}
 @Override public void stop() {super.setPlayWhenReady(false);super.stop();}
}
