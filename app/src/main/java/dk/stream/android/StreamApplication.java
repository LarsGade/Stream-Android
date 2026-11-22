package dk.stream.android;
public final class StreamApplication extends android.app.Application {
 @Override protected void attachBaseContext(android.content.Context base){super.attachBaseContext(AppLanguage.wrap(base));}
 @Override public void onCreate(){super.onCreate();AppLanguage.init(this);}
}
