package dk.stream.android;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
public final class LibraryCrypto {
 private LibraryCrypto() { }
 public static byte[] encrypt(byte[] plain,SecretKey key) throws GeneralSecurityException {
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key);
  byte[] iv=cipher.getIV();byte[] encrypted=cipher.doFinal(plain);
  byte[] result=new byte[1+iv.length+encrypted.length];result[0]=(byte)iv.length;
  System.arraycopy(iv,0,result,1,iv.length);System.arraycopy(encrypted,0,result,1+iv.length,encrypted.length);return result;
 }
 public static byte[] decrypt(byte[] payload,SecretKey key) throws GeneralSecurityException {
  if(payload.length<29 || payload[0]!=12) throw new GeneralSecurityException("Invalid encrypted library");
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Arrays.copyOfRange(payload,1,13)));
  return cipher.doFinal(payload,13,payload.length-13);
 }
}
