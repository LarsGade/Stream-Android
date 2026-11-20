package dk.stream.android;
/** Manual fullscreen remains available when system rotation is locked. */
final class ImageFullscreenState {
 private Boolean landscape,manual;private boolean fullscreen;
 ImageFullscreenState(int saved){manual=saved<0?null:saved!=0;}
 boolean update(boolean landscape,boolean rotationEnabled){if(this.landscape!=null&&this.landscape!=landscape)manual=null;this.landscape=landscape;fullscreen=manual!=null?manual:landscape&&rotationEnabled;return fullscreen;}
 boolean toggle(){manual=!fullscreen;fullscreen=manual;return fullscreen;}
 int saved(){return manual==null?-1:manual?1:0;}
}
