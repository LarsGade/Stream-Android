package dk.stream.android;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;
/** Private Saavn song references can be read through the app's temporary external relay. */
public final class SaavnMediaProvider extends ContentProvider {
 @Override public boolean onCreate(){return true;}
 @Override public String getType(Uri uri){if(uri.getPathSegments().size()==2&&"cover".equals(uri.getPathSegments().get(0)))return "image/png";SaavnSong song=SaavnMusic.peek(uri.getLastPathSegment());return song==null?"audio/mp4":SaavnMusic.media(song).mime;}
 private String id(Uri uri)throws FileNotFoundException{try{return SaavnMusic.id(uri.toString());}catch(Exception invalid){throw new FileNotFoundException("Invalid song");}}
 @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException("Read only");String id=id(uri);String kind=uri.getPathSegments().get(0);try{if("song".equals(kind))return HttpMediaFile.open(getContext(),SaavnMusic.resolve(getContext(),id,false).stream);if(!"cover".equals(kind))throw new IOException();File directory=new File(getContext().getCacheDir(),"music-covers");if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException();File image=new File(directory,id+".png");if(!image.exists()){SaavnSong song=SaavnMusic.resolve(getContext(),id,false);byte[] bytes=PlaybackArtwork.remote(getContext(),song.image);if(bytes==null)throw new IOException();synchronized(this){if(!image.exists()){android.util.AtomicFile atomic=new android.util.AtomicFile(image);FileOutputStream output=null;try{output=atomic.startWrite();output.write(bytes);atomic.finishWrite(output);}catch(IOException failure){if(output!=null)atomic.failWrite(output);throw failure;}}}}return ParcelFileDescriptor.open(image,ParcelFileDescriptor.MODE_READ_ONLY);}catch(Exception failure){throw new FileNotFoundException("Musikken kunne ikke indlæses fra Saavn.");}}
 @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){try{String id=id(uri);SaavnMusic.load(getContext());SaavnSong song=SaavnMusic.peek(id);String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor cursor=new MatrixCursor(columns);Object[] row=new Object[columns.length];boolean cover="cover".equals(uri.getPathSegments().get(0));for(int i=0;i<columns.length;i++){if(OpenableColumns.DISPLAY_NAME.equals(columns[i]))row[i]=(song==null?id:song.title)+(cover?".png":".m4a");if(OpenableColumns.SIZE.equals(columns[i])&&!cover){if(song==null)song=SaavnMusic.resolve(getContext(),id,false);long size=HttpMediaFile.size(SaavnApi.STREAM,okhttp3.HttpUrl.get(song.stream));if(size>=0)row[i]=size;}}cursor.addRow(row);return cursor;}catch(Exception invalid){return null;}}
 @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
 @Override public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}
 @Override public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
}
