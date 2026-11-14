package dk.stream.android;

import android.content.Context;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** Linked document roots, folder navigation and independent local favorites. */
@android.annotation.SuppressLint("ViewConstructor")
public final class LocalFilesPanel extends LinearLayout {
 public interface Actions {Library library();boolean save(Runnable change);void pickFiles();void pickFolder();void play(LocalMedia entry);default void changeFolder(LocalMedia entry) {pickFolder();}default void navigationChanged(boolean inside) {}}
 private final Actions actions;
 private final LinearLayout imports;
 private final Button up,changeFolder;
 private final CheckBox recursive;
 private final TextView path,message;
 private final ListView list;
 private final ArrayList<LocalMedia> visible=new ArrayList<>();
 private final ArrayDeque<LocalMedia> parents=new ArrayDeque<>();
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final Adapter adapter=new Adapter();
 private boolean favorites,closed;
 private int generation;
 public LocalFilesPanel(Context context,Actions actions) {
  super(context);this.actions=actions;setOrientation(VERTICAL);
  imports=new LinearLayout(context);addView(imports);
  Button files=new Button(context);files.setText("Tilføj filer");imports.addView(files,new LayoutParams(0,-2,1));files.setOnClickListener(v->actions.pickFiles());
  Button folder=new Button(context);folder.setText("Tilføj mappe");imports.addView(folder,new LayoutParams(0,-2,1));folder.setOnClickListener(v->actions.pickFolder());
  changeFolder=new Button(context);changeFolder.setText("Skift kildemappe");addView(changeFolder);changeFolder.setOnClickListener(v->{LocalMedia root=parents.peekFirst();if(root!=null) actions.changeFolder(root);});
  recursive=new CheckBox(context);recursive.setText("Medtag filer fra alle undermapper");recursive.setTextColor(Color.WHITE);recursive.setChecked(true);addView(recursive);recursive.setOnCheckedChangeListener((button,checked)->{if(!parents.isEmpty()) loadFolder();});
  up=new Button(context);up.setText("Tilbage til overordnet mappe");addView(up);up.setOnClickListener(v->back());
  path=new TextView(context);path.setTextColor(Color.WHITE);path.setTextSize(18);addView(path);
  message=new TextView(context);message.setTextColor(Color.LTGRAY);addView(message);
  list=new ListView(context);addView(list,new LayoutParams(-1,0,1));list.setAdapter(adapter);
  list.setOnItemClickListener((p,v,position,id)->{LocalMedia entry=visible.get(position);if(entry.folder) {parents.addLast(entry);loadFolder();}else actions.play(entry);});
  list.setOnItemLongClickListener((p,v,position,id)->{LocalMedia entry=visible.get(position);PopupMenu menu=new PopupMenu(context,v);
   menu.getMenu().add(actions.library().isLocalFavorite(entry.uri)?"Fjern fra favoritter":"Tilføj til favoritter").setOnMenuItemClickListener(item->{actions.save(()->actions.library().toggleLocalFavorite(entry));reload();return true;});
   if(parents.isEmpty()&&!favorites&&entry.folder) menu.getMenu().add("Skift kildemappe").setOnMenuItemClickListener(item->{actions.changeFolder(entry);return true;});
   if(parents.isEmpty()&&!favorites) menu.getMenu().add("Fjern link").setOnMenuItemClickListener(item->{actions.save(()->actions.library().removeLocal(entry.uri));reload();return true;});menu.show();return true;});
 }
 public void open(boolean favorites) {this.favorites=favorites;parents.clear();reload();}
 public boolean back() {if(parents.isEmpty()) return false;parents.removeLast();reload();return true;}
 public void sourceChanged() {parents.clear();reload();}
 public void reload() {changeFolder.setVisibility(!favorites&&!parents.isEmpty()?VISIBLE:GONE);recursive.setVisibility(parents.isEmpty()?GONE:VISIBLE);actions.navigationChanged(!parents.isEmpty());imports.setVisibility(favorites?GONE:VISIBLE);up.setVisibility(parents.isEmpty()?GONE:VISIBLE);if(parents.isEmpty()) {generation++;path.setText(favorites?"":"Lokale filer");show(favorites?actions.library().localFavorites:actions.library().localFiles);message.setText(visible.isEmpty()?(favorites?"Tilføj filer eller mapper til favoritter med stjernen.":"Tilføj lyd, video eller mapper fra enheden."):"");}else loadFolder();}
 private void show(List<LocalMedia> entries) {visible.clear();visible.addAll(entries);adapter.notifyDataSetChanged();}
 private void loadFolder() {
  changeFolder.setVisibility(favorites?GONE:VISIBLE);recursive.setVisibility(VISIBLE);actions.navigationChanged(true);boolean includeDescendants=recursive.isChecked();  int request=++generation;LocalMedia folder=parents.peekLast();path.setText(folder.name);up.setVisibility(VISIBLE);message.setText("Indlæser …");show(Collections.emptyList());
  worker.execute(()->{try {List<LocalMedia> entries=LocalMedia.contents(getContext(),folder,includeDescendants);post(()->{if(closed||generation!=request)return;show(entries);message.setText(entries.isEmpty()?"Ingen lyd- eller videofiler i mappen.":"");});}
   catch(Exception failure) {post(()->{if(!closed&&generation==request) message.setText("Kan ikke åbne mappen. Tilføj den igen, hvis den er flyttet, slettet eller adgangen er fjernet.");});}});
 }
 public void close() {closed=true;generation++;worker.shutdownNow();}
 private final class Adapter extends BaseAdapter {
  public int getCount(){return visible.size();}public LocalMedia getItem(int p){return visible.get(p);}public long getItemId(int p){return p;}
  public View getView(int p,View recycled,ViewGroup parent) {
   LocalMedia entry=getItem(p);LinearLayout row=new LinearLayout(getContext());row.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);row.setGravity(Gravity.CENTER_VERTICAL);int pad=Math.round(8*getResources().getDisplayMetrics().density);row.setPadding(pad,pad,pad,pad);
   ImageView icon=new ImageView(getContext());icon.setImageResource(entry.folder?android.R.drawable.ic_menu_agenda:entry.audio()?android.R.drawable.ic_media_play:android.R.drawable.ic_menu_slideshow);row.addView(icon,new LayoutParams(pad*5,pad*5));
   TextView name=new TextView(getContext());name.setText(entry.name);name.setTextColor(Color.WHITE);name.setTextSize(16);name.setPadding(pad,0,pad,0);row.addView(name,new LayoutParams(0,-2,1));
   ImageButton star=new ImageButton(getContext());boolean favorite=actions.library().isLocalFavorite(entry.uri);star.setFocusable(false);star.setImageResource(favorite?R.drawable.ic_favorite:R.drawable.ic_favorite_outline);star.setBackgroundColor(Color.TRANSPARENT);star.setContentDescription((favorite?"Fjern fra favoritter: ":"Tilføj til favoritter: ")+entry.name);row.addView(star,new LayoutParams(pad*6,pad*6));star.setOnClickListener(v->{actions.save(()->actions.library().toggleLocalFavorite(entry));reload();});return row;
  }
 }
}
