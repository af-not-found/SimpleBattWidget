package net.afnf.and.simplebattwidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import net.afnf.and.utils.AndroidUtils;
import net.afnf.and.utils.Logger;

public class StaticIntentListener extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        // 端末起動完了
        if (AndroidUtils.isActionEquals(intent, Intent.ACTION_BOOT_COMPLETED)) {
            Logger.v("StaticIntentListener ACTION_BOOT_COMPLETED");

            // サービスがない場合
            BackgroundService service = BackgroundService.getInstance();
            if (service == null) {
                // サービス起動
                Intent srvIntent = new Intent(context, BackgroundService.class);
                context.startService(srvIntent);
            }
        }
    }
}
