package dk.stream.android;

import android.content.Context;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;
import okhttp3.HttpUrl;

@androidx.media3.common.util.UnstableApi
public final class PcloudPanel extends LinearLayout {
 public interface Actions {Library library();boolean save(Runnable mutation);void login();void logout();void play(LocalMedia entry,List<LocalMedia> queue);void navigationChanged(boolean inside);}
 private final Actions actions;private final EditText search;private final TextView path,message;private final ImageButton up;private final Button login,logout,refresh;private final ListView list;
 private final List<LocalMedia> entries=new ArrayList<>(),visible=new ArrayList<>();private final ArrayDeque<HttpUrl> parents=new ArrayDeque<>();private final Adapter adapter=new Adapter();
 private final ExecutorService worker=Executors.newSingleThreadExecutor();private Future<?> task;private WebDavClient client;private int generation;private boolean closed;
 public PcloudPanel(Context context,Actions actions){
  super(context);this.actions=actions;setOrientation(VERTICAL);
  LinearLayout controls=new LinearLayout(context);addView(controls);login=new Button(context);login.setText("Log på");controls.addView(login,new LayoutParams(0,-2,1));login.setOnClickListener(view->actions.login());
  logout=new Button(context);logout.setText("Log ud");controls.addView(logout,new LayoutParams(0,-2,1));logout.setOnClickListener(view->actions.logout());refresh=new Button(context);refresh.setText("Opdatér");controls.addView(refresh,new LayoutParams(0,-2,1));refresh.setOnClickListener(view->load());
  search=new EditText(context);search.setSingleLine();search.setHint(R.string.search_hint);addView(search);search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence value,int start,int count,int after){}public void onTextChanged(CharSequence value,int start,int before,int count){filter();}public void afterTextChanged(android.text.Editable value){}});
  up=new ImageButton(context);up.setImageResource(R.drawable.ic_back);up.setBackgroundColor(Color.TRANSPARENT);up.setContentDescription("Tilbage");int size=Math.round(48*getResources().getDisplayMetrics().density);LayoutParams back=new LayoutParams(size,size);back.gravity=Gravity.LEFT;addView(up,back);up.setOnClickListener(view->back());
  path=new TextView(context);path.setTextColor(Color.WHITE);path.setTextSize(18);addView(path);message=new TextView(context);message.setTextColor(Color.LTGRAY);message.setVisibility(GONE);addView(message);
  list=new ListView(context);list.setAdapter(adapter);addView(list,new LayoutParams(-1,0,1));list.setOnItemClickListener((parent,view,index,id)->{LocalMedia entry=visible.get(index);if(entry.folder){try{parents.add(PcloudDocument.url(actions.library().pcloud,entry.uri));search.setText("");load();}catch(IllegalArgumentException error){showMessage(error.getMessage());}}else actions.play(entry,new ArrayList<>(visible));});
  list.setOnItemLongClickListener((parent,view,index,id)->{LocalMedia entry=visible.get(index);if(entry.folder)return true;PopupMenu menu=new PopupMenu(context,view);menu.getMenu().add(actions.library().isLocalFavorite(entry.uri)?"Fjern fra favoritter":"Tilføj til favoritter").setOnMenuItemClickListener(item->{actions.save(()->actions.library().toggleLocalFavorite(entry));adapter.notifyDataSetChanged();return true;});menu.show();return true;});
 }
 public void open(){cancel();parents.clear();PcloudAccount account=actions.library().pcloud;boolean connected=account!=null;login.setText(connected?"Konto":"Log på");logout.setVisibility(connected?VISIBLE:GONE);refresh.setVisibility(connected?VISIBLE:GONE);search.setVisibility(connected?VISIBLE:GONE);search.setText("");up.setVisibility(GONE);actions.navigationChanged(false);path.setText("Pcloud");showMessage("");entries.clear();filter();if(connected){parents.add(HttpUrl.get(account.server));load();}}
 public boolean back(){if(parents.size()<2)return false;parents.removeLast();search.setText("");load();return true;}
 private void cancel(){generation++;if(task!=null)task.cancel(true);if(client!=null){client.cancel();client=null;}}
 private void load(){
  PcloudAccount account=actions.library().pcloud;if(account==null||parents.isEmpty())return;cancel();int request=generation;HttpUrl folder=parents.peekLast();path.setText("/".equals(folder.encodedPath())?"Pcloud":folder.uri().getPath());up.setVisibility(parents.size()>1?VISIBLE:GONE);actions.navigationChanged(parents.size()>1);showMessage("");entries.clear();filter();refresh.setEnabled(false);WebDavClient requestClient=new WebDavClient(account);client=requestClient;
  task=worker.submit(()->{try{List<WebDavClient.Entry> result=requestClient.list(folder);List<LocalMedia> files=new ArrayList<>();for(WebDavClient.Entry entry:result)files.add(entry.media(account));post(()->{if(closed||request!=generation)return;entries.addAll(files);filter();refresh.setEnabled(true);});}catch(Exception error){post(()->{if(closed||request!=generation)return;showMessage(error instanceof java.io.IOException?error.getMessage():"Pcloud kunne ikke åbnes.");refresh.setEnabled(true);});}});
 }
 private void filter(){String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);visible.clear();for(LocalMedia entry:entries)if(entry.displayName().toLowerCase(Locale.ROOT).contains(query))visible.add(entry);adapter.notifyDataSetChanged();}
 private void showMessage(String value){message.setText(value);message.setVisibility(value==null||value.isEmpty()?GONE:VISIBLE);}
 public void playbackChanged(){adapter.notifyDataSetChanged();}
 public void close(){closed=true;cancel();worker.shutdownNow();}
 private final class Adapter extends BaseAdapter{
  public int getCount(){return visible.size();}public LocalMedia getItem(int index){return visible.get(index);}public long getItemId(int index){return index;}
  public View getView(int index,View recycled,ViewGroup parent){LocalMedia entry=getItem(index);LinearLayout row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);row.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);int pad=Math.round(8*getResources().getDisplayMetrics().density);row.setPadding(pad,pad,pad,pad);
   ImageView icon=new ImageView(getContext());boolean playing=LocalAudioService.playing(entry.uri)||CastPlayback.playingLocal(getContext(),entry.uri);icon.setImageResource(entry.folder?android.R.drawable.ic_menu_agenda:playing?R.drawable.ic_pause:R.drawable.ic_play);row.addView(icon,new LayoutParams(pad*5,pad*5));
   TextView name=new TextView(getContext());name.setText(entry.displayName());name.setTextColor(Color.WHITE);name.setTextSize(16);row.addView(name,new LayoutParams(0,-2,1));
   if(!entry.folder){ImageButton star=new ImageButton(getContext());star.setFocusable(false);star.setBackgroundColor(Color.TRANSPARENT);boolean favorite=actions.library().isLocalFavorite(entry.uri);star.setImageResource(favorite?R.drawable.ic_favorite:R.drawable.ic_favorite_outline);star.setContentDescription(favorite?"Fjern fra favoritter":"Tilføj til favoritter");row.addView(star,new LayoutParams(pad*6,pad*6));star.setOnClickListener(view->{actions.save(()->actions.library().toggleLocalFavorite(entry));adapter.notifyDataSetChanged();});}return row;
  }
 }
}
