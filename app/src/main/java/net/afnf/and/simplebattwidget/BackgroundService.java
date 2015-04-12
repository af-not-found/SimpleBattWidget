package net.afnf.and.simplebattwidget;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import net.afnf.and.utils.AndroidUtils;
import net.afnf.and.utils.Logger;

public class BackgroundService extends Service {

    public static final int NUM_PREV_VOLTAGE = 4;
    protected int[] prev_voltages = new int[NUM_PREV_VOLTAGE];
    protected static final int[] prev_voltage_weights = new int[]{10, 18, 36, 36};
    protected static BackgroundService instance;
    protected NotificationCompat.Builder notificationBuilder;
    protected int averageVoltage = 0;
    protected int voltage = 0;
    protected int calcedLevel = -1;
    protected int level = -1;
    protected int temperature = -100;
    protected boolean charging = false;
    protected long lastBattUpdate = 0;
    protected long lastLog = 0;
    protected float usage = 0;
    protected BattLevelHistory blh = null;
    boolean alertNotified = false;
    boolean drainNotified = false;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {

            // SCREEN_ON
            if (AndroidUtils.isActionEquals(intent, Intent.ACTION_SCREEN_ON)) {
                // ウィジェット更新
                DefaultWidgetProvider.update(context);
            }

            // バッテリー更新
            else if (AndroidUtils.isActionEquals(intent, Intent.ACTION_BATTERY_CHANGED)) {

                lastBattUpdate = System.currentTimeMillis();

                voltage = intent.getIntExtra("voltage", 0);
                level = intent.getIntExtra("level", 0);
                temperature = intent.getIntExtra("temperature", 0);
                boolean charging_prev = charging;
                charging = intent.getIntExtra("status", 0) == BatteryManager.BATTERY_STATUS_CHARGING;

                // 計算
                averageVoltage = averageVoltage(voltage);
                calcedLevel = calcLevel(averageVoltage);
                int levelForNotify = Math.min(calcedLevel, level);

                // 履歴更新
                if (blh.updateHistory(lastBattUpdate, level)) {

                    // 変化があれば保存
                    blh.save(context);

                    // バッテリードレインを検出したら通知
                    if (blh.getPrevUsageColorId(context) == R.color.fast) {
                        // 未通知なら通知
                        if (drainNotified == false) {
                            doNotify(R.string.notif_battery_drain);
                            drainNotified = true;
                        }
                    }
                }

                // ウィジェット更新
                DefaultWidgetProvider.update(context);

                // 通知
                postNotify(levelForNotify);

                // ログ出力
                if (lastBattUpdate >= lastLog + Const.LOG_MIN_INTERVAL) {
                    StringBuilder sb = new StringBuilder();
                    if (lastLog == 0) {
                        sb.append("calced level average voltage temperature charging\n");
                    }
                    sb.append(calcedLevel);
                    sb.append(" ");
                    sb.append(level);
                    sb.append(" ");
                    sb.append(averageVoltage);
                    sb.append(" ");
                    sb.append(voltage);
                    sb.append(" ");
                    sb.append(temperature);
                    sb.append(" ");
                    sb.append(charging ? "t" : "f");
                    Logger.i(sb.toString());
                    lastLog = lastBattUpdate;
                }

                // 充電状態が変われば警告通知を可能にする
                if (charging_prev != charging) {
                    alertNotified = false;
                    if (charging == false) {
                        drainNotified = false;
                    }
                }

                // トリガー起動
                int warningLowVoltage = Const.getWarningLowVoltage(context);
                int warningHighVoltage = Const.getWarningHighVoltage(context);
                int warningLowLevel = Const.calcLevel(context, warningLowVoltage, 1);
                int warningHighLevel = Const.calcLevel(context, warningHighVoltage, 1);
                if (charging == false) {
                    if (averageVoltage <= warningLowVoltage || levelForNotify <= warningLowLevel) {
                        onLowVoltage();
                    }
                }
                else if (averageVoltage >= warningHighVoltage || levelForNotify >= warningHighLevel) {
                    onHighVoltage();
                }

                // debug
                //doNotify("test");
            }
        }
    };

    public static BackgroundService getInstance() {
        if (instance == null) {
            Logger.e("BackgroundService is null");
        }
        return instance;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        Logger.v("BackgroundService onCreate");
        super.onCreate();

        instance = this;
        notificationBuilder = null;
        averageVoltage = 0;
        voltage = 0;
        calcedLevel = -1;
        level = -1;
        temperature = -100;
        charging = false;
        lastLog = 0;
        usage = 0;
        blh = null;
        alertNotified = false;
        drainNotified = false;

        // registerReceiver
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(broadcastReceiver, filter);

        // 念のためsetClickIntentしておく
        DefaultWidgetProvider.setClickIntent(this);

        // 通知
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notificationBuilder = new NotificationCompat.Builder(getApplicationContext());
        notificationBuilder.setContentIntent(contentIntent);
        notificationBuilder.setSmallIcon(R.drawable.notif_icon_0);
        notificationBuilder.setContentTitle(getString(R.string.app_name));
        notificationBuilder.setContentText(getString(R.string.none));
        notificationBuilder.setWhen(System.currentTimeMillis());
        // フォアグラウンド起動
        startForeground(Const.NOTIF_MAIN_ID, notificationBuilder.build());
    }

    @SuppressLint("InlinedApi")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Logger.v("BackgroundService onStartCommand, intent=" + AndroidUtils.getActionForLog(intent));

        if (blh == null) {
            blh = BattLevelHistory.restore(this);
        }

        if (intent != null) {

            // ウィジェットCLICKのintentだった場合
            if (AndroidUtils.isActionEquals(intent, Const.INTENT_WD_CLICKED)) {

                // クリックアニメーション
                int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                DefaultWidgetProvider.showClickAnimation(this, widgetId);

                // アプリ起動
                Intent actIntent = new Intent(this, MainActivity.class);
                actIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(actIntent);
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onLowMemory() {
        // ログ書き出し
        Logger.startFlushThread(false);

        super.onLowMemory();
    }

    @Override
    public void onDestroy() {
        Logger.v("BackgroundService onDestroy");

        // フォアグラウンドサービス停止
        stopForeground(true);

        unregisterReceiver(broadcastReceiver);
        instance = null;
        notificationBuilder = null;

        // ログ書き出し
        Logger.startFlushThread(true);

        super.onDestroy();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void onLowVoltage() {
        if (alertNotified == false) {
            doNotify(R.string.notif_low_voltage);
            alertNotified = true;
        }
    }

    private void onHighVoltage() {
        if (alertNotified == false) {
            doNotify(R.string.notif_high_voltage);
            alertNotified = true;
        }
    }

    protected int averageVoltage(int currentVoltage) {
        prev_voltages[NUM_PREV_VOLTAGE - 1] = currentVoltage;

        int averageVoltage = 0;
        for (int i = 0; i < NUM_PREV_VOLTAGE; i++) {
            if (prev_voltages[i] == 0) {
                prev_voltages[i] = currentVoltage;
            }
            averageVoltage += prev_voltage_weights[i] * prev_voltages[i] / 100.0;
            if (i >= 1) {
                prev_voltages[i - 1] = prev_voltages[i];
            }
        }

        return averageVoltage;
    }

    protected int calcLevel(int voltage) {
        return Const.calcLevel(this, voltage, 1);
    }

    public void postNotify(int level) {
        //Logger.v("BackgroundService postNotify");

        int notifyImageId = R.drawable.notif_icon_0;
        switch (level) {
            case 1:
                notifyImageId = R.drawable.notif_icon_1;
                break;
            case 2:
                notifyImageId = R.drawable.notif_icon_2;
                break;
            case 3:
                notifyImageId = R.drawable.notif_icon_3;
                break;
            case 4:
                notifyImageId = R.drawable.notif_icon_4;
                break;
            case 5:
                notifyImageId = R.drawable.notif_icon_5;
                break;
            case 6:
                notifyImageId = R.drawable.notif_icon_6;
                break;
            case 7:
                notifyImageId = R.drawable.notif_icon_7;
                break;
            case 8:
                notifyImageId = R.drawable.notif_icon_8;
                break;
            case 9:
                notifyImageId = R.drawable.notif_icon_9;
                break;
            case 10:
                notifyImageId = R.drawable.notif_icon_10;
                break;
            case 11:
                notifyImageId = R.drawable.notif_icon_11;
                break;
            case 12:
                notifyImageId = R.drawable.notif_icon_12;
                break;
            case 13:
                notifyImageId = R.drawable.notif_icon_13;
                break;
            case 14:
                notifyImageId = R.drawable.notif_icon_14;
                break;
            case 15:
                notifyImageId = R.drawable.notif_icon_15;
                break;
            case 16:
                notifyImageId = R.drawable.notif_icon_16;
                break;
            case 17:
                notifyImageId = R.drawable.notif_icon_17;
                break;
            case 18:
                notifyImageId = R.drawable.notif_icon_18;
                break;
            case 19:
                notifyImageId = R.drawable.notif_icon_19;
                break;
            case 20:
                notifyImageId = R.drawable.notif_icon_20;
                break;
            case 21:
                notifyImageId = R.drawable.notif_icon_21;
                break;
            case 22:
                notifyImageId = R.drawable.notif_icon_22;
                break;
            case 23:
                notifyImageId = R.drawable.notif_icon_23;
                break;
            case 24:
                notifyImageId = R.drawable.notif_icon_24;
                break;
            case 25:
                notifyImageId = R.drawable.notif_icon_25;
                break;
            case 26:
                notifyImageId = R.drawable.notif_icon_26;
                break;
            case 27:
                notifyImageId = R.drawable.notif_icon_27;
                break;
            case 28:
                notifyImageId = R.drawable.notif_icon_28;
                break;
            case 29:
                notifyImageId = R.drawable.notif_icon_29;
                break;
            case 30:
                notifyImageId = R.drawable.notif_icon_30;
                break;
            case 31:
                notifyImageId = R.drawable.notif_icon_31;
                break;
            case 32:
                notifyImageId = R.drawable.notif_icon_32;
                break;
            case 33:
                notifyImageId = R.drawable.notif_icon_33;
                break;
            case 34:
                notifyImageId = R.drawable.notif_icon_34;
                break;
            case 35:
                notifyImageId = R.drawable.notif_icon_35;
                break;
            case 36:
                notifyImageId = R.drawable.notif_icon_36;
                break;
            case 37:
                notifyImageId = R.drawable.notif_icon_37;
                break;
            case 38:
                notifyImageId = R.drawable.notif_icon_38;
                break;
            case 39:
                notifyImageId = R.drawable.notif_icon_39;
                break;
            case 40:
                notifyImageId = R.drawable.notif_icon_40;
                break;
            case 41:
                notifyImageId = R.drawable.notif_icon_41;
                break;
            case 42:
                notifyImageId = R.drawable.notif_icon_42;
                break;
            case 43:
                notifyImageId = R.drawable.notif_icon_43;
                break;
            case 44:
                notifyImageId = R.drawable.notif_icon_44;
                break;
            case 45:
                notifyImageId = R.drawable.notif_icon_45;
                break;
            case 46:
                notifyImageId = R.drawable.notif_icon_46;
                break;
            case 47:
                notifyImageId = R.drawable.notif_icon_47;
                break;
            case 48:
                notifyImageId = R.drawable.notif_icon_48;
                break;
            case 49:
                notifyImageId = R.drawable.notif_icon_49;
                break;
            case 50:
                notifyImageId = R.drawable.notif_icon_50;
                break;
            case 51:
                notifyImageId = R.drawable.notif_icon_51;
                break;
            case 52:
                notifyImageId = R.drawable.notif_icon_52;
                break;
            case 53:
                notifyImageId = R.drawable.notif_icon_53;
                break;
            case 54:
                notifyImageId = R.drawable.notif_icon_54;
                break;
            case 55:
                notifyImageId = R.drawable.notif_icon_55;
                break;
            case 56:
                notifyImageId = R.drawable.notif_icon_56;
                break;
            case 57:
                notifyImageId = R.drawable.notif_icon_57;
                break;
            case 58:
                notifyImageId = R.drawable.notif_icon_58;
                break;
            case 59:
                notifyImageId = R.drawable.notif_icon_59;
                break;
            case 60:
                notifyImageId = R.drawable.notif_icon_60;
                break;
            case 61:
                notifyImageId = R.drawable.notif_icon_61;
                break;
            case 62:
                notifyImageId = R.drawable.notif_icon_62;
                break;
            case 63:
                notifyImageId = R.drawable.notif_icon_63;
                break;
            case 64:
                notifyImageId = R.drawable.notif_icon_64;
                break;
            case 65:
                notifyImageId = R.drawable.notif_icon_65;
                break;
            case 66:
                notifyImageId = R.drawable.notif_icon_66;
                break;
            case 67:
                notifyImageId = R.drawable.notif_icon_67;
                break;
            case 68:
                notifyImageId = R.drawable.notif_icon_68;
                break;
            case 69:
                notifyImageId = R.drawable.notif_icon_69;
                break;
            case 70:
                notifyImageId = R.drawable.notif_icon_70;
                break;
            case 71:
                notifyImageId = R.drawable.notif_icon_71;
                break;
            case 72:
                notifyImageId = R.drawable.notif_icon_72;
                break;
            case 73:
                notifyImageId = R.drawable.notif_icon_73;
                break;
            case 74:
                notifyImageId = R.drawable.notif_icon_74;
                break;
            case 75:
                notifyImageId = R.drawable.notif_icon_75;
                break;
            case 76:
                notifyImageId = R.drawable.notif_icon_76;
                break;
            case 77:
                notifyImageId = R.drawable.notif_icon_77;
                break;
            case 78:
                notifyImageId = R.drawable.notif_icon_78;
                break;
            case 79:
                notifyImageId = R.drawable.notif_icon_79;
                break;
            case 80:
                notifyImageId = R.drawable.notif_icon_80;
                break;
            case 81:
                notifyImageId = R.drawable.notif_icon_81;
                break;
            case 82:
                notifyImageId = R.drawable.notif_icon_82;
                break;
            case 83:
                notifyImageId = R.drawable.notif_icon_83;
                break;
            case 84:
                notifyImageId = R.drawable.notif_icon_84;
                break;
            case 85:
                notifyImageId = R.drawable.notif_icon_85;
                break;
            case 86:
                notifyImageId = R.drawable.notif_icon_86;
                break;
            case 87:
                notifyImageId = R.drawable.notif_icon_87;
                break;
            case 88:
                notifyImageId = R.drawable.notif_icon_88;
                break;
            case 89:
                notifyImageId = R.drawable.notif_icon_89;
                break;
            case 90:
                notifyImageId = R.drawable.notif_icon_90;
                break;
            case 91:
                notifyImageId = R.drawable.notif_icon_91;
                break;
            case 92:
                notifyImageId = R.drawable.notif_icon_92;
                break;
            case 93:
                notifyImageId = R.drawable.notif_icon_93;
                break;
            case 94:
                notifyImageId = R.drawable.notif_icon_94;
                break;
            case 95:
                notifyImageId = R.drawable.notif_icon_95;
                break;
            case 96:
                notifyImageId = R.drawable.notif_icon_96;
                break;
            case 97:
                notifyImageId = R.drawable.notif_icon_97;
                break;
            case 98:
                notifyImageId = R.drawable.notif_icon_98;
                break;
            case 99:
                notifyImageId = R.drawable.notif_icon_99;
                break;
            case 100:
                notifyImageId = R.drawable.notif_icon_100;
                break;
        }

        String notifyText = getLevelStr() + ", " + getVoltageStr() + ", " + getTemperatureStr();

        NotificationManager nman = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder.setSmallIcon(notifyImageId).setContentText(notifyText);
        nman.notify(Const.NOTIF_MAIN_ID, notificationBuilder.build());
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void doNotify(int notif_text_id) {

        String notifyText = getString(notif_text_id);

        final NotificationManager nman = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
        builder.setContentIntent(contentIntent);
        builder.setTicker(notifyText);
        builder.setSmallIcon(android.R.drawable.stat_notify_error);
        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentText(notifyText);
        builder.setWhen(System.currentTimeMillis());
        builder.setAutoCancel(true);

        if (notif_text_id == R.string.notif_battery_drain) {
            builder.setVibrate(new long[]{0, 200, 2000, 200, 2000, 200, 2000, 200});
            builder.setLights(R.color.fast, 300, 3000);
        }
        else {
            builder.setVibrate(new long[]{0, 200, 2000, 200});
            builder.setLights(R.color.mid, 300, 3000);
        }

        // Android5.0以上
        if (Build.VERSION.SDK_INT > 20) {
            builder.setCategory(Notification.CATEGORY_SERVICE);
            builder.setPriority(Notification.PRIORITY_HIGH);
        }

        nman.notify(Const.NOTIF_ALERT_ID, builder.build());
    }

    public int getCalcedLevel() {
        return calcedLevel;
    }

    public int getAverageVoltage() {
        return averageVoltage;
    }

    public int getTemperature() {
        return temperature;
    }

    public String getLevelStr() {
        if (getCalcedLevel() >= 0) {
            return getCalcedLevel() + "/" + level;
        }
        else {
            return getString(R.string.none);
        }
    }

    public String getVoltageStrForActivity() {
        if (getAverageVoltage() >= 0) {
            return getAverageVoltage() + getVoltageSuffix() + " (raw=" + voltage + ")";
        }
        else {
            return getString(R.string.none);
        }
    }

    public String getLevelStrForActivity() {
        if (getCalcedLevel() >= 0) {
            return getCalcedLevel() + "% (raw=" + level + ")";
        }
        else {
            return getString(R.string.none);
        }
    }

    public String getVoltageSuffix() {
        // 前回の更新から15分以上経っていれば、語尾を"?"に変更
        String suffix = "mv";
        if (System.currentTimeMillis() - lastBattUpdate > 15 * 60 * 1000) {
            suffix = "mv?";
        }
        return suffix;
    }

    public String getVoltageStr() {
        return getAverageVoltage() + getVoltageSuffix();
    }

    @SuppressLint("DefaultLocale")
    public String getTemperatureStr() {
        if (getTemperature() > -100) {
            return String.format("%.1f℃", ((double) getTemperature()) / 10);
        }
        else {
            return getString(R.string.none);
        }
    }
}
