package dk.stream.android;

import java.util.*;

/** Snapshot of the displayed files; folder rows are never playback items. */
public final class LocalQueue {
 public final List<LocalMedia> entries;
 public final int start;
 public LocalQueue(List<LocalMedia> displayed,LocalMedia selected) {
  List<LocalMedia> files=new ArrayList<>();Set<String> seen=new HashSet<>();int index=-1;
  for(LocalMedia entry:displayed) if(!entry.folder && entry.supported() && entry.image()==selected.image() && seen.add(entry.uri)) {
   if(entry.uri.equals(selected.uri)) index=files.size();files.add(entry);
  }
  if(index<0) {files.clear();files.add(selected);index=0;}
  if(selected.folder || !selected.supported()) throw new IllegalArgumentException("Media file required");
  entries=Collections.unmodifiableList(files);start=index;
 }
}
