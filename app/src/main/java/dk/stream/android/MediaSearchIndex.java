package dk.stream.android;

import java.util.*;


/** Iterative, deduplicated indexing of both folders and files, independent of UI and storage. */
final class MediaSearchIndex {
 interface Cancelled {boolean getAsBoolean();}
 interface Published {void accept(List<LocalMedia> entries);}
 interface Children {List<LocalMedia> read(LocalMedia folder)throws Exception;}
 static void scan(List<LocalMedia> roots,Children children,Cancelled cancelled,Published publish){
  Map<String,LocalMedia> found=new LinkedHashMap<>();ArrayDeque<LocalMedia> pending=new ArrayDeque<>();
  for(LocalMedia root:roots)if(add(found,root)&&root.folder)pending.add(root);
  publish.accept(new ArrayList<>(found.values()));long last=System.nanoTime();
  while(!pending.isEmpty()&&!cancelled.getAsBoolean()){
   LocalMedia folder=pending.removeFirst();
   try{for(LocalMedia entry:children.read(folder)){if(cancelled.getAsBoolean())return;if(add(found,entry)&&entry.folder)pending.addLast(entry);}}
   catch(java.util.concurrent.CancellationException stop){return;}
   catch(Exception unavailable){if(cancelled.getAsBoolean())return;/* Other accessible branches must still be searched. */}
   long now=System.nanoTime();if(now-last>=150_000_000L){publish.accept(new ArrayList<>(found.values()));last=now;}
  }
  if(!cancelled.getAsBoolean())publish.accept(new ArrayList<>(found.values()));
 }
 static List<LocalMedia> matches(Collection<LocalMedia> index,String text){String query=text.trim().toLowerCase(Locale.ROOT);List<LocalMedia> result=new ArrayList<>();for(LocalMedia entry:index)if(entry.displayFileName().toLowerCase(Locale.ROOT).contains(query))result.add(entry);Collections.sort(result,NaturalNameOrder.MEDIA);return result;}
 private static boolean add(Map<String,LocalMedia> entries,LocalMedia entry){if(entries.containsKey(entry.uri))return false;entries.put(entry.uri,entry);return true;}
 private MediaSearchIndex(){}
}
