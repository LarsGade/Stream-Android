package dk.stream.android;
import android.app.Activity;
import android.content.pm.PackageManager;
import java.util.*;
final class StartupPermissions {
 static String[] forVersion(int sdk){List<String> permissions=new ArrayList<>();if(sdk>=33)permissions.add("android.permission.POST_NOTIFICATIONS");if(sdk>=37)permissions.add("android.permission.ACCESS_LOCAL_NETWORK");if(sdk<=28)permissions.add("android.permission.WRITE_EXTERNAL_STORAGE");return permissions.toArray(new String[0]);}
 static void request(Activity activity){android.content.SharedPreferences prefs=activity.getSharedPreferences("startup_permissions",0);if(prefs.getBoolean("requested_v1",false))return;List<String> missing=new ArrayList<>();for(String permission:forVersion(android.os.Build.VERSION.SDK_INT))if(activity.checkSelfPermission(permission)!=PackageManager.PERMISSION_GRANTED)missing.add(permission);prefs.edit().putBoolean("requested_v1",true).apply();if(!missing.isEmpty())activity.requestPermissions(missing.toArray(new String[0]),7460);}
 private StartupPermissions(){}
}
