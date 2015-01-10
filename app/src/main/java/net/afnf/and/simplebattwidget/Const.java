package net.afnf.and.simplebattwidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import net.afnf.and.utils.AndroidUtils;

public class Const {

    public static final String LOGTAG = "simplebattwidget";
    public static final String LOGDIR = "simplebattwidget";

    public static final String INTENT_PREFIX = Const.class.getPackage().getName() + ".INTENT";
    public static final String INTENT_WD_CLICKED = INTENT_PREFIX + ".WD_CLICKED";
    public static final String INTENT_WD_UPDATED = INTENT_PREFIX + ".WD_UPDATED";
    public static final String INTENT_WD_STYLE_CHANGED = INTENT_PREFIX + ".WD_STYLE_CHANGED";

    public static final int LOG_MIN_INTERVAL = 1000 * 60 * 1;
    public static final int NOTIF_MAIN_ID = 1;
    public static final int NOTIF_ALERT_ID = 2;
    public static final int NOTIF_DRAIN_ID = 3;

    public static int getMaxVoltage(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return AndroidUtils.getPrefInt(pref, context.getString(R.string.key_max_voltage),
                context.getString(R.string.dv_max_voltage));
    }

    public static int getWarningHighVoltage(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return AndroidUtils.getPrefInt(pref, context.getString(R.string.key_warning_high_voltage),
                context.getString(R.string.dv_warning_high_voltage));
    }

    public static int getWarningLowVoltage(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return AndroidUtils.getPrefInt(pref, context.getString(R.string.key_warning_low_voltage),
                context.getString(R.string.dv_warning_low_voltage));
    }

    public static int getMinVoltage(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return AndroidUtils.getPrefInt(pref, context.getString(R.string.key_min_voltage),
                context.getString(R.string.dv_min_voltage));
    }

    public static int getBatteryDrainRate(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return AndroidUtils.getPrefInt(pref, "key_drain_rate",
                context.getString(R.string.dv_drain_rate));
    }

    public static int getBatteryWaringRate(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return AndroidUtils.getPrefInt(pref, "key_warn_rate",
                context.getString(R.string.dv_warn_rate));
    }

    public static int calcLevel(Context context, int voltage, int magnify) {
        int max = Const.getMaxVoltage(context);
        int min = Const.getMinVoltage(context);
        return calcLevel(max, min, voltage, magnify);
    }

    public static int calcLevel(int max, int min, int voltage, int magnify) {
        if (voltage <= min) {
            return 0;
        }
        else if (max <= voltage) {
            return 100;
        }
        else {
            double d = voltage - min;
            int calcLevel = (int) (100.0 * magnify * d / (max - min));
            return calcLevel;
        }
    }
}
