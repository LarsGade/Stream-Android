package dk.stream.android;
final class PlaybackText {
 static final String SOURCE="Afspilles via Stream";
 static androidx.media3.common.MediaMetadata.Builder metadata(String title){return new androidx.media3.common.MediaMetadata.Builder().setTitle(title).setDisplayTitle(title).setArtist(SOURCE).setSubtitle(SOURCE).setAlbumTitle(shortTitle(title)).setAlbumArtist(shortTitle(title));}
 private static final java.util.regex.Pattern EPISODE=java.util.regex.Pattern.compile("(?iu)\\b(?:S\\s*\\d+\\s*(?:[·._:/–—-]\\s*)?E\\s*\\d+|\\d+\\s*x\\s*\\d+|(?:sæson|season|episode|afsnit|ep\\.?)\\s*\\d+)\\b");
 static String shortTitle(String title){if(title==null||title.trim().isEmpty())return title;String value=title.trim();java.util.regex.Matcher match=EPISODE.matcher(value);if(!match.find())return title;if(match.start()>0){String base=trimSeparators(value.substring(0,match.start()));if(!base.isEmpty())return base;}else{String rest=trimSeparators(value.substring(match.end()));while(!rest.isEmpty()){java.util.regex.Matcher next=EPISODE.matcher(rest);if(!next.find()||next.start()!=0)break;rest=trimSeparators(rest.substring(next.end()));}if(!rest.isEmpty())return rest;}return title;}
 private static String trimSeparators(String value){return value.replaceAll("^[\\s·._:/–—()\\[\\]-]+|[\\s·._:/–—()\\[\\]-]+$","").trim();}
 private PlaybackText(){}
}
