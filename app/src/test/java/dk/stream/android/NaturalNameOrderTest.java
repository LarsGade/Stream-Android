package dk.stream.android;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class NaturalNameOrderTest {
 private List<String> sorted(String... input){List<String> names=new ArrayList<>(Arrays.asList(input));names.sort(NaturalNameOrder::compare);return names;}
 @Test public void numbersSortNumerically(){assertEquals(Arrays.asList("Fil 1","Fil 2","Fil 9","Fil 10","Fil 100"),sorted("Fil 10","Fil 9","Fil 100","Fil 2","Fil 1"));}
 @Test public void everyNumericPartCounts(){assertEquals(Arrays.asList("S2 E2","S2 E10","S10 E1"),sorted("S10 E1","S2 E10","S2 E2"));}
 @Test public void equalNumbersUseSuffixBeforeZeroPadding(){assertEquals(Arrays.asList("Fil 1a","Fil 01b","Fil 2","Fil 02"),sorted("Fil 02","Fil 01b","Fil 2","Fil 1a"));}
 @Test public void handlesNumbersLargerThanLong(){String large=new String(new char[80]).replace('\0','9');assertTrue(NaturalNameOrder.compare("File "+large,"File 1"+large)<0);assertTrue(NaturalNameOrder.compare("File 9223372036854775808","File 9223372036854775809")<0);}
 @Test public void sortsDanishLettersAndIgnoresCaseForNaturalNumbers(){assertEquals(Arrays.asList("fil 2","FIL 10"),sorted("FIL 10","fil 2"));assertEquals(Arrays.asList("Zebra","Æble","Øvelse","År"),sorted("Øvelse","År","Zebra","Æble"));}
 @Test public void foldersComeFirstAndEachTypeUsesNaturalOrder(){LocalMedia f10=new LocalMedia("content://test/f10","Mappe 10","",true),f2=new LocalMedia("content://test/f2","Mappe 2","",true),a10=new LocalMedia("content://test/a10","A10.mp3","audio/mpeg",false),a2=new LocalMedia("content://test/a2","A2.mp3","audio/mpeg",false);List<LocalMedia> entries=new ArrayList<>(Arrays.asList(a10,f10,a2,f2));entries.sort(NaturalNameOrder.MEDIA);assertEquals(Arrays.asList(f2,f10,a2,a10),entries);}
 @Test public void comparatorIsConsistentForMixedNames(){String[] names={"","1","01","000","0","A1","a01","Å2","Å10","fil2a","fil02b","fil10","-2","-10"," file2"};for(String a:names)for(String b:names){assertEquals(-Integer.signum(NaturalNameOrder.compare(b,a)),Integer.signum(NaturalNameOrder.compare(a,b)));for(String c:names)if(NaturalNameOrder.compare(a,b)<=0&&NaturalNameOrder.compare(b,c)<=0)assertTrue(a+","+b+","+c,NaturalNameOrder.compare(a,c)<=0);}}
}
