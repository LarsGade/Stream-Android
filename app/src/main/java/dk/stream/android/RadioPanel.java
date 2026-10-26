package dk.stream.android;

import android.content.*;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import com.bumptech.glide.Glide;
import java.util.*;
import java.util.concurrent.*;


@android.annotation.SuppressLint("ViewConstructor")
@androidx.media3.common.util.UnstableApi
public final class RadioPanel extends LinearLayout {
 public interface LibrarySource {Library get();}
 public interface Save {boolean apply(Runnable mutation);}
 private final LibrarySource library;
 private final Save save;
 private final Runnable beforePlay;
 private final LocalChannelLogos logos;
 private final RadioBrowser browser;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final Handler main=new Handler(Looper.getMainLooper());
 private final FavoriteListView list;
 private final TextView title,message,playing;
 private final EditText search;
 private final Button refresh,stop;
 private final Adapter adapter=new Adapter();
 private List<RadioStation> stations=new ArrayList<>(),visible=new ArrayList<>();
 private boolean favorites,loading,loaded,closed;
 private final Runnable tick=new Runnable() {public void run() {if(closed || !isAttachedToWindow()) return;updatePlayback();postDelayed(this,1000);}};
 public RadioPanel(Context context,LibrarySource library,Save save,Runnable beforePlay) {this(context,library,save,beforePlay,new RadioBrowser());}
 RadioPanel(Context context,LibrarySource library,Save save,Runnable beforePlay,RadioBrowser browser) {
  super(context);this.library=library;this.save=save;this.beforePlay=beforePlay;this.browser=browser;logos=LocalChannelLogos.load(context);setOrientation(VERTICAL);
  title=text("",20);addView(title);
  search=new EditText(context);search.setSingleLine();search.setHint(R.string.search_hint);addView(search);
  search.addTextChangedListener(new android.text.TextWatcher() {public void beforeTextChanged(CharSequence s,int a,int c,int f) { }public void onTextChanged(CharSequence s,int a,int b,int c) {filter();}public void afterTextChanged(android.text.Editable e) { }});
  refresh=new Button(context);refresh.setText(R.string.refresh_radio);addView(refresh);refresh.setOnClickListener(v->load());
  message=text("",13);message.setTextColor(Color.LTGRAY);addView(message);
  list=new FavoriteListView(context);list.setDividerHeight(dp(6));addView(list,new LayoutParams(-1,0,1));list.setAdapter(adapter);
  list.setOnItemClickListener((parent,view,position,id)->play(visible.get(position)));
  list.setFavoriteListener(new FavoriteListView.Listener() {
   public boolean enabled() {return favorites;}
   public void showActions(int position,View anchor) {
    if(position<0||position>=visible.size()) return;RadioStation station=visible.get(position);PopupMenu menu=new PopupMenu(context,anchor);
    menu.getMenu().add(0,1,0,R.string.rename_radio).setOnMenuItemClickListener(item->{rename(station);return true;});
    menu.getMenu().add(0,2,1,R.string.remove_from_favorites).setOnMenuItemClickListener(item->{save.apply(()->library.get().removeRadio(station.id));filter();return true;});menu.show();
   }
   public void move(int from,int to) {RadioStation station=visible.remove(from);visible.add(to,station);adapter.notifyDataSetChanged();}
   public void commit() {List<RadioStation> order=new ArrayList<>(visible);save.apply(()->library.get().reorderRadio(order));filter();}
   public void cancel() {filter();}
  });
  playing=text("",13);addView(playing);stop=new Button(context);stop.setText(R.string.radio_stop);addView(stop);stop.setOnClickListener(v->{context.stopService(new Intent(context,RadioPlaybackService.class));postDelayed(this::updatePlayback,100);});
  TextView credit=text("Radio Browser",11);credit.setTextColor(Color.LTGRAY);credit.setGravity(Gravity.CENTER);addView(credit);credit.setOnClickListener(v->context.startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://www.radio-browser.info/"))));
 }
 private int dp(int value) {return Math.round(value*getResources().getDisplayMetrics().density);}
 private TextView text(String value,int size) {TextView view=new TextView(getContext());view.setText(value);view.setTextSize(size);view.setTextColor(Color.WHITE);view.setPadding(dp(4),dp(6),dp(4),dp(6));return view;}
 public void open(boolean favorites) {list.cancelFavoriteGesture();this.favorites=favorites;search.setText("");filter();updatePlayback();if(!favorites && !loaded && !loading) load();}
 private void load() {
  if(loading) return;loading=true;refresh.setEnabled(false);filter();
  worker.execute(()->{
   try {List<RadioStation> result=browser.loadDanish();main.post(()->{if(closed)return;stations=result;loaded=true;loading=false;refresh.setEnabled(true);filter();});}
   catch(Exception error) {main.post(()->{if(closed)return;loading=false;refresh.setEnabled(true);filter();if(!favorites) message.setText(R.string.radio_failed);});}
  });
 }
 private void filter() {
  title.setText(favorites?R.string.radio_favorites:R.string.radio);search.setVisibility(favorites?GONE:VISIBLE);refresh.setVisibility(favorites?GONE:VISIBLE);
  visible=new ArrayList<>();String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);
  for(RadioStation station:favorites?library.get().radioFavorites:stations) if(favorites || station.displayName().toLowerCase(Locale.ROOT).contains(query)) visible.add(station);
  message.setText(favorites?(visible.isEmpty()?getContext().getString(R.string.radio_favorites_empty):""):loading?getContext().getString(R.string.radio_loading):visible.isEmpty()?getContext().getString(R.string.radio_empty):"");adapter.notifyDataSetChanged();
 }
 private void play(RadioStation station) {beforePlay.run();androidx.core.content.ContextCompat.startForegroundService(getContext(),RadioPlaybackService.playIntent(getContext(),station));postDelayed(this::updatePlayback,300);}
 private void updatePlayback() {String now=RadioPlaybackService.nowPlaying(),error=RadioPlaybackService.error();playing.setText(error.isEmpty()?now:error);stop.setVisibility(now.isEmpty()?GONE:VISIBLE);}
 private void rename(RadioStation station) {
  EditText name=new EditText(getContext());name.setSingleLine();name.setText(station.displayName());
  AlertDialog dialog=new AlertDialog.Builder(getContext()).setTitle(R.string.rename_radio).setView(name).setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.save_source,null).create();
  dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(name.getText().toString().trim().isEmpty()) {name.setError(getContext().getString(R.string.rename_radio));return;}if(save.apply(()->library.get().renameRadio(station.id,name.getText().toString()))) {dialog.dismiss();filter();}}));dialog.show();
 }
 public void close() {closed=true;worker.shutdownNow();removeCallbacks(tick);main.removeCallbacksAndMessages(null);}
 @Override protected void onAttachedToWindow() {super.onAttachedToWindow();removeCallbacks(tick);post(tick);}
 @Override protected void onDetachedFromWindow() {removeCallbacks(tick);super.onDetachedFromWindow();}
 private final class Adapter extends BaseAdapter {
  public int getCount() {return visible.size();}public RadioStation getItem(int position) {return visible.get(position);}public long getItemId(int position) {return position;}
  public View getView(int position,View recycled,ViewGroup parent) {
   RadioStation station=getItem(position);LinearLayout row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(8),dp(8),dp(4),dp(8));row.setBackgroundColor(0xFF1D1D1D);
   ImageView logo=new ImageView(getContext());logo.setScaleType(ImageView.ScaleType.FIT_CENTER);row.addView(logo,new LayoutParams(dp(48),dp(48)));
   TextView name=text(station.displayName(),16);name.setMaxLines(2);row.addView(name,new LayoutParams(0,-2,1));
   String local=logos.find(station.displayName());Glide.with(RadioPanel.this).load(local==null?station.logo:local).placeholder(R.drawable.ic_channel).error(R.drawable.ic_channel).override(dp(96),dp(96)).into(logo);
   if(!favorites) {boolean favorite=library.get().findRadio(station.id)!=null;ImageButton star=new ImageButton(getContext());star.setImageResource(favorite?R.drawable.ic_favorite:R.drawable.ic_favorite_outline);star.setContentDescription(getContext().getString(favorite?R.string.remove_favorite:R.string.add_favorite,station.displayName()));star.setFocusable(false);star.setBackgroundColor(Color.TRANSPARENT);row.addView(star,new LayoutParams(dp(48),dp(48)));star.setOnClickListener(v->{save.apply(()->{if(library.get().findRadio(station.id)==null) library.get().addRadio(station);else library.get().removeRadio(station.id);});filter();});}
   row.setAlpha(list.isPicked(position)?0.6f:1f);return row;
  }
 }
}
