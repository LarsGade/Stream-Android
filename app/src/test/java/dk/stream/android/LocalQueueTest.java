package dk.stream.android;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class LocalQueueTest {
 private LocalMedia file(String id,boolean audio) {return new LocalMedia("content://docs/"+id,id+(audio?".mp3":".mp4"),audio?"audio/mpeg":"video/mp4",false);}
 @Test public void startsAtSelectedFileAndRetainsMixedListOrder() {
  LocalMedia a=file("first",true),b=file("second",false),c=file("third",true),folder=new LocalMedia("content://docs/tree","Folder","",true);
  LocalQueue queue=new LocalQueue(Arrays.asList(folder,a,b,c),b);
  assertEquals(Arrays.asList(a,b,c),queue.entries);assertEquals(1,queue.start);assertSame(c,queue.entries.get(queue.start+1));
 }
 @Test public void unavailableSelectionFallsBackToClickedFile() {
  LocalMedia clicked=file("clicked",true);LocalQueue queue=new LocalQueue(Collections.singletonList(file("other",false)),clicked);
  assertEquals(Collections.singletonList(clicked),queue.entries);assertEquals(0,queue.start);
 }
 @Test public void queueIsIndependentOfLaterNavigationAndCannotBeMutated() {
  LocalMedia a=file("first",true),b=file("second",true);List<LocalMedia> visible=new ArrayList<>(Arrays.asList(a,b));LocalQueue queue=new LocalQueue(visible,a);visible.clear();assertEquals(2,queue.entries.size());
  try {queue.entries.clear();fail();}catch(UnsupportedOperationException expected) {}
 }
 @Test public void duplicateDocumentLinksPlayOnlyOnce() {
  LocalMedia a=file("first",true),b=file("second",false);LocalQueue queue=new LocalQueue(Arrays.asList(a,b,a,b),b);assertEquals(Arrays.asList(a,b),queue.entries);assertEquals(1,queue.start);
 }
 @Test public void foldersCannotStartPlayback() {
  LocalMedia folder=new LocalMedia("content://docs/tree","Folder","",true);
  try {new LocalQueue(Collections.singletonList(folder),folder);fail();}catch(IllegalArgumentException expected) {}
 }
}
