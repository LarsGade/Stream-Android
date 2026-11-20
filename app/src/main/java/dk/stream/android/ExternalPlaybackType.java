package dk.stream.android;
import java.util.Locale;
final class ExternalPlaybackType {
 static String mime(String uri,String fallback){String path=uri==null?"":uri.split("[?#]",2)[0].toLowerCase(Locale.ROOT);if(path.endsWith(".m3u8"))return "application/vnd.apple.mpegurl";if(path.endsWith(".mpd"))return "application/dash+xml";if(path.endsWith(".mp3"))return "audio/mpeg";if(path.endsWith(".m4a")||path.endsWith(".m4b"))return "audio/mp4";if(path.endsWith(".aac"))return "audio/aac";if(path.endsWith(".flac"))return "audio/flac";if(path.endsWith(".wav"))return "audio/wav";if(path.endsWith(".ogg")||path.endsWith(".opus"))return "audio/ogg";if(path.endsWith(".mp4")||path.endsWith(".m4v"))return "video/mp4";if(path.endsWith(".mkv"))return "video/x-matroska";if(path.endsWith(".webm"))return "video/webm";if(path.endsWith(".ts"))return "video/mp2t";return fallback==null||fallback.isEmpty()||fallback.equals("application/octet-stream")?"video/*":fallback;}
 private ExternalPlaybackType(){}
}
