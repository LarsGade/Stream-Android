package dk.stream.android;

import java.text.Collator;
import java.util.*;

/** Case-insensitive Danish name order with arbitrary-length numeric runs. */
public final class NaturalNameOrder {
 private static final ThreadLocal<Collator> collators=new ThreadLocal<Collator>(){protected Collator initialValue(){Collator value=Collator.getInstance(new Locale("da","DK"));value.setStrength(Collator.SECONDARY);return value;}};
 public static final Comparator<LocalMedia> MEDIA=(a,b)->a.folder!=b.folder?(a.folder?-1:1):compare(a.name,b.name);
 private static int digit(char value){return Character.digit(value,10);}
 public static int compare(String a,String b){
  int i=0,j=0,padding=0;Collator collator=collators.get();
  while(i<a.length()&&j<b.length()){
   if(digit(a.charAt(i))>=0&&digit(b.charAt(j))>=0){
    int endA=i,endB=j;while(endA<a.length()&&digit(a.charAt(endA))>=0)endA++;while(endB<b.length()&&digit(b.charAt(endB))>=0)endB++;
    int firstA=i,firstB=j;while(firstA<endA&&digit(a.charAt(firstA))==0)firstA++;while(firstB<endB&&digit(b.charAt(firstB))==0)firstB++;
    int lengthA=endA-firstA,lengthB=endB-firstB;if(lengthA!=lengthB)return Integer.compare(lengthA,lengthB);
    for(int k=0;k<lengthA;k++){int difference=digit(a.charAt(firstA+k))-digit(b.charAt(firstB+k));if(difference!=0)return difference;}
    if(padding==0)padding=Integer.compare(endA-i,endB-j);i=endA;j=endB;
   }else{
    int endA=i,endB=j;while(endA<a.length()&&digit(a.charAt(endA))<0)endA++;while(endB<b.length()&&digit(b.charAt(endB))<0)endB++;
    int difference=collator.compare(a.substring(i,endA).toLowerCase(new Locale("da","DK")),b.substring(j,endB).toLowerCase(new Locale("da","DK")));if(difference!=0)return difference;i=endA;j=endB;
   }
  }
  if(i<a.length()||j<b.length())return i<a.length()?1:-1;
  return padding!=0?padding:a.compareTo(b);
 }
 private NaturalNameOrder(){}
}
