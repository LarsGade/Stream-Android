package dk.stream.android;
final class DownloadProgress {
 static int basisPoints(int done,int total,long copied,long expected){double fraction=expected>0?Math.max(0,Math.min(1,copied/(double)expected)):0;return (int)Math.max(0,Math.min(10000,(done+fraction)*10000/Math.max(1,total)));}
 static int percent(int done,int total,long copied,long expected){return basisPoints(done,total,copied,expected)/100;}
 private DownloadProgress(){}
}
