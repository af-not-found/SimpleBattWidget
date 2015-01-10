package net.afnf.and.utils;

import android.content.Context;
import android.os.Build;

import java.lang.Thread.UncaughtExceptionHandler;

public class MyUncaughtExceptionHandler implements UncaughtExceptionHandler {

    private static final String LABEL = "##MyUncaughtExceptionHandler";

    public static void init(Context context) {

        // 設定済でなければ
        UncaughtExceptionHandler h = Thread.getDefaultUncaughtExceptionHandler();
        if (h == null || MyStringUtlis.eqauls(LABEL, h.toString()) == false) {

            // UncaughtExceptionHandler設定
            Thread.setDefaultUncaughtExceptionHandler(new MyUncaughtExceptionHandler());

            // 端末・アプリ情報出力
            Logger.i("device:" + Build.DEVICE);
            Logger.i("model:" + Build.MODEL);
            Logger.i("release:" + Build.VERSION.RELEASE);
            Logger.i("sdk:" + Build.VERSION.SDK_INT);
            Logger.i("appver:" + AndroidUtils.getAppVer(context));
            Logger.i("build:" + AndroidUtils.getBuildDate(context));

            // ログ削除
            Logger.startDeleteOldFileThread();
        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        Logger.e("uncaughtException", ex);
        Logger.startFlushThread(true);
    }

    @Override
    public String toString() {
        return LABEL;
    }
}
