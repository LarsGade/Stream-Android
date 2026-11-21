package dk.stream.android;
import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.os.Build;
import java.util.Locale;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
final class AppLanguage {
 static String saved(Context context){return normalize(context.getSharedPreferences("app_language",0).getString("language","da"));}
 static String normalize(String code){return "en".equals(code)?"en":"da";}
 static Context wrap(Context context){Configuration config=new Configuration(context.getResources().getConfiguration());config.setLocale(Locale.forLanguageTag(saved(context)));return context.createConfigurationContext(config);}
 static void init(Context context){String code=saved(context);UiText.init(context,code.equals("en"));apply(context,code);}
 static void choose(Context context,String code){code=normalize(code);context.getSharedPreferences("app_language",0).edit().putString("language",code).apply();UiText.init(context,code.equals("en"));apply(context,code);}
 private static void apply(Context context,String code){Locale.setDefault(Locale.forLanguageTag(code));if(Build.VERSION.SDK_INT>=33){LocaleManager manager=context.getSystemService(LocaleManager.class);android.os.LocaleList locales=android.os.LocaleList.forLanguageTags(code);if(!locales.equals(manager.getApplicationLocales()))manager.setApplicationLocales(locales);}else AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code));Configuration config=new Configuration(context.getResources().getConfiguration());config.setLocale(Locale.forLanguageTag(code));context.getResources().updateConfiguration(config,context.getResources().getDisplayMetrics());Context application=context.getApplicationContext();if(application!=null)application.getResources().updateConfiguration(config,application.getResources().getDisplayMetrics());}
 private AppLanguage(){}
}
