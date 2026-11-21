package dk.stream.android;

import android.content.Context;
import android.net.Uri;
import androidx.media3.datasource.*;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import java.io.IOException;
import java.util.*;

/** Resolves only this account's opaque references on the player's loader thread. */
@androidx.media3.common.util.UnstableApi
public final class PcloudDataSource implements DataSource {
 private final Context context;private final List<TransferListener> listeners=new ArrayList<>();private DataSource active;private Uri original;private final DataSource.Factory fallback;
 public PcloudDataSource(Context context){this(context,new DefaultDataSource.Factory(context));}
 public PcloudDataSource(Context context,DataSource.Factory fallback){this.context=context.getApplicationContext();this.fallback=fallback;}
 @Override public void addTransferListener(TransferListener listener){listeners.add(listener);if(active!=null)active.addTransferListener(listener);}
 @Override public long open(DataSpec spec) throws IOException {
  original=spec.uri;
  if(SaavnMusic.is(spec.uri.toString())){SaavnSong song=SaavnMusic.resolve(context,SaavnMusic.id(spec.uri.toString()),false);active=new OkHttpDataSource.Factory(SaavnApi.STREAM).createDataSource();spec=spec.buildUpon().setUri(Uri.parse(song.stream)).build();}
  else if(PcloudDocument.is(spec.uri.toString())){
   try{PcloudAccount account=new LibraryStore(context).load().pcloud;okhttp3.HttpUrl url=PcloudDocument.url(account,spec.uri.toString());active=new OkHttpDataSource.Factory(new WebDavClient(account).streamClient()).createDataSource();spec=spec.buildUpon().setUri(Uri.parse(url.toString())).build();}
   catch(Exception invalid){throw new IOException("Log på Pcloud-kontoen igen for at afspille filen.");}
  }else active=fallback.createDataSource();
  for(TransferListener listener:listeners)active.addTransferListener(listener);
  try{return active.open(spec);}catch(IOException error){try{active.close();}catch(IOException ignored){}active=null;throw error;}
 }
 @Override public int read(byte[] buffer,int offset,int length) throws IOException {if(active==null)throw new IOException("Afspilleren er lukket.");return active.read(buffer,offset,length);}
 @Override public Uri getUri(){return original;}
 @Override public Map<String,List<String>> getResponseHeaders(){return active==null?Collections.emptyMap():active.getResponseHeaders();}
 @Override public void close() throws IOException {if(active!=null)try{active.close();}finally{active=null;original=null;}}
}
