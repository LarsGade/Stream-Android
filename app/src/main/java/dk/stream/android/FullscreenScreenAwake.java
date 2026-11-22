package dk.stream.android;

/** Fullscreen preference overrides ordinary inline playback screen behaviour. */
final class FullscreenScreenAwake {
 static boolean keep(boolean fullscreen,boolean enabled,boolean otherwise){return fullscreen?enabled:otherwise;}
 static void apply(android.view.Window window,boolean enabled){if(enabled)window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}
 private FullscreenScreenAwake(){}
}
