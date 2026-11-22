package dk.stream.android;

/** Keep unchanged cover bytes, and retain a cover through transient missing metadata. */
final class PlaybackCoverState {
 private String media;private Object model;private int fallback;
 boolean update(String media,Object candidate,int fallback){
  if(candidate instanceof String&&((String)candidate).isEmpty()||candidate instanceof byte[]&&((byte[])candidate).length==0||candidate instanceof Integer)candidate=null;
  boolean sameMedia=java.util.Objects.equals(this.media,media);
  if(sameMedia&&candidate==null&&model!=null)candidate=model;
  if(sameMedia&&this.fallback==fallback&&java.util.Objects.deepEquals(model,candidate))return false;
  this.media=media;this.fallback=fallback;model=candidate instanceof byte[]?((byte[])candidate).clone():candidate;return true;
 }
 Object model(){return model;}
 void clear(){media=null;model=null;fallback=0;}
}
