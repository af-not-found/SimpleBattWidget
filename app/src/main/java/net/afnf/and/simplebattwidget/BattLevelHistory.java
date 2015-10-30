package net.afnf.and.simplebattwidget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import net.afnf.and.utils.MyStringUtlis;

public class BattLevelHistory {

    public static final String PREF_KEY = "BattLevelHistory.hisdata";
    public static final int MSEC_1H = 1000 * 60 * 60 * 1;
    //    public static final int MSEC_24H = MSEC_1H * 24;
    //    public static final int HIS_INTERVAL_MIN = 60;
    //    public static final int MSEC_INTERVAL = 1000 * 60 * HIS_INTERVAL_MIN;
    public static final int HIS_NUM = 24; // 24時間
    protected int[] history = new int[HIS_NUM];
    public static final long TIME_OFFSET = 1417392000000L; // 2014/12/01 00:00:00+00 (UTC)
    protected int offset_hour = -1;

    public static BattLevelHistory restore(Context context) {
        BattLevelHistory instance = new BattLevelHistory();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String str = pref.getString(PREF_KEY, "0@0");
        instance.fromString(str);
        return instance;
    }

    public void save(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = pref.edit();
        edit.putString(PREF_KEY, this.toString());
        edit.commit();
    }

    protected void fromString(String str) {
        try {
            String[] val = str.split("@");
            offset_hour = MyStringUtlis.toInt(val[0], -1);

            String[] historyStr = val[1].split(",");
            int count = Math.min(historyStr.length, HIS_NUM);
            for (int i = 0; i < count; i++) {
                history[i] = MyStringUtlis.toInt(historyStr[i], -1);
            }
        }
        catch (Exception e) {
            Log.w("fromString failed, str=" + str, e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(offset_hour);
        sb.append("@");
        for (int i = 0; i < HIS_NUM; i++) {
            sb.append(history[i]);
            sb.append(",");
        }
        return sb.toString();
    }

    protected boolean updateHistory(long now, int value) {
        int this_offset_hour = (int) ((now - TIME_OFFSET) / MSEC_1H);

        if (offset_hour == -1) {
            offset_hour = this_offset_hour - 1;
        }
        int diff = this_offset_hour - offset_hour;
        offset_hour = this_offset_hour;

        if (diff <= 0) {
            return false;
        }

        if (diff < HIS_NUM) {
            for (int i = HIS_NUM - diff - 1; i >= 0; i--) {
                history[i + diff] = history[i];
            }

            if (diff >= 2) {
                for (int i = 1; i < diff; i++) {
                    history[i] = history[diff];
                }
            }
        }

        history[0] = value;

        return true;
    }

    protected float calcUsage(int offset) {
        return history[offset] - history[offset + 1];
    }

    public String getUsageStr() {
        return getUsageStr(0);
    }

    public String getUsageStr(int offset) {
        float this_usage = calcUsage(offset);
        return convertValueToUsageStr(this_usage);
    }

    public int getPrevUsageColorId(Context context) {
        float prev0 = calcUsage(0);
        float prev1 = calcUsage(1);
        float prev2 = calcUsage(2);
        int color_id = R.color.orig;

        int dr = Const.getBatteryDrainRate(context);
        int wr = Const.getBatteryWaringRate(context);
        if (dr < 0 && prev0 <= dr && prev1 <= dr && prev2 <= dr) {
            color_id = R.color.fast;
        }
        else if (wr < 0 && prev0 <= wr && prev1 <= wr && prev2 <= wr) {
            color_id = R.color.mid;
        }
        else if (wr < 0 && (prev1 <= wr || prev2 <= wr)) {
            color_id = R.color.slow;
        }
        return color_id;
    }

    @SuppressLint("DefaultLocale")
    protected String convertValueToUsageStr(float this_usage) {
        String fmt = null;
        if ((int) this_usage == 0) {
            fmt = "%.1f%%/h";
            this_usage = +0.0f;
        }
        else {
            fmt = "%+.1f%%/h";
        }
        String s = String.format(fmt, this_usage);
        int len = s.length();
        s = "     ".substring(0, (8 - len)) + s;
        return s;

    }
}
