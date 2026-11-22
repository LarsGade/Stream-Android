package dk.stream.android;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.xmlpull.v1.*;

/** Read-only WebDAV. Credentials are scoped to the configured origin. */
public final class WebDavClient {
 public static final class Entry {
  public final HttpUrl url;public final String name,mime;public final boolean folder;public final long size;
  Entry(HttpUrl url,String name,String mime,boolean folder,long size){this.url=url;this.name=name;this.mime=mime;this.folder=folder;this.size=size;}
  public LocalMedia media(PcloudAccount account){return new LocalMedia(PcloudDocument.uri(account,url,size),name,mime,folder);}
 }
 private final HttpUrl base;
 private final OkHttpClient client;
 public WebDavClient(PcloudAccount account){this(HttpUrl.get(account.server),account.email,account.password);}
 // Package-private constructor permits deterministic tests on a local HTTP server.
 WebDavClient(HttpUrl base,String email,String password){
  this.base=base;String authorization=Credentials.basic(email,password,java.nio.charset.StandardCharsets.UTF_8);
  client=new OkHttpClient.Builder().connectTimeout(20,TimeUnit.SECONDS).readTimeout(45,TimeUnit.SECONDS).followSslRedirects(false).addNetworkInterceptor(chain->{
   Request request=chain.request();if(!request.url().scheme().equals(base.scheme()))throw new IOException("Usikker omdirigering afvist.");
   Request.Builder scoped=request.newBuilder().removeHeader("Authorization");if(PcloudDocument.sameOrigin(base,request.url()))scoped.header("Authorization",authorization);return chain.proceed(scoped.build());
  }).build();
 }
 public OkHttpClient streamClient(){return client;}
 public void cancel(){client.dispatcher().cancelAll();}
 public List<Entry> list(HttpUrl folder)throws IOException{return list(folder,false);}
 public List<Entry> listAll(HttpUrl folder)throws IOException{return list(folder,true);}
 private List<Entry> list(HttpUrl folder,boolean includeOther) throws IOException {
  PcloudDocument.checkOrigin(base,folder);if(!folder.encodedPath().endsWith("/"))folder=folder.newBuilder().addPathSegment("").build();
  RequestBody body=RequestBody.create("<?xml version=\"1.0\" encoding=\"utf-8\"?><d:propfind xmlns:d=\"DAV:\"><d:prop><d:displayname/><d:resourcetype/><d:getcontenttype/><d:getcontentlength/></d:prop></d:propfind>",MediaType.get("application/xml; charset=utf-8"));
  OkHttpClient listing=client.newBuilder().followRedirects(false).callTimeout(60,TimeUnit.SECONDS).build();HttpUrl target=folder;
  for(int redirects=0;redirects<5;redirects++){
   try(Response response=listing.newCall(new Request.Builder().url(target).header("Depth","1").method("PROPFIND",body).build()).execute()){
    if(Arrays.asList(301,302,307,308).contains(response.code())){String location=response.header("Location");HttpUrl next=location==null?null:target.resolve(location);if(next==null)throw new IOException("Ugyldig WebDAV-omdirigering.");try{PcloudDocument.checkOrigin(base,next);}catch(IllegalArgumentException denied){throw new IOException("WebDAV-omdirigering til en anden server er afvist.");}target=next;continue;}
    if(response.code()!=207&&response.code()!=200)throw failure(response.code());if(response.body()==null)throw new IOException("Tomt WebDAV-svar.");return parse(target,response.body().byteStream(),includeOther);
   }
  }
  throw new IOException("For mange WebDAV-omdirigeringer.");
 }
 static IOException failure(int code){return new IOException(code==401||code==403?"Pcloud afviste login. Kontrollér region, e-mail og adgangskode. Ved 2FA kan forbindelsen kræve godkendelse i en mail fra pCloud.":code==404?"Pcloud-mappen eller filen findes ikke.":"Pcloud svarede med fejl "+code+".");}
 public interface CallStarted {void started(Call call);}
 public InputStream open(HttpUrl url,long offset)throws IOException{return open(url,offset,null);}
 public InputStream open(HttpUrl url,long offset,CallStarted started) throws IOException {
  PcloudDocument.checkOrigin(base,url);Request.Builder request=new Request.Builder().url(url);if(offset>0)request.header("Range","bytes="+offset+"-");Call call=client.newCall(request.build());if(started!=null)started.started(call);Response response=call.execute();
  if(!response.isSuccessful()||response.body()==null){int code=response.code();response.close();throw failure(code);}
  InputStream input=response.body().byteStream();
  try{if(response.code()==206)DownloadRange.validate(response.header("Content-Range"),offset);if(offset>0&&response.code()==200){long remaining=offset;while(remaining>0){long skipped=input.skip(remaining);if(skipped>0)remaining-=skipped;else if(input.read()<0)throw new EOFException();else remaining--;}}}
  catch(IOException failure){response.close();throw failure;}
  return new FilterInputStream(input){@Override public void close() throws IOException {try{super.close();}finally{response.close();}}};
 }
 static List<Entry> parse(HttpUrl folder,InputStream input)throws IOException{return parse(folder,input,false);}
 static List<Entry> parse(HttpUrl folder,InputStream input,boolean includeOther) throws IOException {
  try{
   XmlPullParserFactory factory=XmlPullParserFactory.newInstance();factory.setNamespaceAware(true);XmlPullParser xml=factory.newPullParser();try{xml.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL,false);}catch(XmlPullParserException unsupported){if(xml.getFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL))throw new IOException("XML-parseren kan ikke blokere DTD.");}xml.setInput(input,null);
   List<Entry> entries=new ArrayList<>();Set<String> seen=new HashSet<>();String href=null,name="",mime="",status="";boolean directory=false,inResponse=false,inPropstat=false,propsDirectory=false,successful=false,anySuccess=false;String propsName="",propsMime="";long size=-1,propsSize=-1;String field=null;StringBuilder text=new StringBuilder();int depth=0;
   for(int event=xml.nextToken();event!=XmlPullParser.END_DOCUMENT;event=xml.nextToken()){
    if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
    if(event==XmlPullParser.DOCDECL)throw new IOException("WebDAV XML med DTD afvises.");
    if(event==XmlPullParser.START_TAG){if(++depth>64)throw new IOException("For dybt XML-svar.");String tag=xml.getName();if(depth==1&&!("DAV:".equals(xml.getNamespace())&&tag.equals("multistatus")))throw new IOException("Pcloud returnerede ikke en WebDAV-mappeliste.");if("DAV:".equals(xml.getNamespace())){
     if(tag.equals("response")){inResponse=true;href=null;name="";mime="";size=-1;directory=false;successful=false;}
     else if(inResponse&&tag.equals("propstat")){inPropstat=true;propsName="";propsMime="";propsSize=-1;propsDirectory=false;status="";}
     else if(inPropstat&&tag.equals("collection"))propsDirectory=true;
     if(inResponse&&Arrays.asList("href","displayname","getcontenttype","getcontentlength","status").contains(tag)){field=tag;text.setLength(0);}
    }}else if(event==XmlPullParser.TEXT||event==XmlPullParser.CDSECT||event==XmlPullParser.ENTITY_REF){if(field!=null&&xml.getText()!=null)text.append(xml.getText());}
    else if(event==XmlPullParser.END_TAG){String tag=xml.getName();if("DAV:".equals(xml.getNamespace())){
     if(tag.equals(field)){String value=text.toString();if(tag.equals("href"))href=value.trim();else if(tag.equals("status"))status=value;else if(inPropstat&&tag.equals("displayname"))propsName=value;else if(inPropstat&&tag.equals("getcontenttype"))propsMime=value.trim();else if(inPropstat&&tag.equals("getcontentlength"))try{propsSize=Long.parseLong(value.trim());}catch(NumberFormatException ignored){}field=null;}
     if(tag.equals("propstat")){if(status.trim().matches(".*\\s2[0-9][0-9](?:\\s.*)?")){successful=true;anySuccess=true;if(!propsName.isEmpty())name=propsName;if(!propsMime.isEmpty())mime=propsMime;if(propsSize>=0)size=propsSize;directory|=propsDirectory;}inPropstat=false;}
     else if(tag.equals("response")){inResponse=false;if(successful&&href!=null){HttpUrl url=folder.resolve(href);if(url!=null&&PcloudDocument.sameOrigin(folder,url)&&url.username().isEmpty()&&url.password().isEmpty()&&url.query()==null&&url.fragment()==null){String parent=folder.encodedPath();if(!parent.endsWith("/"))parent+="/";String path=url.encodedPath();String trimmed=path.endsWith("/")?path.substring(0,path.length()-1):path;if(path.startsWith(parent)&&!trimmed.equals(parent.substring(0,parent.length()-1))){String rest=trimmed.substring(parent.length());if(!rest.isEmpty()&&!rest.contains("/")&&seen.add(url.toString())){if(name.isEmpty()){List<String> parts=url.pathSegments();name=parts.get(parts.size()-(directory&&path.endsWith("/")?2:1));}LocalMedia classification=new LocalMedia("content://classification/file",name,mime,directory);if(includeOther||classification.supported())entries.add(new Entry(url,name,mime,directory,size));}}}}}
    }depth--;}
   }
   if(!anySuccess)throw new IOException("Pcloud gav ikke adgang til mappen.");
   Collections.sort(entries,(a,b)->a.folder!=b.folder?(a.folder?-1:1):NaturalNameOrder.compare(a.name,b.name));return entries;
  }catch(XmlPullParserException|IndexOutOfBoundsException error){throw new IOException("Pcloud returnerede et ugyldigt WebDAV-svar.",error);}
 }
}
