package dk.stream.android;
import java.io.*;
import okhttp3.*;
/** Read music directly from its HTTP source without a document-provider descriptor. */
final class CastMusicStream {
 static InputStream open(OkHttpClient client,HttpUrl url,long offset)throws IOException{
  Request.Builder request=new Request.Builder().url(url).header("Accept-Encoding","identity");
  if(offset>0)request.header("Range","bytes="+offset+"-");
  Response response=client.newCall(request.build()).execute();
  if(!response.isSuccessful()||response.body()==null){int code=response.code();response.close();throw new IOException("Music HTTP "+code);}
  InputStream input=response.body().byteStream();
  try{if(response.code()==206)DownloadRange.validate(response.header("Content-Range"),offset);
   if(offset>0&&response.code()==200){long remaining=offset;while(remaining>0){long skipped=input.skip(remaining);if(skipped>0)remaining-=skipped;else if(input.read()<0)throw new EOFException();else remaining--;}}
  }catch(IOException failure){response.close();throw failure;}
  return new FilterInputStream(input){@Override public void close()throws IOException{try{super.close();}finally{response.close();}}};
 }
 private CastMusicStream(){}
}
