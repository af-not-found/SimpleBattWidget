package net.afnf.and.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.preference.PreferenceManager;

import net.afnf.and.simplebattwidget.Const;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AndroidUtils {

    public static final File CONFIG_FILE;

    static {
        File outdir = new File(Environment.getExternalStorageDirectory(), Const.LOGDIR);
        CONFIG_FILE = new File(outdir, "_config.bin");
    }

    private AndroidUtils() {

    }

    public static String getAction(Intent intent) {
        if (intent != null) {
            return intent.getAction();
        }
        return null;
    }

    public static String getActionForLog(Intent intent) {
        String action = getAction(intent);
        if (action != null) {
            if (action.startsWith(Const.INTENT_PREFIX)) {
                return action.substring(Const.INTENT_PREFIX.length());
            }
            else {
                return action;
            }
        }
        return null;
    }

    public static boolean isActionEquals(Intent intent, String expected) {
        if (expected == null) {
            return false;
        }
        else {
            String action = getAction(intent);
            return action != null && action.equals(expected);
        }
    }

    public static boolean isUIThread(Context context) {
        return Thread.currentThread().equals(context.getMainLooper().getThread());
    }

    public static int indexOf(String[] datas, String val) {
        if (val != null && datas != null) {
            for (int i = 0; i < datas.length; i++) {
                if (datas[i] != null && datas[i].equals(val)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static boolean isWifiDisabled(WifiManager wifi) {
        return wifi.getWifiState() == WifiManager.WIFI_STATE_DISABLED;
    }

    public static boolean isWifiEnabled(WifiManager wifi) {
        return wifi.getWifiState() == WifiManager.WIFI_STATE_ENABLED;
    }

    public static boolean isWifiEnabledOrEnabling(WifiManager wifi) {
        int wifiState = wifi.getWifiState();
        return wifiState == WifiManager.WIFI_STATE_ENABLED || wifiState == WifiManager.WIFI_STATE_ENABLING;
    }

    public static boolean sleep(long ms) {
        try {
            if (ms > 0) {
                Thread.sleep(ms);
            }
            return true;
        }
        catch (InterruptedException e) {
            return false;
        }
    }

    public static String getBuildDate(Context context) {
        ZipFile zf = null;
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
            zf = new ZipFile(ai.sourceDir);
            ZipEntry ze = zf.getEntry("classes.dex");
            long time = ze.getTime();
            return SimpleDateFormat.getDateTimeInstance().format(new java.util.Date(time));
        }
        catch (Exception e) {
            Logger.w("getBuildDate failed", e);
            return "";
        }
        finally {
            if (zf != null) {
                try {
                    zf.close();
                }
                catch (Exception e) {
                }
            }
        }
    }

    public static String getAppVer(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName + "(" + packageInfo.versionCode + ")";
        }
        catch (Throwable e) {
            Logger.w("getPackageInfo failed", e);
            return "";
        }
    }

    public static String intToIpaddr(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + ((i >> 24) & 0xFF);
    }

    /**
     * http://developer.android.com/guide/practices/screens_support.html#dips-pels
     */
    public static int dip2pixel(Context context, int dip) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dip * scale + 0.5f);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 設定系
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static int getPrefInt(SharedPreferences pref, String key, int default_value) {
        Object value = getPrefObject(pref, key);
        int ret = default_value;
        if (value != null && value instanceof String) {
            String str = (String) value;
            if (MyStringUtlis.isEmpty(str) == false) {
                try {
                    ret = Integer.parseInt(str);
                }
                catch (Throwable e) {
                    Logger.w("invalid value, key=" + key + ", value=" + str);
                }
            }
        }
        return ret;
    }

    public static int getPrefInt(SharedPreferences pref, String key, String default_value) {
        int ret = getPrefInt(pref, key, Integer.MIN_VALUE);
        if (ret == Integer.MIN_VALUE) {
            ret = Integer.parseInt(default_value);
        }
        return ret;
    }

    public static String getPrefString(SharedPreferences pref, String key) {
        Object value = getPrefObject(pref, key);
        if (value instanceof String) {
            String s = (String) value;
            if (MyStringUtlis.isEmpty(s) == false) {
                return s;
            }
        }
        return null;
    }

    public static Boolean getPrefBoolean(SharedPreferences pref, String key) {
        Object value = getPrefObject(pref, key);
        return value != null ? (Boolean) value : null;
    }

    protected static Object getPrefObject(SharedPreferences pref, String key) {

        // 値がある場合
        Map<String, ?> all = pref.getAll();
        if (all != null) {
            Object obj = all.get(key);
            return obj;
        }

        return null;
    }

    @SuppressWarnings({"unchecked"})
    public static boolean loadPreferencesFromFile(Context context) {
        boolean success = false;
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(new FileInputStream(CONFIG_FILE));
            Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
            edit.clear(); // 一旦破棄
            Map<String, ?> entries = (Map<String, ?>) input.readObject();
            for (Entry<String, ?> entry : entries.entrySet()) {
                Object v = entry.getValue();
                String key = entry.getKey();
                if (v instanceof Boolean) {
                    edit.putBoolean(key, ((Boolean) v).booleanValue());
                }
                else if (v instanceof Float) {
                    edit.putFloat(key, ((Float) v).floatValue());
                }
                else if (v instanceof Integer) {
                    edit.putInt(key, ((Integer) v).intValue());
                }
                else if (v instanceof Long) {
                    edit.putLong(key, ((Long) v).longValue());
                }
                else if (v instanceof String) {
                    edit.putString(key, ((String) v));
                }
            }
            edit.commit();
            success = true;
        }
        catch (Throwable e) {
            Logger.e("loadPreferencesFromFile failed", e);
        }
        finally {
            try {
                if (input != null) {
                    input.close();
                }
            }
            catch (Throwable e) {
            }
        }
        return success;
    }

    public static void deleteAllPreference(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        Editor edit = pref.edit();
        edit.clear();
        edit.commit();
    }
}
