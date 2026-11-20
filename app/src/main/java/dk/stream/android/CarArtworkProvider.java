package dk.stream.android;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Read-only public access to packaged artwork, never arbitrary app files or source URLs. */
@androidx.media3.common.util.UnstableApi
public final class CarArtworkProvider extends ContentProvider {
 static Uri audio(String id){return new Uri.Builder().scheme("content").authority(BuildConfig.APPLICATION_ID+".carart").appendPath("audio").appendPath(id).build();}
 static Uri page(String id){return new Uri.Builder().scheme("content").authority(BuildConfig.APPLICATION_ID+".carart").appendPath("page").appendPath(id).build();}
 static Uri station(Context context,String id){return new Uri.Builder().scheme("content").authority(context.getPackageName()+".carart").appendPath("station").appendPath(id).build();}
 public static Uri uri(Context context,String name) {return new Uri.Builder().scheme("content").authority(context.getPackageName()+".carart").appendPath("logo").appendPath(name).build();}
 @Override public boolean onCreate() {return true;}
 @Override public String getType(Uri uri) {return "image/png";}
 @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
  if(!"r".equals(mode)||uri.getPathSegments().size()!=2)throw new FileNotFoundException("Read-only artwork");
  String type=uri.getPathSegments().get(0);if("page".equals(type)){String id=uri.getLastPathSegment();if(!AndroidAutoPages.folder(id)&&!"radio-placeholder".equals(id))throw new FileNotFoundException("Unknown page");return cached("page-v1-"+id+".png",CarPageArtwork.png(id));}
  if("audio".equals(type)){try{LocalMedia file=CarAudioCatalog.resolve(getContext(),uri.getLastPathSegment());LocalMedia logoFile=file.folder?new LocalMedia(file.uri,file.name+" / episode.mp3","audio/mpeg",false):file;byte[] data=LocalFileArtwork.bytes(getContext(),logoFile);return cached("audio-v1-"+java.util.UUID.nameUUIDFromBytes((file.uri+file.name).getBytes(java.nio.charset.StandardCharsets.UTF_8))+".png",data!=null?data:CarPageArtwork.png(AndroidAutoPages.PODCAST));}catch(Exception error){throw new FileNotFoundException("Audio artwork unavailable");}}
  if("station".equals(type))return stationFile(uri.getLastPathSegment());
  if(!"logo".equals(type))throw new FileNotFoundException("Unknown artwork");
  String name=uri.getLastPathSegment();String asset=LocalChannelLogos.load(getContext()).find(name);
  String fileName=asset==null?"Stream.png":asset.substring(asset.lastIndexOf('/')+1);
  File directory=new File(getContext().getCacheDir(),"car_artwork");if(!directory.exists()&&!directory.mkdirs()) throw new FileNotFoundException("Artwork cache unavailable");
  File file=new File(directory,fileName);if(!file.exists()) {
   byte[] data=PlaybackArtwork.local(getContext(),name);if(data==null) throw new FileNotFoundException("Artwork unavailable");
   try(FileOutputStream out=new FileOutputStream(file)) {out.write(data);} catch(IOException e) {throw new FileNotFoundException("Artwork unavailable");}
  }
  return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
 }
 private synchronized ParcelFileDescriptor cached(String name,byte[] data)throws FileNotFoundException{File directory=new File(getContext().getCacheDir(),"car_artwork");if(!directory.exists()&&!directory.mkdirs())throw new FileNotFoundException("Artwork cache unavailable");File file=new File(directory,name);try{if(!file.exists()){android.util.AtomicFile atomic=new android.util.AtomicFile(file);FileOutputStream out=null;try{out=atomic.startWrite();out.write(data);atomic.finishWrite(out);}catch(IOException failure){if(out!=null)atomic.failWrite(out);throw failure;}}return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);}catch(IOException failure){file.delete();throw new FileNotFoundException("Artwork unavailable");}}
 private final java.util.Map<String,RadioStation> cachedStations=new java.util.HashMap<>();private long cacheModified=-1,cacheLength=-1;
 private synchronized RadioStation cachedStation(String id){File file=new File(getContext().getFilesDir(),"car_radio.json");if(!file.exists())file=new File(getContext().getCacheDir(),"car_radio.json");if(file.lastModified()!=cacheModified||file.length()!=cacheLength){cachedStations.clear();for(RadioStation station:CarRadioCatalog.readCache(getContext()))cachedStations.put(station.id,station);cacheModified=file.lastModified();cacheLength=file.length();}return cachedStations.get(id);}
 private ParcelFileDescriptor stationFile(String id)throws FileNotFoundException{
  RadioStation station=null;try{for(RadioStation candidate:new LibraryStore(getContext()).load().radioFavorites)if(candidate.id.equals(id)){station=candidate;break;}}catch(Exception ignored){}if(station==null)station=cachedStation(id);
  if(station==null)return cached("radio-placeholder-v1.png",CarPageArtwork.png("radio-placeholder"));
  String logo=PlaybackArtwork.logoName(getContext(),station);boolean bundled=PlaybackArtwork.bundled(getContext(),logo);String signature=(bundled?"assets-"+BuildConfig.VERSION_CODE:"remote-v1")+"\n"+station.id+"\n"+station.name+"\n"+station.alias+"\n"+station.logo;String key="station-"+java.util.UUID.nameUUIDFromBytes(signature.getBytes(java.nio.charset.StandardCharsets.UTF_8))+".png";File directory=new File(getContext().getCacheDir(),"car_artwork");File file=new File(directory,key),fallback=new File(directory,key+".fallback");if(file.exists()&&(!fallback.exists()||System.currentTimeMillis()-file.lastModified()<5*60*1000))return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
  byte[] bytes=bundled?PlaybackArtwork.local(getContext(),logo):PlaybackArtwork.remote(getContext(),station.logo);boolean missing=bytes==null;if(missing)bytes=CarPageArtwork.png("radio-placeholder");file.delete();ParcelFileDescriptor result=cached(key,bytes);try{if(missing){if(!fallback.exists())fallback.createNewFile();}else fallback.delete();}catch(IOException ignored){}return result;
 }
 @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort) {
  String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor cursor=new MatrixCursor(columns);Object[] values=new Object[columns.length];
  for(int i=0;i<columns.length;i++) if(OpenableColumns.DISPLAY_NAME.equals(columns[i])) values[i]="logo.png";cursor.addRow(values);return cursor;
 }
 @Override public Uri insert(Uri uri,ContentValues values) {throw new UnsupportedOperationException("Read only");}
 @Override public int update(Uri uri,ContentValues values,String selection,String[] args) {throw new UnsupportedOperationException("Read only");}
 @Override public int delete(Uri uri,String selection,String[] args) {throw new UnsupportedOperationException("Read only");}
}
