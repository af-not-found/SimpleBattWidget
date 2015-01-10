package net.afnf.and.simplebattwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import net.afnf.and.utils.AndroidUtils;

public class DefaultWidgetProvider extends AppWidgetProvider {

    private static final long MIN_UPDATE_INTERVAL = 1000;

    private static long lastUpdated = -1;

    public static void setClickIntent(Context context) {

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), DefaultWidgetProvider.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

        for (int id : appWidgetIds) {
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_default);
            setClickIntent(context, id, rv);
        }
    }

    public static void setClickIntent(Context context, int appWidgetId, RemoteViews rv) {

        Intent intent = new Intent(context, BackgroundService.class);
        intent.setAction(Const.INTENT_WD_CLICKED);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        //intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds); // 未使用

        PendingIntent pendingIntent = PendingIntent.getService(context, appWidgetId, intent, 0); // putExtraする場合、第2引数(requestCode)の指定が必須

        // rv.setOnClickPendingIntent(R.id.widgetImage, pendingIntent); // 不要らしい
        // rv.setOnClickPendingIntent(R.id.widgetText, pendingIntent); // 不要らしい
        rv.setOnClickPendingIntent(R.id.widgetDefaultLayout, pendingIntent);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void showClickAnimation(Context context, int widgetId) {
        ClickAnimateThread thread = new ClickAnimateThread();
        thread.context = context;
        thread.widgetId = widgetId;
        thread.start();
    }

    public static void update(Context context) {
        Intent intent = new Intent(context, DefaultWidgetProvider.class);
        intent.setAction(Const.INTENT_WD_UPDATED);
        context.sendBroadcast(intent);
    }

    public static void changeStyle(Context context) {
        Intent intent = new Intent(context, DefaultWidgetProvider.class);
        intent.setAction(Const.INTENT_WD_STYLE_CHANGED);
        context.sendBroadcast(intent);
    }

    public static boolean hasWidget(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = getWidgetIds(appWidgetManager, context);
        return (appWidgetIds == null || appWidgetIds.length == 0) == false;
    }

    private static int[] getWidgetIds(AppWidgetManager appWidgetManager, Context context) {
        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), DefaultWidgetProvider.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
        return appWidgetIds;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        //Logger.v("Widget onUpdate");
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        // サービスがない場合
        BackgroundService service = BackgroundService.getInstance();
        if (service == null) {
            // サービス起動
            Intent srvIntent = new Intent(context, BackgroundService.class);
            context.startService(srvIntent);
        }

        // サービスがある場合
        else {
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_default);
            rv.setTextViewText(R.id.wdBattPercentage, service.getLevelStr());
            rv.setTextViewText(R.id.wdBattVoltage, service.getVoltageStr());
            rv.setTextViewText(R.id.wdUsage, service.blh.getUsageStr());
            rv.setTextColor(R.id.wdUsage, context.getResources().getColor(service.blh.getPrevUsageColorId(context)));
            // updateAppWidget
            for (int id : appWidgetIds) {
                setClickIntent(context, id, rv);
                appWidgetManager.updateAppWidget(id, rv);
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //Logger.v("Widget onReceive intent=" + Utils.getActionForLog(intent));

        // デフォルトの処理
        super.onReceive(context, intent);

        boolean forceUpdate = false;
        long now = System.currentTimeMillis();
        if (now >= lastUpdated + MIN_UPDATE_INTERVAL) {
            lastUpdated = now;
            forceUpdate = true;
        }

        // 準備
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = getWidgetIds(appWidgetManager, context);

        // ACTION_APPWIDGET_UPDATE(super.onReceiveからonUpdateが呼ばれる)でなく、強制更新の場合
        // またはINTENT_WD_CHANGE_STYLE
        // またはINTENT_WD_UPDATE
        boolean update = (AndroidUtils.isActionEquals(intent, AppWidgetManager.ACTION_APPWIDGET_UPDATE) == false && forceUpdate)
                || AndroidUtils.isActionEquals(intent, Const.INTENT_WD_STYLE_CHANGED)
                || AndroidUtils.isActionEquals(intent, Const.INTENT_WD_UPDATED);

        // onUpdateに委譲
        if (update) {
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }

    static class ClickAnimateThread extends Thread {
        Context context;
        int widgetId;

        @Override
        public void run() {
            try {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_default);
                rv.setInt(R.id.widgetDefaultLayout, "setBackgroundResource", R.drawable.backbround_non_trans);
                appWidgetManager.updateAppWidget(widgetId, rv);
                AndroidUtils.sleep(250);
                rv.setInt(R.id.widgetDefaultLayout, "setBackgroundResource", R.drawable.backbround);

                setClickIntent(context, widgetId, rv);
                appWidgetManager.updateAppWidget(widgetId, rv);
            }
            catch (Throwable e) {
                // do nothing
            }
        }
    }
}
