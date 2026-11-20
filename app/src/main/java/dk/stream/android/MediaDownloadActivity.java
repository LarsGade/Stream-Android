package dk.stream.android;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import java.util.concurrent.*;
import org.json.*;

@androidx.media3.common.util.UnstableApi
public final class MediaDownloadActivity extends AppCompatActivity {
 private final ExecutorService worker=Executors.newSingleThreadExecutor();private String plan;private boolean picking;
 private final ActivityResultLauncher<Intent> save=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),result->{if(result.getResultCode()!=RESULT_OK||result.getData()==null||result.getData().getData()==null){cleanup();finish();return;}Uri destination=result.getData().getData();try{getContentResolver().takePersistableUriPermission(destination,Intent.FLAG_GRANT_WRITE_URI_PERMISSION);if((result.getData().getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0)getContentResolver().takePersistableUriPermission(destination,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException ignored){}try{androidx.core.content.ContextCompat.startForegroundService(this,new Intent(this,MediaDownloadService.class).setData(destination).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION).putExtra("plan",plan));}catch(RuntimeException failure){Toast.makeText(this,"Download kunne ikke startes.",Toast.LENGTH_LONG).show();cleanup();}finish();});
 private final ActivityResultLauncher<String> notifications=registerForActivityResult(new ActivityResultContracts.RequestPermission(),granted->{if(!granted)Toast.makeText(this,"Tillad notifikationer i telefonens indstillinger for at se og styre Download.",Toast.LENGTH_LONG).show();prepareDownload();});
 @Override public void onCreate(Bundle state){super.onCreate(state);TextView message=new TextView(this);message.setText("Klargør download …");message.setTextColor(android.graphics.Color.WHITE);message.setGravity(android.view.Gravity.CENTER);message.setBackgroundColor(0xFF101010);setContentView(message);if(state!=null){plan=state.getString("plan");picking=state.getBoolean("picking");}if(picking)return;if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)notifications.launch(android.Manifest.permission.POST_NOTIFICATIONS);else prepareDownload();}
 private void prepareDownload(){String request=getIntent().getStringExtra("request");worker.execute(()->{try{JSONObject prepared=DownloadPlanner.plan(this,MediaDownloads.read(this,request));plan=MediaDownloads.save(this,prepared);MediaDownloads.file(this,request).delete();runOnUiThread(()->{if(isFinishing()||isDestroyed()){cleanup();return;}picking=true;try{save.launch(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(prepared.optString("mime","application/octet-stream")).putExtra(Intent.EXTRA_TITLE,prepared.optString("name","Download")));}catch(RuntimeException unavailable){cleanup();Toast.makeText(this,"Filvælgeren kunne ikke åbnes.",Toast.LENGTH_LONG).show();finish();}});}catch(Exception failure){try{MediaDownloads.file(this,request).delete();}catch(Exception ignored){}runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())Toast.makeText(this,"Download kunne ikke klargøres. Kontrollér adgang til filerne.",Toast.LENGTH_LONG).show();cleanup();finish();});}});}
 private void cleanup(){if(plan!=null)try{MediaDownloads.file(this,plan).delete();}catch(Exception ignored){}}
 @Override protected void onSaveInstanceState(Bundle state){state.putString("plan",plan);state.putBoolean("picking",picking);super.onSaveInstanceState(state);}
 @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
