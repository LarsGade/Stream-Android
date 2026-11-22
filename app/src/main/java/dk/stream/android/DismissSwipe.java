package dk.stream.android;
final class DismissSwipe {
 static boolean horizontal(float dx,float dy,float slop){return Math.abs(dx)>slop&&Math.abs(dx)>Math.abs(dy)*1.5f;}
 static boolean dismiss(float dx,float dy,float width,float density){return horizontal(dx,dy,0)&&Math.abs(dx)>=Math.max(48*density,width*.2f);}
 private DismissSwipe(){}
}
