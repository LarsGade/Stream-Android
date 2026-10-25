package dk.stream.android;
import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public final class LibraryStore {
 private static final String ALIAS="StreamLibrary_v1";
 private final SharedPreferences preferences;
 public LibraryStore(Context context) { preferences=context.getSharedPreferences("stream_library",Context.MODE_PRIVATE); }
 private SecretKey key() throws Exception {
  KeyStore keys=KeyStore.getInstance("AndroidKeyStore");keys.load(null);
  if(keys.containsAlias(ALIAS)) return (SecretKey)keys.getKey(ALIAS,null);
  KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
  generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
   .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
  return generator.generateKey();
 }
 public Library load() throws Exception {
  String payload=preferences.getString("encrypted_library",null);if(payload==null) return new Library();
  byte[] plain=LibraryCrypto.decrypt(Base64.decode(payload,Base64.NO_WRAP),key());
  return Library.fromJson(new String(plain,StandardCharsets.UTF_8));
 }
 public void save(Library library) throws Exception {
  String encoded=Base64.encodeToString(LibraryCrypto.encrypt(library.toJson().getBytes(StandardCharsets.UTF_8),key()),Base64.NO_WRAP);
  if(!preferences.edit().putString("encrypted_library",encoded).commit()) throw new IOException("Unable to save library");
 }
}
