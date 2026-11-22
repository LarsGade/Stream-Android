package dk.stream.android;
import java.io.*;
import java.util.regex.*;
final class DownloadRange {
 static void validate(String header,long offset)throws IOException{Matcher match=Pattern.compile("bytes (\\d+)-(\\d+)/(?:\\d+|\\*)").matcher(header==null?"":header);if(!match.matches()||Long.parseLong(match.group(1))!=offset)throw new IOException("Unexpected download range");}
 static long total(String header,long fallback){if(header!=null){int slash=header.lastIndexOf('/');if(slash>=0)try{return Long.parseLong(header.substring(slash+1));}catch(NumberFormatException ignored){}}return fallback;}
 static void skip(InputStream input,long offset)throws IOException{while(offset>0){long skipped=input.skip(offset);if(skipped>0)offset-=skipped;else if(input.read()<0)throw new EOFException("Download source shortened");else offset--;}}
 private DownloadRange(){}
}
