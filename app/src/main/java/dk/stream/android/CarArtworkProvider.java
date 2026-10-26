package dk.stream.android;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Read-only public access to packaged artwork, never arbitrary app files or source URLs. */
public final class CarArtworkProvider extends ContentProvider {
 public static Uri uri(Context context,String name) {return new Uri.Builder().scheme("content").authority(context.getPackageName()+".carart").appendPath("logo").appendPath(name).build();}
 @Override public boolean onCreate() {return true;}
 @Override public String getType(Uri uri) {return "image/png";}
 @Override public synchronized ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
  if(!"r".equals(mode) || uri.getPathSegments().size()!=2 || !"logo".equals(uri.getPathSegments().get(0))) throw new FileNotFoundException("Read-only artwork");
  String name=uri.getLastPathSegment();String asset=LocalChannelLogos.load(getContext()).find(name);
  String fileName=asset==null?"Stream.png":asset.substring(asset.lastIndexOf('/')+1);
  File directory=new File(getContext().getCacheDir(),"car_artwork");if(!directory.exists()&&!directory.mkdirs()) throw new FileNotFoundException("Artwork cache unavailable");
  File file=new File(directory,fileName);if(!file.exists()) {
   byte[] data=PlaybackArtwork.local(getContext(),name);if(data==null) throw new FileNotFoundException("Artwork unavailable");
   try(FileOutputStream out=new FileOutputStream(file)) {out.write(data);} catch(IOException e) {throw new FileNotFoundException("Artwork unavailable");}
  }
  return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
 }
 @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort) {
  String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor cursor=new MatrixCursor(columns);Object[] values=new Object[columns.length];
  for(int i=0;i<columns.length;i++) if(OpenableColumns.DISPLAY_NAME.equals(columns[i])) values[i]="logo.png";cursor.addRow(values);return cursor;
 }
 @Override public Uri insert(Uri uri,ContentValues values) {throw new UnsupportedOperationException("Read only");}
 @Override public int update(Uri uri,ContentValues values,String selection,String[] args) {throw new UnsupportedOperationException("Read only");}
 @Override public int delete(Uri uri,String selection,String[] args) {throw new UnsupportedOperationException("Read only");}
}
