package dk.stream.android;

import java.nio.charset.StandardCharsets;
import java.util.*;
import okhttp3.HttpUrl;
import org.json.*;

/** Saved only inside LibraryStore's encrypted payload. */
public final class PcloudAccount {
 public static final String EU="https://ewebdav.pcloud.com/",US="https://webdav.pcloud.com/";
 public final String server,email,password,id;
 public PcloudAccount(String server,String email,String password) {
  HttpUrl url=HttpUrl.parse(server);
  if(url==null||!url.isHttps()||url.port()!=443||!Arrays.asList("ewebdav.pcloud.com","webdav.pcloud.com").contains(url.host())||!url.username().isEmpty()||!url.password().isEmpty()||url.query()!=null||url.fragment()!=null||!url.encodedPath().equals("/"))throw new IllegalArgumentException("Vælg pCloud-region EU eller USA.");
  if(email==null||email.trim().isEmpty()||password==null||password.isEmpty())throw new IllegalArgumentException("Angiv e-mail og adgangskode.");
  this.server=url.toString();this.email=email.trim();this.password=password;
  id=UUID.nameUUIDFromBytes((this.server+"\n"+this.email.toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8)).toString();
 }
 public JSONObject json() throws JSONException{return new JSONObject().put("server",server).put("email",email).put("password",password);}
 public static PcloudAccount from(JSONObject value) throws JSONException{return new PcloudAccount(value.getString("server"),value.getString("email"),value.getString("password"));}
}
