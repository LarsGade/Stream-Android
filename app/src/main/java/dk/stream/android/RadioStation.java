package dk.stream.android;

import org.json.*;

/** Stable Radio Browser identity; a local alias never changes the stream or favorite identity. */
public final class RadioStation {
 public final String id,name,url,logo,alias;
 public final boolean hls;
 public RadioStation(String id,String name,String url,String logo) {this(id,name,url,logo,"");}
 public RadioStation(String id,String name,String url,String logo,String alias) {this(id,name,url,logo,alias,false);}
 public RadioStation(String id,String name,String url,String logo,String alias,boolean hls) {this.hls=hls;this.id=id;this.name=name;this.url=url;this.logo=logo;this.alias=alias;}
 public String displayName() {return alias.isEmpty()?name:alias;}
 public RadioStation renamed(String value) {if(value.trim().isEmpty()) throw new IllegalArgumentException("Name required");return new RadioStation(id,name,url,logo,value.trim(),hls);}
 public JSONObject toJson() throws JSONException {return new JSONObject().put("id",id).put("name",name).put("url",url).put("logo",logo).put("alias",alias).put("hls",hls);}
 public static RadioStation fromJson(JSONObject value) throws JSONException {return new RadioStation(value.getString("id"),value.getString("name"),value.getString("url"),value.optString("logo",""),value.optString("alias",""),value.optBoolean("hls"));}
 @Override public String toString() {return displayName();}
}
