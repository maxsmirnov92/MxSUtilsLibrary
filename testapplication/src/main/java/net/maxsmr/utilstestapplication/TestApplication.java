package net.maxsmr.utilstestapplication;

import android.app.Application;

import net.maxsmr.commonutils.android.logging.ConfigureLog4J;

import org.apache.log4j.Level;

import java.io.File;

public class TestApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ConfigureLog4J.initInstance(5, new File(getFilesDir(), "1.log").getAbsolutePath()); // FIXME don't check file if not needed
        ConfigureLog4J.getInstance().configure(Level.ALL, false, 0, 0);
    }
}
