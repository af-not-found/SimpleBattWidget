package net.afnf.and.simplebattwidget;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public class MyPreferenceActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // summary更新
        updateSummary(R.string.key_max_voltage);
        updateSummary(R.string.key_warning_high_voltage);
        updateSummary(R.string.key_warning_low_voltage);
        updateSummary(R.string.key_min_voltage);

        // リスナー登録
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(lister);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(lister);
    }

    private SharedPreferences.OnSharedPreferenceChangeListener lister = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key != null) {
                // summary更新
                updateSummary(R.string.key_max_voltage);
                updateSummary(R.string.key_warning_high_voltage);
                updateSummary(R.string.key_warning_low_voltage);
                updateSummary(R.string.key_min_voltage);
            }
        }
    };

    private void updateSummary(int key_id) {
        String key = getString(key_id);
        EditTextPreference pref = (EditTextPreference) findPreference(key);
        int value = 0;
        switch (key_id) {
            case R.string.key_max_voltage:
                value = Const.getMaxVoltage(this);
                break;
            case R.string.key_warning_high_voltage:
                value = Const.getWarningHighVoltage(this);
                break;
            case R.string.key_warning_low_voltage:
                value = Const.getWarningLowVoltage(this);
                break;
            case R.string.key_min_voltage:
                value = Const.getMinVoltage(this);
                break;
        }

        pref.setSummary(value + "mv (" + Const.calcLevel(this, value, 1) + "%)");
    }

    /**
     * 入れ子のPreferenceにテーマが設定されないバグを回避 <br>
     * <p/>
     * http://code.google.com/p/android/issues/detail?id=4611
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);

        if (Build.VERSION.SDK_INT <= 10) {
            if (preference != null) {
                if (preference instanceof PreferenceScreen) {
                    if (((PreferenceScreen) preference).getDialog() != null) {
                        Drawable drawable = this.getWindow().getDecorView().getBackground().getConstantState().newDrawable();
                        ((PreferenceScreen) preference).getDialog().getWindow().getDecorView().setBackgroundDrawable(drawable);
                    }
                }
            }
        }
        return false;
    }
}
