package net.afnf.and.simplebattwidget;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.afnf.and.utils.Logger;

import proguard.annotation.KeepName;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //Logger.v("MainActivity onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (BuildConfig.DEBUG) {
            findViewById(R.id.buttonFlashLog).setVisibility(View.VISIBLE);
        }

        // サービスがない場合
        BackgroundService service = BackgroundService.getInstance();
        if (service == null) {
            // サービス起動
            Intent srvIntent = new Intent(this, BackgroundService.class);
            startService(srvIntent);
        }
        else {
            TextView battPercentage = (TextView) findViewById(R.id.battPercentage);
            TextView battVoltage = (TextView) findViewById(R.id.battVoltage);
            TextView usage = (TextView) findViewById(R.id.usage);
            TextView temperature = (TextView) findViewById(R.id.temperature);

            battPercentage.setText(service.getLevelStrForActivity());
            battVoltage.setText(service.getVoltageStrForActivity());
            usage.setText(service.blh.getUsageStr());
            usage.setTextColor(getResources().getColor(service.blh.getPrevUsageColorId(this)));
            temperature.setText(service.getTemperatureStr());

            LinearLayout usageLayout = (LinearLayout) findViewById(R.id.usageLayout);
            usageLayout.removeAllViews();
            for (int i = 0; i < 16; i++) {
                TextView textView = new TextView(this);
                textView.setTypeface(Typeface.MONOSPACE);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                textView.setTextColor(getResources().getColor(R.color.orig));

                int time = (i + 1);
                textView.setText((time < 10 ? " " : "") + time + "h : " + service.blh.getUsageStr(i));
                usageLayout.addView(textView);
            }

            // ウィジェット更新
            DefaultWidgetProvider.update(this);
        }
    }

    @Override
    protected void onDestroy() {
        // ウィジェットが無くなればサービス停止
        if (DefaultWidgetProvider.hasWidget(this) == false) {
            Intent intent = new Intent(this, BackgroundService.class);
            stopService(intent);
        }
        super.onDestroy();
    }

    @KeepName
    public void onShowUsage(View view) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        ComponentName compo = new ComponentName("com.android.settings", "com.android.settings.fuelgauge.PowerUsageSummary");
        intent.setComponent(compo);
        startActivity(intent);
    }

    @KeepName
    public void onFlashLog(View view) {
        Logger.startFlushThread(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int i = 1;
        menu.add(Menu.NONE, i++, Menu.NONE, R.string.settings).setIcon(android.R.drawable.ic_menu_preferences)
                .setIntent(new Intent(this, MyPreferenceActivity.class));
        return super.onCreateOptionsMenu(menu);
    }
}
