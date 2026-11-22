package dk.stream.android;

import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class MediaSearchIndexTest {
 private static LocalMedia folder(String id,String name){return new LocalMedia("content://documents/"+id,name,"vnd.android.document/directory",true);}
 private static LocalMedia file(String id,String name){return new LocalMedia("content://documents/"+id,name,"audio/mpeg",false);}
 @Test public void findsUnmatchedFoldersDescendantsAndSortsMatchingFoldersFirst(){
  LocalMedia root=folder("root","Root"),outer=folder("outer","Unrelated"),nested=folder("nested","Episode folder"),song=file("song","Episode 10.mp3"),second=file("second","Episode 2.mp3");
  Map<String,List<LocalMedia>> tree=new HashMap<>();tree.put(root.uri,Arrays.asList(outer,second));tree.put(outer.uri,Arrays.asList(nested));tree.put(nested.uri,Arrays.asList(song));
  List<LocalMedia> index=new ArrayList<>();MediaSearchIndex.scan(Arrays.asList(root),f->tree.get(f.uri),()->false,s->{index.clear();index.addAll(s);});
  List<LocalMedia> result=MediaSearchIndex.matches(index,"EPISODE");assertEquals(Arrays.asList(nested,second,song),result);
  assertEquals(Arrays.asList(song),MediaSearchIndex.matches(index,"episode 10"));
 }
 @Test public void cyclesAndOverlappingFavoriteRootsAreReadOnce(){
  LocalMedia root=folder("root","Root"),child=folder("child","Child"),song=file("song","Song.mp3");AtomicInteger reads=new AtomicInteger();List<LocalMedia> result=new ArrayList<>();
  MediaSearchIndex.scan(Arrays.asList(root,child,song),f->{reads.incrementAndGet();return f==root?Arrays.asList(child,song):Arrays.asList(root,song);},()->false,s->{result.clear();result.addAll(s);});
  assertEquals(2,reads.get());assertEquals(3,result.size());
 }
 @Test public void inaccessibleBranchDoesNotHideOtherRootsOrCloudResults(){
  LocalMedia denied=folder("denied","Denied"),cloud=new LocalMedia("content://dk.stream.android.pcloud/account?url=folder","Cloud", "directory",true),song=file("song","Found.mp3");List<LocalMedia> result=new ArrayList<>();
  MediaSearchIndex.scan(Arrays.asList(denied,cloud),f->{if(f==denied)throw new java.io.IOException("No access");return Arrays.asList(song);},()->false,s->{result.clear();result.addAll(s);});assertTrue(result.contains(song));
 }
 @Test public void cancellationStopsBeforeReadingRemainingFolders(){
  AtomicBoolean cancelled=new AtomicBoolean();AtomicInteger reads=new AtomicInteger();LocalMedia root=folder("root","Root");
  MediaSearchIndex.scan(Arrays.asList(root),f->{reads.incrementAndGet();cancelled.set(true);return Arrays.asList(folder("next","Next"));},cancelled::get,s->{});assertEquals(1,reads.get());
 }
 @Test public void deepTreesUseIterationAndPublishBeforeCompletion(){
  AtomicInteger publications=new AtomicInteger();List<LocalMedia> result=new ArrayList<>();
  MediaSearchIndex.scan(Arrays.asList(folder("0","Folder 0")),f->{int depth=Integer.parseInt(java.net.URI.create(f.uri).getPath().substring(1));return depth<1500?Arrays.asList(folder(""+(depth+1),"Folder "+(depth+1))):Arrays.asList(file("end","Final.mp3"));},()->false,s->{publications.incrementAndGet();result.clear();result.addAll(s);});
  assertTrue(publications.get()>=2);assertEquals(1502,result.size());assertEquals(1,MediaSearchIndex.matches(result,"Final").size());
 }
}
