package dk.stream.android;
import android.content.*;
import android.os.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import java.util.concurrent.*;
import org.json.*;

@androidx.media3.common.util.UnstableApi
public final class MediaDownloadActivity extends AppCompatActivity {
 private String plan;private boolean started,notificationsAsked;
 private final ActivityResultLauncher<String> notifications=registerForActivityResult(new ActivityResultContracts.RequestPermission(),granted->{if(!granted)UiText.toast(this,"Tillad notifikationer i telefonens indstillinger for at se og styre Download.",Toast.LENGTH_LONG).show();permissions();});
 private final ActivityResultLauncher<String> storage=registerForActivityResult(new ActivityResultContracts.RequestPermission(),granted->{if(granted)prepareDownload();else{UiText.toast(this,"Tillad lagring for at gemme i Download-mappen.",Toast.LENGTH_LONG).show();finish();}});
 @Override public void onCreate(Bundle state){super.onCreate(state);TextView message=new TextView(this);message.setText(UiText.text("Klargør download …"));message.setTextColor(android.graphics.Color.WHITE);message.setGravity(android.view.Gravity.CENTER);message.setBackgroundColor(0xFF101010);setContentView(message);if(state!=null){plan=state.getString("plan");started=state.getBoolean("started");notificationsAsked=state.getBoolean("notificationsAsked");}if(started){finish();return;}permissions();}
 private void permissions(){if(Build.VERSION.SDK_INT>=33&&!notificationsAsked&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){notificationsAsked=true;notifications.launch(android.Manifest.permission.POST_NOTIFICATIONS);return;}if(Build.VERSION.SDK_INT<=28&&checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!=android.content.pm.PackageManager.PERMISSION_GRANTED){storage.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);return;}prepareDownload();}
 private void prepareDownload(){if(started)return;plan=getIntent().getStringExtra("request");startDownload();}
 private void startDownload(){try{androidx.core.content.ContextCompat.startForegroundService(this,new Intent(this,MediaDownloadService.class).putExtra("plan",plan).putExtra("automatic",true));started=true;}catch(RuntimeException failure){UiText.toast(this,"Download kunne ikke startes.",Toast.LENGTH_LONG).show();cleanup();}finish();}
 private void cleanup(){if(plan!=null)try{MediaDownloads.file(this,plan).delete();}catch(Exception ignored){}}
 @Override protected void onSaveInstanceState(Bundle state){state.putString("plan",plan);state.putBoolean("started",started);state.putBoolean("notificationsAsked",notificationsAsked);super.onSaveInstanceState(state);}
}
