package dk.stream.android;
import java.io.InterruptedIOException;
final class DownloadControl {
 private boolean paused,cancelled;private int revision;
 synchronized void pause(){if(!cancelled&&!paused){paused=true;revision++;}}
 synchronized void resume(){paused=false;notifyAll();}
 synchronized void cancel(){cancelled=true;paused=false;notifyAll();}
 synchronized boolean paused(){return paused;}synchronized boolean cancelled(){return cancelled;}synchronized int revision(){return revision;}
 synchronized void await()throws InterruptedIOException{while(paused&&!cancelled)try{wait();}catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new InterruptedIOException("Download interrupted");}if(cancelled)throw new InterruptedIOException("Download cancelled");}
}
