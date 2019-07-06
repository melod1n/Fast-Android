package ru.melodin.fast.common;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.preference.PreferenceManager;

import java.util.Locale;

import ru.melodin.fast.database.DatabaseHelper;

public class AppGlobal extends Application {

    public static volatile SQLiteDatabase database;
    public static volatile Locale locale;
    public static volatile Handler handler;
    public static volatile SharedPreferences preferences;
    public static volatile String app_version_name;
    public static volatile int app_version_code;
    private static AppGlobal instance;

    public static synchronized AppGlobal getContext() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        handler = new Handler(getMainLooper());
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        database = DatabaseHelper.getInstance().getWritableDatabase();
        locale = Locale.getDefault();

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            app_version_name = pInfo.versionName;
            app_version_code = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        CrashManager.init();
        TaskManager.init();
        ThemeManager.init();
    }
}
