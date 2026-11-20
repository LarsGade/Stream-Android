package dk.stream.android;
import java.io.*;
import java.util.*;
import java.util.zip.*;
final class DownloadArchive {
 interface Copy {void copy(int index,OutputStream output)throws Exception;}
 static void write(List<String> names,OutputStream output,Copy copy)throws Exception{if(names.isEmpty())throw new IOException("No files");if(names.size()==1){copy.copy(0,output);return;}Set<String> used=new HashSet<>();try(ZipOutputStream zip=new ZipOutputStream(output)){zip.setLevel(Deflater.NO_COMPRESSION);for(int index=0;index<names.size();index++){zip.putNextEntry(new ZipEntry(DownloadNames.unique(names.get(index),used)));copy.copy(index,zip);zip.closeEntry();}}}
 private DownloadArchive(){}
}
