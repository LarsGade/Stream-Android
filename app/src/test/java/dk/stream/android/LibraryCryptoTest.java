package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
public class LibraryCryptoTest {
 private SecretKey key() throws Exception {KeyGenerator generator=KeyGenerator.getInstance("AES");generator.init(256);return generator.generateKey();}
 @Test public void encryptsCredentialsAndDecryptsAfterReload() throws Exception {
  SecretKey key=key();byte[] plain="password=secret&username=account".getBytes(StandardCharsets.UTF_8);
  byte[] encrypted=LibraryCrypto.encrypt(plain,key);assertArrayEquals(plain,LibraryCrypto.decrypt(encrypted,key));
  assertFalse(new String(encrypted,StandardCharsets.ISO_8859_1).contains("secret"));
  assertFalse(Arrays.equals(encrypted,LibraryCrypto.encrypt(plain,key)));
 }
 @Test(expected=java.security.GeneralSecurityException.class) public void rejectsModifiedCiphertext() throws Exception {
  SecretKey key=key();byte[] encrypted=LibraryCrypto.encrypt("secret".getBytes(StandardCharsets.UTF_8),key);encrypted[encrypted.length-1]^=1;LibraryCrypto.decrypt(encrypted,key);
 }
 @Test(expected=java.security.GeneralSecurityException.class) public void rejectsDifferentKey() throws Exception {
  byte[] encrypted=LibraryCrypto.encrypt("secret".getBytes(StandardCharsets.UTF_8),key());LibraryCrypto.decrypt(encrypted,key());
 }
 @Test(expected=java.security.GeneralSecurityException.class) public void rejectsCorruptPayload() throws Exception {LibraryCrypto.decrypt(new byte[5],key());}
}
