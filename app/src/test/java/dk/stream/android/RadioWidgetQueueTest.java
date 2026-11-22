package dk.stream.android;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import org.json.*;
public class RadioWidgetQueueTest {
 private RadioStation station(String id){return new RadioStation(id,id,"https://example.com/"+id,"");}
 private List<RadioStation> stations(){return Arrays.asList(station("a"),station("b"),station("c"));}
 @Test public void onlyChosenStationsAppearInSavedOrder(){List<RadioStation> list=RadioWidgetQueue.available(stations(),Arrays.asList("c","a"));assertEquals("c",list.get(0).id);assertEquals("a",list.get(1).id);assertEquals(2,list.size());}
 @Test public void forwardAndBackWrapAmongTheChosenSubset(){List<RadioStation> list=RadioWidgetQueue.available(stations(),Arrays.asList("a","c"));assertEquals("c",RadioWidgetQueue.adjacent(list,"a",1).id);assertEquals("a",RadioWidgetQueue.adjacent(list,"c",1).id);assertEquals("c",RadioWidgetQueue.adjacent(list,"a",-1).id);}
 @Test public void deletedFavoritesAreSkippedWithoutAddingUnchosenStations(){List<RadioStation> list=RadioWidgetQueue.available(Arrays.asList(station("b"),station("c")),Arrays.asList("a","c"));assertEquals(1,list.size());assertEquals("c",RadioWidgetQueue.current(list,"a").id);}
 @Test public void oneStationAndAnEmptyListAreHandled(){assertEquals("a",RadioWidgetQueue.adjacent(Collections.singletonList(station("a")),"a",1).id);assertNull(RadioWidgetQueue.current(Collections.emptyList(),"a"));assertNull(RadioWidgetQueue.adjacent(Collections.emptyList(),"a",-1));}
 @Test public void separateWidgetsKeepDifferentSubsetsAndSelections(){List<RadioStation> first=RadioWidgetQueue.available(stations(),Arrays.asList("a","b"));List<RadioStation> second=RadioWidgetQueue.available(stations(),Arrays.asList("b","c"));assertEquals("a",RadioWidgetQueue.adjacent(first,"b",1).id);assertEquals("c",RadioWidgetQueue.adjacent(second,"b",1).id);assertEquals("b",RadioWidgetQueue.current(second,"b").id);}
 @Test public void selectionSurvivesBackupWithSingleStationWidgets()throws Exception{Map<String,Map<String,Object>> state=BackupPreferences.empty();state.get("radio_widgets").put("station_1","a");state.get("radio_widgets").put("station_2","c");state.get("radio_widgets").put("queue_2",RadioWidgetQueue.json(Arrays.asList("c","a")));Map<String,Object> restored=BackupPreferences.decode(BackupPreferences.encode(state)).get("radio_widgets");assertEquals("a",restored.get("station_1"));assertEquals(Arrays.asList("c","a"),RadioWidgetQueue.read((String)restored.get("queue_2")));}
 @Test(expected=JSONException.class) public void duplicatesCannotEnterAWidgetSelection()throws Exception{RadioWidgetQueue.read("[\"a\",\"a\"]");}
 @Test(expected=JSONException.class) public void emptySelectionsAreRejected()throws Exception{RadioWidgetQueue.read("[]");}
 @Test(expected=JSONException.class) public void malformedSelectionsAreRejectedOnImport()throws Exception{Map<String,Map<String,Object>> state=BackupPreferences.empty();state.get("radio_widgets").put("queue_2","not-json");BackupPreferences.encode(state);}
}
