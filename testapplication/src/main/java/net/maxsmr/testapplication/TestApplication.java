package net.maxsmr.testapplication;

import android.app.Application;

import net.maxsmr.commonutils.android.logging.ConfigureLog4J;

import org.apache.log4j.Level;

import java.io.File;

public class TestApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ConfigureLog4J.getInstance().configure(Level.ALL, false, new File(getFilesDir(), "1.log").getAbsolutePath(), 0, 0);
    }
}
