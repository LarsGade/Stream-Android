package dk.stream.android;
import java.util.*;
/** Metadata only; never retains decoded pixels or an Activity. */
public final class ImageSession {
 private static ImageQueue queue;private static int index;private static String token;private static Object visibleOwner;
 public static String start(LocalMedia selected,List<LocalMedia> displayed){queue=new ImageQueue(displayed,selected);index=queue.start;token=UUID.randomUUID().toString();return token;}
 public static boolean restore(String expected,LocalMedia fallback){if(queue!=null&&Objects.equals(token,expected))return true;start(fallback,Collections.singletonList(fallback));return false;}
 public static LocalMedia current(){return queue==null?null:queue.entries.get(index);}
 public static List<LocalMedia> entries(){return queue==null?Collections.emptyList():queue.entries;}
 public static String token(){return token;}public static int index(){return index;}public static int size(){return queue==null?0:queue.entries.size();}
 public static boolean move(int direction){if(queue==null)return false;int next=queue.move(index,direction);if(next==index)return false;index=next;return true;}
 public static boolean select(String uri){if(queue==null)return false;int next=queue.indexOf(uri);if(next<0||next==index)return false;index=next;return true;}
 public static void visible(Object owner){visibleOwner=owner;}public static void hidden(Object owner){if(visibleOwner==owner)visibleOwner=null;}public static boolean viewing(){return visibleOwner!=null&&queue!=null;}
 public static long favoriteVersion;public static void favoritesChanged(){favoriteVersion++;}
 private ImageSession(){}
}
