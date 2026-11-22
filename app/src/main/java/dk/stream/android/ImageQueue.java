package dk.stream.android;
import java.util.*;
/** A stable picture-only snapshot of the displayed order. */
public final class ImageQueue {
 public final List<LocalMedia> entries;public final int start;
 public ImageQueue(List<LocalMedia> displayed,LocalMedia selected){if(!selected.image())throw new IllegalArgumentException("Image required");List<LocalMedia> images=new ArrayList<>();Set<String> seen=new HashSet<>();int index=-1;for(LocalMedia entry:displayed)if(entry.image()&&seen.add(entry.uri)){if(entry.uri.equals(selected.uri))index=images.size();images.add(entry);}if(index<0){images.clear();images.add(selected);index=0;}entries=Collections.unmodifiableList(images);start=index;}
 public int move(int current,int direction){return (int)Math.max(0,Math.min(entries.size()-1,(long)current+direction));}
 public int indexOf(String uri){for(int i=0;i<entries.size();i++)if(entries.get(i).uri.equals(uri))return i;return -1;}
}
