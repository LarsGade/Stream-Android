package dk.stream.android;
final class MediaFallback {
 static int icon(LocalMedia file){return file!=null&&file.folder?R.drawable.ic_folder:file!=null&&file.image()?R.drawable.ic_image:file!=null&&!file.audio()?R.drawable.ic_video:R.drawable.ic_music_note;}
 private MediaFallback(){}
}
