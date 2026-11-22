package dk.stream.android;
import org.json.*;

/** A single most-recent selection across radio, phone files and car files. */
final class LastAudioSelection {
 final RadioStation radio;final LocalMedia file;
 private LastAudioSelection(RadioStation radio,LocalMedia file){this.radio=radio;this.file=file;}
 static LastAudioSelection radio(RadioStation station){if(station==null)throw new IllegalArgumentException();Playlist.httpUrl(station.url);return new LastAudioSelection(station,null);}
 static LastAudioSelection file(LocalMedia file){if(file==null||file.folder||!file.audio())throw new IllegalArgumentException("Not audio");return new LastAudioSelection(null,file);}
 String key(){return radio!=null?"radio:"+radio.id:"file:"+file.uri;}
 String json()throws JSONException{return radio!=null?new JSONObject().put("radio",radio.toJson()).toString():new JSONObject().put("file",file.json()).toString();}
 static LastAudioSelection read(String json)throws JSONException{JSONObject data=new JSONObject(json);if(data.has("radio")==data.has("file"))throw new JSONException("Invalid audio selection");try{return data.has("radio")?radio(RadioStation.fromJson(data.getJSONObject("radio"))):file(LocalMedia.from(data.getJSONObject("file")));}catch(IllegalArgumentException invalid){throw new JSONException("Invalid audio selection");}}
}
