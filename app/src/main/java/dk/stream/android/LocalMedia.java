package dk.stream.android;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import java.util.*;
import org.json.*;

/** References to user-selected documents. No media bytes are imported or copied. */
public final class LocalMedia {
 public final String uri,name,mime;
 public final boolean folder;
 public LocalMedia(String uri,String name,String mime,boolean folder) {
  if(!"content".equals(java.net.URI.create(uri).getScheme())) throw new IllegalArgumentException("Document URI required");
  this.uri=uri;this.name=name==null?"Mediefil":name;this.mime=mime==null?"":mime;this.folder=folder;
 }
 public boolean audio() {return mime.startsWith("audio/") || name.toLowerCase(Locale.ROOT).matches(".*\\.(mp3|m4a|aac|flac|wav|ogg|opus|wma|aiff|alac)$");}
 public boolean supported() {return folder || audio() || mime.startsWith("video/") || name.toLowerCase(Locale.ROOT).matches(".*\\.(mp4|mkv|webm|avi|mov|m4v|mpeg|mpg|3gp|ts)$");}
 public JSONObject json() throws JSONException {return new JSONObject().put("uri",uri).put("name",name).put("mime",mime).put("folder",folder);}
 public static LocalMedia from(JSONObject value) throws JSONException {return new LocalMedia(value.getString("uri"),value.getString("name"),value.optString("mime",""),value.optBoolean("folder"));}
 public static LocalMedia describe(Context context,Uri uri,boolean tree) {
  if(tree) uri=DocumentsContract.buildDocumentUriUsingTree(uri,DocumentsContract.getTreeDocumentId(uri));
  String name="",mime=context.getContentResolver().getType(uri);
  try(Cursor cursor=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)) {if(cursor!=null && cursor.moveToFirst()) name=cursor.getString(0);}
  if(name==null || name.isEmpty()) name=tree?"Mappe":"Mediefil";
  return new LocalMedia(uri.toString(),name,mime==null?"":mime,tree);
 }
 public static List<LocalMedia> children(Context context,LocalMedia folder) {
  Uri parent=Uri.parse(folder.uri),children=DocumentsContract.buildChildDocumentsUriUsingTree(parent,DocumentsContract.getDocumentId(parent));
  List<LocalMedia> result=new ArrayList<>();
  String[] columns={DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE};
  try(Cursor cursor=context.getContentResolver().query(children,columns,null,null,null)) {
   if(cursor==null) throw new IllegalStateException("Mappe utilgængelig");
   while(cursor.moveToNext()) {
    String mime=cursor.getString(2);if(mime==null) mime="";
    LocalMedia entry=new LocalMedia(DocumentsContract.buildDocumentUriUsingTree(parent,cursor.getString(0)).toString(),cursor.getString(1),mime,DocumentsContract.Document.MIME_TYPE_DIR.equals(mime));
    if(entry.supported()) result.add(entry);
   }
  }
  Collections.sort(result,(a,b)->a.folder!=b.folder?(a.folder?-1:1):a.name.compareToIgnoreCase(b.name));return result;
 }
 /** Iterative traversal includes every depth and guards against provider cycles. */
 public static List<LocalMedia> contents(Context context,LocalMedia root,boolean recursive) {
  List<LocalMedia> direct=children(context,root);
  if(!recursive) return direct;
  List<LocalMedia> result=new ArrayList<>(direct);
  ArrayDeque<LocalMedia> pending=new ArrayDeque<>();Set<String> visited=new HashSet<>();visited.add(root.uri);
  for(LocalMedia entry:direct) if(entry.folder) pending.add(entry);
  while(!pending.isEmpty()) {
   if(Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
   LocalMedia folder=pending.removeFirst();if(!visited.add(folder.uri)) continue;
   for(LocalMedia entry:children(context,folder)) {
    LocalMedia relative=new LocalMedia(entry.uri,folder.name+" / "+entry.name,entry.mime,entry.folder);
    if(entry.folder) pending.addLast(relative);else result.add(relative);
   }
  }
  return result;
 }
 @Override public String toString() {return name;}
}
