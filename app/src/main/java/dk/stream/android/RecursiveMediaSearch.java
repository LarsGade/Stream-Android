package dk.stream.android;

import android.content.Context;
import android.os.*;
import java.util.*;
import java.util.concurrent.*;

/** A page-scoped background index reused while the user edits the query. Main-thread API. */
final class RecursiveMediaSearch {
 private final Context context;private final Runnable changed;private final Handler main=new Handler(Looper.getMainLooper());
 private final ExecutorService worker=Executors.newSingleThreadExecutor();private final List<LocalMedia> index=new ArrayList<>();
 private List<String> scope=Collections.emptyList();private PcloudAccount account;private Future<?> task;private Runnable pending;private volatile int generation;private volatile boolean closed;private volatile WebDavClient client;
 RecursiveMediaSearch(Context context,Runnable changed){this.context=context.getApplicationContext();this.changed=changed;}
 List<LocalMedia> search(String query,List<LocalMedia> roots,PcloudAccount account){
  List<String> key=new ArrayList<>();for(LocalMedia root:roots)key.add(root.uri+"\n"+root.name);
  if(!key.equals(scope)||this.account!=account){clear();scope=key;this.account=account;index.addAll(roots);int request=generation;List<LocalMedia> captured=new ArrayList<>(roots);pending=()->{pending=null;if(closed||request!=generation)return;task=worker.submit(()->scan(captured,account,request));};main.postDelayed(pending,200);}
  return MediaSearchIndex.matches(index,query);
 }
 private void scan(List<LocalMedia> roots,PcloudAccount account,int request){
  WebDavClient cloud=account==null?null:new WebDavClient(account);client=cloud;
  try{MediaSearchIndex.scan(roots,folder->{if(PcloudDocument.is(folder.uri)){if(cloud==null)return Collections.emptyList();List<LocalMedia> result=new ArrayList<>();for(WebDavClient.Entry entry:cloud.list(PcloudDocument.url(account,folder.uri)))result.add(entry.media(account));return result;}return LocalMedia.children(context,folder);},()->closed||generation!=request||Thread.currentThread().isInterrupted(),snapshot->main.post(()->{if(!closed&&generation==request){index.clear();index.addAll(snapshot);changed.run();}}));}
  finally{if(cloud!=null)cloud.cancel();if(client==cloud)client=null;}
 }
 void clear(){generation++;if(pending!=null){main.removeCallbacks(pending);pending=null;}if(task!=null){task.cancel(true);task=null;}WebDavClient active=client;if(active!=null)active.cancel();scope=Collections.emptyList();account=null;index.clear();}
 void close(){closed=true;clear();worker.shutdownNow();}
}
