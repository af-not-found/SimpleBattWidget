package net.afnf.and.simplebattwidget;

import android.os.Bundle;
import android.test.InstrumentationTestRunner;

public class MyInstrumentationTestRunner extends InstrumentationTestRunner {

    @Override
    public void onCreate(Bundle arguments) {
        System.setProperty("dexmaker.dexcache", getTargetContext().getCacheDir().toString());
        arguments.putString("package", "net.afnf.and");
        super.onCreate(arguments);
    }
}