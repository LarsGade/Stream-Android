package dk.stream.android;
/** Transient ownership: a chooser selection never survives a return to Stream or process restart. */
final class ExternalOutputState {
 private static String token,player;
 static synchronized void begin(String value){token=value;player=null;}
 static synchronized void choose(String value,String packageName,String streamPackage){if(value!=null&&value.equals(token)&&packageName!=null&&!packageName.equals(streamPackage))player=packageName;}
 static synchronized void end(String value){if(value!=null&&value.equals(token)){token=null;player=null;}}
 static synchronized boolean pending(){return token!=null;}
 static synchronized boolean active(){return token!=null&&player!=null;}
 static synchronized boolean blue(boolean streamPlaying){if(streamPlaying){if(player!=null){token=null;player=null;}return false;}return active();}
 private ExternalOutputState(){}
}
