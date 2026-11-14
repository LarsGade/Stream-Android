package dk.stream.android;

import android.content.Context;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** Linked document roots, folder navigation and independent local favorites. */
@android.annotation.SuppressLint("ViewConstructor")
@androidx.media3.common.util.UnstableApi
public final class LocalFilesPanel extends LinearLayout {
 public interface Actions {Library library();boolean save(Runnable change);void pickFiles();void pickFolder();void play(LocalMedia entry);default boolean playing(LocalMedia entry) {return LocalAudioService.playing(entry.uri);}default void changeFolder(LocalMedia entry) {pickFolder();}default void navigationChanged(boolean inside) {}}
 private final Actions actions;
 private final EditText search;
 private final ArrayList<LocalMedia> entries=new ArrayList<>();
 private final Button changeFolder;
 private final ImageButton up;
 private final TextView path,message;
 private final ListView list;
 private final ArrayList<LocalMedia> visible=new ArrayList<>();
 private final ArrayDeque<LocalMedia> parents=new ArrayDeque<>();
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final Adapter adapter=new Adapter();
 private boolean favorites,closed;
 private int generation;
 private Future<?> folderTask;
 public LocalFilesPanel(Context context,Actions actions) {
  super(context);this.actions=actions;setOrientation(VERTICAL);
  search=new EditText(context);search.setSingleLine();search.setHint(R.string.search_hint);addView(search);
  search.addTextChangedListener(new android.text.TextWatcher() {public void beforeTextChanged(CharSequence value,int start,int count,int after) {}public void onTextChanged(CharSequence value,int start,int before,int count) {filter();}public void afterTextChanged(android.text.Editable value) {}});
  changeFolder=new Button(context);changeFolder.setText("Vælg kildemappe");addView(changeFolder);changeFolder.setOnClickListener(v->{LocalMedia root=parents.peekFirst();if(root!=null) actions.changeFolder(root);else actions.pickFolder();});
  up=new ImageButton(context);up.setImageResource(R.drawable.ic_back);up.setContentDescription("Tilbage");up.setBackgroundColor(Color.TRANSPARENT);int backSize=Math.round(48*getResources().getDisplayMetrics().density);LayoutParams backParams=new LayoutParams(backSize,backSize);backParams.gravity=Gravity.LEFT;addView(up,backParams);up.setOnClickListener(v->back());
  path=new TextView(context);path.setTextColor(Color.WHITE);path.setTextSize(18);addView(path);
  message=new TextView(context);message.setTextColor(Color.LTGRAY);message.setVisibility(GONE);addView(message);
  list=new ListView(context);addView(list,new LayoutParams(-1,0,1));list.setAdapter(adapter);
  list.setOnItemClickListener((p,v,position,id)->{LocalMedia entry=visible.get(position);if(entry.folder) {parents.addLast(entry);loadFolder();}else actions.play(entry);});
  list.setOnItemLongClickListener((p,v,position,id)->{LocalMedia entry=visible.get(position);PopupMenu menu=new PopupMenu(context,v);
   menu.getMenu().add(actions.library().isLocalFavorite(entry.uri)?"Fjern fra favoritter":"Tilføj til favoritter").setOnMenuItemClickListener(item->{actions.save(()->actions.library().toggleLocalFavorite(entry));reload();return true;});
   if(parents.isEmpty()&&!favorites&&entry.folder) menu.getMenu().add("Skift kildemappe").setOnMenuItemClickListener(item->{actions.changeFolder(entry);return true;});
   if(parents.isEmpty()&&!favorites) menu.getMenu().add("Fjern link").setOnMenuItemClickListener(item->{actions.save(()->actions.library().removeLocal(entry.uri));reload();return true;});menu.show();return true;});
 }
 public void open(boolean favorites) {this.favorites=favorites;parents.clear();search.setText("");if(!favorites) for(LocalMedia entry:actions.library().localFiles) if(entry.folder) {parents.add(entry);break;}reload();}
 public boolean back() {if(parents.isEmpty()) return false;parents.removeLast();reload();return true;}
 public List<LocalMedia> playbackEntries() {return new ArrayList<>(visible);}
 public void playbackChanged() {adapter.notifyDataSetChanged();}
 public void sourceChanged() {open(false);}
 public void reload() {changeFolder.setVisibility(favorites?GONE:VISIBLE);actions.navigationChanged(!parents.isEmpty());up.setVisibility(parents.isEmpty()?GONE:VISIBLE);if(parents.isEmpty()) {if(folderTask!=null) folderTask.cancel(true);generation++;path.setText(favorites?"":"Lokale");List<LocalMedia> roots=new ArrayList<>();if(!favorites) for(LocalMedia entry:actions.library().localFiles) if(entry.folder){roots.add(entry);break;}show(favorites?actions.library().localFavorites:roots);showMessage("");}else loadFolder();}
 private void showMessage(String text) {message.setText(text);message.setVisibility(text.isEmpty()?GONE:VISIBLE);}
 private void show(List<LocalMedia> result) {entries.clear();entries.addAll(result);filter();}
 private void filter() {visible.clear();String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);for(LocalMedia entry:entries) if(entry.displayName().toLowerCase(Locale.ROOT).contains(query)) visible.add(entry);adapter.notifyDataSetChanged();}
 private void loadFolder() {
  changeFolder.setVisibility(favorites?GONE:VISIBLE);actions.navigationChanged(true);  int request=++generation;LocalMedia folder=parents.peekLast();path.setText(folder.name);up.setVisibility(VISIBLE);showMessage("");show(Collections.emptyList());
  if(folderTask!=null) folderTask.cancel(true);folderTask=worker.submit(()->{try {List<LocalMedia> entries=LocalMedia.contents(getContext(),folder);post(()->{if(closed||generation!=request)return;show(entries);showMessage(entries.isEmpty()?"Ingen lyd- eller videofiler i mappen.":"");});}
   catch(Exception failure) {post(()->{if(!closed&&generation==request) showMessage("Kan ikke åbne mappen. Tilføj den igen, hvis den er flyttet, slettet eller adgangen er fjernet.");});}});
 }
 public void close() {closed=true;if(folderTask!=null) folderTask.cancel(true);generation++;worker.shutdownNow();}
 private final class Adapter extends BaseAdapter {
  public int getCount(){return visible.size();}public LocalMedia getItem(int p){return visible.get(p);}public long getItemId(int p){return p;}
  public View getView(int p,View recycled,ViewGroup parent) {
   LocalMedia entry=getItem(p);LinearLayout row=new LinearLayout(getContext());row.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);row.setGravity(Gravity.CENTER_VERTICAL);int pad=Math.round(8*getResources().getDisplayMetrics().density);row.setPadding(pad,pad,pad,pad);
   ImageView icon=new ImageView(getContext());icon.setImageResource(entry.folder?R.drawable.ic_folder:(actions.playing(entry)||CastPlayback.playingLocal(getContext(),entry.uri))?R.drawable.ic_pause:R.drawable.ic_play);icon.setContentDescription(entry.folder?"Åbn mappe":(actions.playing(entry)||CastPlayback.playingLocal(getContext(),entry.uri))?"Pause":"Afspil");row.addView(icon,new LayoutParams(pad*5,pad*5));
   TextView name=new TextView(getContext());name.setText(entry.displayName());name.setTextColor(Color.WHITE);name.setTextSize(16);name.setPadding(pad,0,pad,0);row.addView(name,new LayoutParams(0,-2,1));
   if(!favorites) {ImageButton star=new ImageButton(getContext());boolean favorite=actions.library().isLocalFavorite(entry.uri);star.setFocusable(false);star.setImageResource(favorite?R.drawable.ic_favorite:R.drawable.ic_favorite_outline);star.setBackgroundColor(Color.TRANSPARENT);star.setContentDescription((favorite?"Fjern fra favoritter: ":"Tilføj til favoritter: ")+entry.name);row.addView(star,new LayoutParams(pad*6,pad*6));star.setOnClickListener(v->{actions.save(()->actions.library().toggleLocalFavorite(entry));reload();});}return row;
  }
 }
}
