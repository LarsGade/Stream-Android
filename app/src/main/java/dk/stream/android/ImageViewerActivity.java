package dk.stream.android;

import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.*;
import com.bumptech.glide.request.*;
import com.bumptech.glide.request.target.Target;
import java.util.*;

/** Full picture viewer with a stable, manually navigated gallery on phone and Cast. */
@androidx.media3.common.util.UnstableApi
public final class ImageViewerActivity extends AppCompatActivity {
 @Override public boolean dispatchKeyEvent(android.view.KeyEvent event){return CastPlayback.volumeKey(this,event)||super.dispatchKeyEvent(event);}
 private ImageView picture;private TextView title,count,error;private ImageButton previous,next,slideshow,stop;private androidx.mediarouter.app.MediaRouteButton castButton;private boolean initial;
 private boolean slideshowActive;
 private final Runnable advanceSlide=()->{if(!slideshowActive)return;if(ImageSession.move(1)){render(true);scheduleSlide();}else setSlideshow(false);};
 private float downX,downY;private final Object owner=new Object();private final Handler handler=new Handler(Looper.getMainLooper());
 private final Runnable tick=new Runnable(){public void run(){updateControls();handler.postDelayed(this,500);}};
 public static Intent intent(Context context,LocalMedia selected,List<LocalMedia> displayed){ImageSession.start(selected,displayed);return reopenIntent(context);}
 public static Intent reopenIntent(Context context){Intent intent=new Intent(context,ImageViewerActivity.class);LocalMedia current=ImageSession.current();if(current!=null)intent.putExtra("uri",current.uri).putExtra("name",current.name).putExtra("mime",current.mime).putExtra("gallery",ImageSession.token());return intent;}
 @Override public void onCreate(Bundle state){
  super.onCreate(state);CastPlayback.init(this);String uri=state==null?getIntent().getStringExtra("uri"):state.getString("uri");if(uri==null){finish();return;}
  try{LocalMedia fallback=new LocalMedia(uri,state==null?getIntent().getStringExtra("name"):state.getString("name"),state==null?getIntent().getStringExtra("mime"):state.getString("mime"),false);if(!fallback.image())throw new IllegalArgumentException();ImageSession.restore(state==null?getIntent().getStringExtra("gallery"):state.getString("gallery"),fallback);}catch(IllegalArgumentException invalid){finish();return;}
  initial=state==null;slideshowActive=state!=null&&state.getBoolean("slideshow");
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.BLACK);setContentView(root);int padding=dp(12);root.setPadding(padding,padding,padding,padding);
  androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{androidx.core.graphics.Insets bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()|androidx.core.view.WindowInsetsCompat.Type.displayCutout());view.setPadding(bars.left+padding,bars.top+padding,bars.right+padding,bars.bottom+padding);return insets;});
  LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);root.addView(top,new LinearLayout.LayoutParams(-1,-2));ImageButton back=button(top,R.drawable.ic_back,"Tilbage");back.setOnClickListener(view->finish());
  title=new TextView(this);title.setTextColor(Color.WHITE);title.setTextSize(18);title.setSingleLine();title.setEllipsize(android.text.TextUtils.TruncateAt.END);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));

  castButton=new CastOutputButton(this){@Override public boolean performClick(){if(!networkAllowed()){requestPermissions(new String[]{"android.permission.ACCESS_LOCAL_NETWORK"},7401);return true;}return super.performClick();}};LinearLayout.LayoutParams route=new LinearLayout.LayoutParams(dp(48),dp(48));route.setMarginEnd(dp(8));try{com.google.android.gms.cast.framework.CastButtonFactory.setUpMediaRouteButton(this,castButton);}catch(RuntimeException unavailable){}
  picture=new ImageView(this);picture.setScaleType(ImageView.ScaleType.FIT_CENTER);root.addView(picture,new LinearLayout.LayoutParams(-1,0,1));picture.setOnTouchListener((view,event)->{if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN){downX=event.getX();downY=event.getY();return true;}if(event.getActionMasked()==android.view.MotionEvent.ACTION_UP){float dx=event.getX()-downX,dy=event.getY()-downY;if(Math.abs(dx)>dp(64)&&Math.abs(dx)>Math.abs(dy))move(dx>0?-1:1);else view.performClick();return true;}return true;});
  error=new TextView(this);error.setTextColor(Color.LTGRAY);error.setGravity(Gravity.CENTER);error.setVisibility(View.GONE);root.addView(error);count=new TextView(this);count.setTextColor(Color.LTGRAY);count.setGravity(Gravity.CENTER);root.addView(count);
  LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);root.addView(controls);
  controls.addView(new BluetoothOutputButton(this),new LinearLayout.LayoutParams(dp(48),dp(48)));controls.addView(castButton,route);
  previous=button(controls,R.drawable.ic_previous,"Forrige billede");previous.setOnClickListener(view->move(-1));slideshow=button(controls,R.drawable.ic_slideshow,"Start slideshow");slideshow.setOnClickListener(view->setSlideshow(!slideshowActive));stop=button(controls,R.drawable.ic_stop,"Stop casting");stop.setOnClickListener(view->{CastPlayback.stop(this);updateControls();});next=button(controls,R.drawable.ic_next,"Næste billede");next.setOnClickListener(view->move(1));
 }
 private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
 private ImageButton button(LinearLayout parent,int icon,String label){ImageButton button=new ImageButton(this);button.setImageResource(icon);button.setContentDescription(label);button.setBackgroundColor(Color.TRANSPARENT);parent.addView(button,new LinearLayout.LayoutParams(dp(48),dp(48)));return button;}
 private void move(int direction){if(ImageSession.move(direction)){render(true);scheduleSlide();}}
 private void scheduleSlide(){handler.removeCallbacks(advanceSlide);if(slideshowActive)handler.postDelayed(advanceSlide,10000);}
 private void setSlideshow(boolean active){slideshowActive=active&&ImageSession.size()>1;picture.setKeepScreenOn(slideshowActive);scheduleSlide();updateControls();}
 private void render(boolean cast){
  LocalMedia current=ImageSession.current();if(current==null){finish();return;}title.setText(current.displayName());picture.setContentDescription(current.displayName());error.setVisibility(View.GONE);count.setText((ImageSession.index()+1)+" / "+ImageSession.size());
  Glide.with(this).load(Uri.parse(current.uri)).fitCenter().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).listener(new RequestListener<Drawable>(){
   public boolean onLoadFailed(GlideException failure,Object model,Target<Drawable> target,boolean first){if(ImageSession.current()!=null&&current.uri.equals(ImageSession.current().uri)){error.setText("Billedet kan ikke vises. Kontrollér filformat og adgang.");error.setVisibility(View.VISIBLE);}return false;}
   public boolean onResourceReady(Drawable image,Object model,Target<Drawable> target,DataSource source,boolean first){return false;}
  }).into(picture);updateControls();if(cast&&CastPlayback.connected(this))castCurrent();
 }
 private void updateControls(){if(previous==null||ImageSession.current()==null)return;slideshow.setEnabled(ImageSession.size()>1);slideshow.setAlpha(slideshow.isEnabled()?1f:0.35f);slideshow.setSelected(slideshowActive);slideshow.setImageTintList(android.content.res.ColorStateList.valueOf(slideshowActive?0xFF3686FF:Color.WHITE));slideshow.setContentDescription(slideshowActive?"Stop slideshow":"Start slideshow");previous.setEnabled(ImageSession.index()>0);next.setEnabled(ImageSession.index()+1<ImageSession.size());previous.setAlpha(previous.isEnabled()?1f:0.35f);next.setAlpha(next.isEnabled()?1f:0.35f);stop.setVisibility(CastPlayback.image(this)?View.VISIBLE:View.GONE);}
 private boolean networkAllowed(){return android.os.Build.VERSION.SDK_INT<37||checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK")==android.content.pm.PackageManager.PERMISSION_GRANTED;}
 private void castCurrent(){if(!networkAllowed()){requestPermissions(new String[]{"android.permission.ACCESS_LOCAL_NETWORK"},7402);return;}LocalMedia current=ImageSession.current();if(current!=null)CastPlayback.playLocal(this,current,ImageSession.entries(),0);}
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(results.length==0||results[0]!=android.content.pm.PackageManager.PERMISSION_GRANTED)return;if(request==7401)castButton.performClick();else if(request==7402)castCurrent();}
 @Override protected void onSaveInstanceState(Bundle state){LocalMedia current=ImageSession.current();if(current!=null){state.putString("uri",current.uri);state.putString("name",current.name);state.putString("mime",current.mime);state.putString("gallery",ImageSession.token());state.putBoolean("slideshow",slideshowActive);}super.onSaveInstanceState(state);}
 @Override protected void onStart(){super.onStart();if(picture==null)return;ImageSession.visible(owner);render(initial);initial=false;setSlideshow(slideshowActive);handler.post(tick);}
 @Override protected void onStop(){handler.removeCallbacks(tick);handler.removeCallbacks(advanceSlide);ImageSession.hidden(owner);if(picture!=null)Glide.with(this).clear(picture);super.onStop();}
}
